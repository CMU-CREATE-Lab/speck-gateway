package org.bodytrack.airbot;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataSampleUploader
   {
   private static final Logger LOG = Logger.getLogger(DataSampleUploader.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   public static final String MAX_NUM_UPLOAD_THREADS_SYSTEM_PROPERTY = "org.bodytrack.airbot.DataSampleUploader.max-num-upload-threads";
   private static final int DEFAULT_MAX_NUM_UPLOAD_THREADS = 1;
   private static final int MAX_NUM_UPLOAD_THREADS;

   static
      {
      final String maxNumUploadThreadsStr = System.getProperty(MAX_NUM_UPLOAD_THREADS_SYSTEM_PROPERTY);
      if (maxNumUploadThreadsStr == null)
         {
         MAX_NUM_UPLOAD_THREADS = DEFAULT_MAX_NUM_UPLOAD_THREADS;
         }
      else
         {
         int maxNumUploadThreads;
         try
            {
            maxNumUploadThreads = Integer.parseInt(maxNumUploadThreadsStr);
            }
         catch (NumberFormatException e)
            {
            LOG.error("NumberFormatException while trying to parse [" + maxNumUploadThreadsStr + "] as an int for the max number of upload threads.  Defaulting to " + DEFAULT_MAX_NUM_UPLOAD_THREADS, e);
            maxNumUploadThreads = DEFAULT_MAX_NUM_UPLOAD_THREADS;
            }

         MAX_NUM_UPLOAD_THREADS = Math.max(1, maxNumUploadThreads);
         }

      final String message = "DataSampleUploader: using up to [" + MAX_NUM_UPLOAD_THREADS + "] upload thread(s).";
      LOG.info(message);
      CONSOLE_LOG.info(message);
      }

   @NotNull
   private final RemoteStorageCredentials remoteStorageCredentials;

   public interface EventListener
      {
      void handleFileUploadedEvent(@NotNull final File uploadedFile, @Nullable final DataSampleUploadResponse uploadResponse);
      }

   private final ExecutorService executor = Executors.newFixedThreadPool(MAX_NUM_UPLOAD_THREADS, new DaemonThreadFactory(this.getClass() + ".executor"));
   private final Set<EventListener> eventListeners = new HashSet<EventListener>();

   /**
    * Constructs a <code>DataSampleUploader</code> for the given {@link RemoteStorageCredentials} and {@link RemoteStorageCredentials}.
    */
   public DataSampleUploader(@NotNull final RemoteStorageCredentials remoteStorageCredentials)
      {
      this.remoteStorageCredentials = remoteStorageCredentials;

      if (LOG.isInfoEnabled())
         {
         final String msg = "URL for uploading: [" + DataSampleUploadHelper.getUploadUrl(remoteStorageCredentials) + "]";
         LOG.info("DataSampleUploader.DataSampleUploader(): " + msg);
         CONSOLE_LOG.info(msg);
         }
      }

   public void addEventListener(@Nullable final EventListener listener)
      {
      if (listener != null)
         {
         eventListeners.add(listener);
         }
      }

   public void submitUploadFileTask(@Nullable final File fileToUpload, @Nullable final String originalFilename)
      {
      if (LOG.isDebugEnabled())
         {
         LOG.debug("DataSampleUploader.submitUploadFileTask(" + fileToUpload + ", " + originalFilename + ")");
         }

      if (fileToUpload != null && originalFilename != null)
         {
         executor.execute(new UploadFileTask(fileToUpload));
         }
      }

   private final class UploadFileTask implements Runnable
      {
      private final File fileToUpload;

      private UploadFileTask(@NotNull final File fileToUpload)
         {
         this.fileToUpload = fileToUpload;
         }

      @Override
      public void run()
         {
         final DataSampleUploadResponse dataSampleUploadResponse = DataSampleUploadHelper.upload(remoteStorageCredentials,
                                                                                             new FileEntity(fileToUpload, ContentType.APPLICATION_OCTET_STREAM),
                                                                                             fileToUpload.toString());
         // notify listeners
         for (final EventListener listener : eventListeners)
            {
            listener.handleFileUploadedEvent(fileToUpload, dataSampleUploadResponse);
            }
         }
      }
   }