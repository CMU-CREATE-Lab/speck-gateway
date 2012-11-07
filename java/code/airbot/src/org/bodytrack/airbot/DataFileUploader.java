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
public final class DataFileUploader
   {
   private static final Logger LOG = Logger.getLogger(DataFileUploader.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   public static final String MAX_NUM_UPLOAD_THREADS_SYSTEM_PROPERTY = "org.bodytrack.airbot.DataFileUploader.max-num-upload-threads";
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

      final String message = "DataFileUploader: using up to [" + MAX_NUM_UPLOAD_THREADS + "] upload thread(s).";
      LOG.info(message);
      CONSOLE_LOG.info(message);
      }

   @NotNull
   private final DataStorageCredentials dataStorageCredentials;

   public interface EventListener
      {
      void handleFileUploadedEvent(@NotNull final File uploadedFile, @Nullable final DataFileUploadResponse uploadResponse);
      }

   private final ExecutorService executor = Executors.newFixedThreadPool(MAX_NUM_UPLOAD_THREADS, new DaemonThreadFactory(this.getClass() + ".executor"));
   private final Set<EventListener> eventListeners = new HashSet<EventListener>();

   /**
    * Constructs a <code>DataFileUploader</code> for the given {@link DataStorageCredentials} and {@link DataStorageCredentials}.
    */
   public DataFileUploader(@NotNull final DataStorageCredentials dataStorageCredentials)
      {
      this.dataStorageCredentials = dataStorageCredentials;

      if (LOG.isInfoEnabled())
         {
         final String msg = "URL for uploading: [" + DataFileUploadHelper.getUploadUrl(dataStorageCredentials) + "]";
         LOG.info("DataFileUploader.DataFileUploader(): " + msg);
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
         LOG.debug("DataFileUploader.submitUploadFileTask(" + fileToUpload + ", " + originalFilename + ")");
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
         final DataFileUploadResponse dataFileUploadResponse = DataFileUploadHelper.upload(dataStorageCredentials,
                                                                                           new FileEntity(fileToUpload, ContentType.APPLICATION_OCTET_STREAM),
                                                                                           fileToUpload.toString());
         // notify listeners
         for (final EventListener listener : eventListeners)
            {
            listener.handleFileUploadedEvent(fileToUpload, dataFileUploadResponse);
            }
         }
      }

   public static void main(final String[] args) throws InterruptedException
      {
      /*
      final String response1 = "{\"result\":\"OK\",\"message\":\"Upload successful!\"}";
      final String response2 = "{\"result\":\"OK\",\"message\":\"Upload successful!\",\"payload\":{\"successful_records\":\"1\",\"failed_records\":\"0\"}}";
      final String response3 = "{\"result\":\"KO\",\"message\":\"Upload failed!\",\"payload\":{\"failure\":\"exception: Failed to parse JSON file\"}}";

      convertToJson(response1);
      convertToJson(response2);
      convertToJson(response3);
      */

      /*
      System.out.println("isValid() = " + isValid(
            new DataStorageCredentials()
            {
            @NotNull
            @Override
            public String getHostName()
               {
               return "localhost";
               }

            @Override
            public int getHostPort()
               {
               return 8080;
               }

            @NotNull
            @Override
            public String getUsername()
               {
               return "test";
               }

            @NotNull
            @Override
            public String getPassword()
               {
               return "testtest";
               }

            @NotNull
            @Override
            public String getDeviceName()
               {
               return "AuthenticationTest";
               }
            }));
      */

      final DataFileUploader uploader = new DataFileUploader(
            new DataStorageCredentials()
            {
            @NotNull
            @Override
            public String getHostName()
               {
               return "localhost";
               }

            @Override
            public int getHostPort()
               {
               return 8080;
               }

            @NotNull
            @Override
            public String getUsername()
               {
               return "test";
               }

            @NotNull
            @Override
            public String getPassword()
               {
               return "testtest";
               }

            @NotNull
            @Override
            public String getDeviceName()
               {
               return "AirBotTest";
               }
            });

      uploader.submitUploadFileTask(new File("/Users/chris/BodyTrack/AirBotData/AirBot3948343337381410000f00/50972a3a.UPLOADING"), "50972a3a.TXT");
      //uploader.submitUploadFileTask(new File("/Users/chris/BodyTrack/AirBotData/AirBot3948343337381410000f00/50972a3f.UPLOADING"), "50972a3f.TXT");

      LOG.debug("DataFileUploader.main(): Busy waiting....");
      final long endTime = System.currentTimeMillis() + 10 * 1000;
      do
         {
         Thread.sleep(500);
         }
      while (System.currentTimeMillis() < endTime);
      }
   }