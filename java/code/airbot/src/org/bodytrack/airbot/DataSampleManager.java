package org.bodytrack.airbot;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataSampleManager implements DataSampleUploader.EventListener
   {
   public interface Statistics
      {
      enum Category
         {
            UPLOADS_REQUESTED,
            UPLOADS_SUCCESSFUL,
            UPLOADS_FAILED,
            DOWNLOADS_REQUESTED,
            DOWNLOADS_SUCCESSFUL,
            DOWNLOADS_FAILED,
            DELETES_REQUESTED,
            DELETES_SUCCESSFUL,
            DELETES_FAILED,
            SAVES_REQUESTED,
            SAVES_SUCCESSFUL,
            SAVES_FAILED
         }

      interface Listener
         {
         void handleValueChange(@NotNull Category category, final int newValue);
         }

      /** Renders the statistics in an ASCII table */
      @Override
      String toString();
      }

   private static final Logger LOG = Logger.getLogger(DataSampleManager.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   @Nullable
   private final DataSampleDownloader dataSampleDownloader;

   @NotNull
   private final DataSampleStore dataSampleStore;

   @Nullable
   private DataSampleUploader dataSampleUploader = null;

   private boolean isRunning = false;
   private boolean hasBeenShutdown = false;

   @NotNull
   private final Lock lock = new ReentrantLock();

   @NotNull
   private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10, new DaemonThreadFactory(this.getClass() + ".executor"));

   @NotNull
   private final StatisticsImpl statistics = new StatisticsImpl();

   @NotNull
   private final Runnable downloadDataSampleRunnable =
         new Runnable()
         {
         @Override
         public void run()
            {
            if (dataSampleDownloader != null)
               {
               CONSOLE_LOG.info("Downloading data sample from device...");

               // try to download a data sample
               final DataSampleDownloader.DownloadResponse downloadResponse = dataSampleDownloader.downloadDataSample();

               final int delayInSecondsUntilNextDataSampleRequest;
               final DataSampleDownloader.DownloadResponse.Status status = downloadResponse.getStatus();
               switch (status)
                  {
                  case OK:
                     statistics.incrementDownloadsRequested();
                     statistics.incrementDownloadsSuccessful();

                     // make sure the data sample is non-null
                     final AirBot.DataSample dataSample = downloadResponse.getDataSample();
                     if (dataSample == null)
                        {
                        LOG.error("DataSampleManager.downloadDataSampleRunnable.run(): Failed to save data sample because it was null.  This shouldn't ever happen!");
                        }
                     else
                        {
                        if (LOG.isInfoEnabled())
                           {
                           CONSOLE_LOG.info("Got data sample " + dataSample.getSampleTime());
                           CONSOLE_LOG.info("Saving data sample " + dataSample.getSampleTime() + "...");
                           }

                        // try to save the data sample
                        statistics.incrementSavesRequested();
                        final boolean wasSaveSuccessful = dataSampleStore.save(dataSample);

                        if (wasSaveSuccessful)
                           {
                           statistics.incrementSavesSuccessful();

                           LOG.debug("DataSampleManager.downloadDataSampleRunnable.run(): Saved data sample [" + dataSample.getSampleTime() + "]");

                           if (LOG.isInfoEnabled())
                              {
                              CONSOLE_LOG.info("Saved data sample " + dataSample.getSampleTime());
                              CONSOLE_LOG.info("Deleting data sample " + dataSample.getSampleTime() + " from device...");
                              }

                           // submit a request to delete the sample from the device
                           statistics.incrementDeletesRequested();
                           final boolean wasDeleteSuccessful = dataSampleDownloader.deleteDataSample(dataSample);

                           if (wasDeleteSuccessful)
                              {
                              statistics.incrementDeletesSuccessful();

                              if (LOG.isInfoEnabled())
                                 {
                                 CONSOLE_LOG.info("Data sample " + dataSample + " was successfully deleted from the device.");
                                 }
                              }
                           else
                              {
                              statistics.incrementDeletesFailed();

                              if (LOG.isInfoEnabled())
                                 {
                                 CONSOLE_LOG.error("Data sample " + dataSample + " could not be deleted from the device.");
                                 }
                              }
                           }
                        else
                           {
                           statistics.incrementSavesFailed();

                           LOG.error("DataSampleManager.downloadDataSampleRunnable.run(): Failed to save data sample [" + dataSample + "]");
                           CONSOLE_LOG.error("Failed to save data sample " + dataSample.getSampleTime());
                           }
                        }
                     delayInSecondsUntilNextDataSampleRequest = 0;

                     break;
                  case NO_DATA_AVAILABLE:

                     delayInSecondsUntilNextDataSampleRequest = 60;

                     if (LOG.isInfoEnabled() || CONSOLE_LOG.isInfoEnabled())
                        {
                        final String message = "No data currently available.  Will try again in " + delayInSecondsUntilNextDataSampleRequest + " seconds.";
                        LOG.info("DataSampleManager.downloadDataSampleRunnable.run(): " + message);
                        CONSOLE_LOG.info(message);

                        final String stats = getStatisticsAsString();
                        LOG.info(stats);
                        CONSOLE_LOG.info(stats);
                        }

                     break;
                  case COMMUNICATION_FAILURE:
                     statistics.incrementDownloadsRequested();
                     statistics.incrementDownloadsFailed();

                     delayInSecondsUntilNextDataSampleRequest = 5;

                     LOG.error("DataSampleManager.downloadDataSampleRunnable.run(): Download failed due to a communication failure.");
                     CONSOLE_LOG.error("Data sample download failed due to a communication error.  Will try again in " + delayInSecondsUntilNextDataSampleRequest + " seconds.");

                     break;
                  default:
                     delayInSecondsUntilNextDataSampleRequest = -1;
                     LOG.error("DataSampleManager.downloadDataSampleRunnable.run(): Unexpected DownloadResponse.Status: " + status);

                     break;
                  }

               if (delayInSecondsUntilNextDataSampleRequest >= 0)
                  {
                  scheduleDownloadDataSample(delayInSecondsUntilNextDataSampleRequest, TimeUnit.SECONDS);
                  }
               }
            }
         };

   public DataSampleManager(@NotNull final AirBotConfig airBotConfig,
                            @Nullable final DataSampleDownloader dataSampleDownloader) throws InitializationException
      {
      this.dataSampleDownloader = dataSampleDownloader;
      this.dataSampleStore = new MultiHeadDataSampleStore(airBotConfig);

      // register self as a listener to the uploader so we can get notified when uploads are complete
      if (dataSampleUploader != null)
         {
         dataSampleUploader.addEventListener(this);
         }
      }

   /**
    * Sets the uploader.  Can only be called once--subsequent calls are ignored. Returns <code>true</code> if the
    * uploader was set successfully, <code>false</code> otherwise.
    */
   public boolean setDataSampleUploader(@Nullable final DataSampleUploader dataSampleUploader)
      {
      if (isDataSampleUploaderDefined())
         {
         final String msg = "The DataSampleUploader can only be set once.";
         LOG.info("DataSampleManager.setDataSampleUploader(): " + msg);
         CONSOLE_LOG.info(msg);
         }
      else
         {
         if (dataSampleUploader != null)
            {
            this.dataSampleUploader = dataSampleUploader;

            // register self as a listener to the uploader so we can get notified when uploads are complete
            this.dataSampleUploader.addEventListener(this);

            // make sure any existing downloaded files are scheduled for upload, but only if we're already running.  If
            // we're not, then don't worry about it since startup() will handle this itself.
            if (isRunning)
               {
               uploadExistingDownloadedSamples();
               }

            return true;
            }
         }
      return false;
      }

   public boolean isDataSampleUploaderDefined()
      {
      return dataSampleUploader != null;
      }

   public void startup()
      {
      lock.lock();  // block until condition holds
      try
         {
         if (!isRunning && !hasBeenShutdown)
            {
            isRunning = true;

            // Clean up samples in the data store, in case the program was terminated while an upload was in progress
            dataSampleStore.resetStateOfUploadingSamples();

            // make sure any existing downloaded samples are scheduled for upload
            uploadExistingDownloadedSamples();

            // schedule the command to get available data samples, which will reschedule itself upon completion
            scheduleDownloadDataSample(0, TimeUnit.SECONDS);
            }
         else
            {
            LOG.debug("DataSampleManager.startup(): Cannot startup since it's already running or has been shutdown.");
            }
         }
      finally
         {
         lock.unlock();
         }
      }

   public void addStatisticsListener(@Nullable final Statistics.Listener listener)
      {
      if (listener != null)
         {
         statistics.addListener(listener);
         }
      }

   private void uploadExistingDownloadedSamples()
      {
      LOG.debug("DataSampleManager.uploadExistingDownloadedSamples()");
      // TODO
      /*
      // If the uploader is defined, then run through all existing downloaded files and kick off an upload job for
      // each one.  To do so, start by getting the list of all downloaded files
      if (isDataSampleUploaderDefined())
         {
         final File[] filesReadyForUpload = dataFileDirectory.listFiles(new DataFileStatusFilenameFilter(DataFileStatus.DOWNLOADED));

         if (filesReadyForUpload != null && filesReadyForUpload.length > 0)
            {
            final String msg = "Found " + filesReadyForUpload.length + " local file(s) to upload.";
            LOG.info("DataSampleManager.setDataFileUploader(): " + msg);
            CONSOLE_LOG.info(msg);
            for (final File file : filesReadyForUpload)
               {
               submitUploadFileTask(file);
               }
            }
         else
            {
            final String msg = "No local file(s) found which need to be uploaded.";
            LOG.info("DataSampleManager.setDataFileUploader(): " + msg);
            CONSOLE_LOG.info(msg);
            }
         }
      */
      }

   private void scheduleDownloadDataSample(final int delay, final TimeUnit timeUnit)
      {
      if (dataSampleDownloader != null)
         {
         executor.schedule(downloadDataSampleRunnable, delay, timeUnit);
         }
      }

   /**
    * Shuts down the <code>DataSampleManager</code>.  Once it is shut down, it cannot be started up again.
    *
    * @see #startup()
    */
   public void shutdown()
      {
      LOG.debug("DataSampleManager.shutdown()");

      lock.lock();  // block until condition holds
      try
         {
         if (isRunning)
            {
            isRunning = false;
            hasBeenShutdown = true;

            // shut down the executor
            try
               {
               LOG.debug("DataSampleManager.shutdown(): Shutting down the executor");
               final List<Runnable> unexecutedTasks = executor.shutdownNow();
               LOG.debug("DataSampleManager.shutdown(): Unexecuted tasks: " + (unexecutedTasks == null ? 0 : unexecutedTasks.size()));
               LOG.debug("DataSampleManager.shutdown(): Waiting up to 30 seconds for the executor to shutdown...");
               final boolean terminatedNormally = executor.awaitTermination(30, TimeUnit.SECONDS);
               if (LOG.isDebugEnabled())
                  {
                  LOG.debug("DataSampleManager.shutdown(): Executor successfully shutdown (timed out = " + !terminatedNormally + ")");
                  }
               }
            catch (Exception e)
               {
               LOG.error("DataSampleManager.shutdown(): Exception while trying to shut down the executor", e);
               }

            // shut down the data store
            dataSampleStore.shutdown();
            }
         }
      finally
         {
         lock.unlock();
         }
      }

   private void submitUploadFileTask(@NotNull final AirBot.DataSample dataSample)
      {
      if (dataSampleUploader != null)
         {
         // TODO: do we need this?  Or should the uploader just have a thread that checks periodically for data to upload?
         /*
         final File fileToUpload = changeFileExtension(file, DataFileStatus.DOWNLOADED.getFilenameExtension(), DataFileStatus.UPLOADING.getFilenameExtension());
         if (fileToUpload != null)
            {
            LOG.debug("DataSampleManager.submitUploadFileTask(): Submitting file [" + fileToUpload.getName() + "] for uploading...");
            dataSampleUploader.submitUploadFileTask(fileToUpload, file.getName());

            // update statistics
            statistics.incrementUploadsRequested();
            }
         else
            {
            LOG.error("DataSampleManager.submitUploadFileTask(): Failed to rename file [" + file.getName() + "] in preparation for uploading it.  Skipping.");
            }
         */
         }
      }

   @Override
   public void handleFileUploadedEvent(@NotNull final File uploadedFile, @Nullable final DataSampleUploadResponse uploadResponse)
      {
      // TODO
      /*
      LOG.debug("DataSampleManager.handleFileUploadedEvent(" + uploadedFile + ", " + uploadResponse + ")");

      if (DataFileStatus.UPLOADING.hasStatus(uploadedFile))
         {
         if (LOG.isDebugEnabled())
            {
            if (LOG.isDebugEnabled())
               {
               LOG.debug("DataSampleManager.handleFileUploadedEvent(): file [" + uploadedFile + "], response = [" + uploadResponse + "]");
               }
            }

         if (uploadResponse == null)
            {
            // update statistics
            statistics.incrementUploadsFailed();

            // If the response was null, then a problem occurred during upload, so just rename the file and return it
            // back to the pool of uploadable files.  Also submit a new upload job for it.

            LOG.info("DataSampleManager.handleFileUploadedEvent(): Upload failure for file [" + uploadedFile.getName() + "].  Renaming it back to the default and will try again later.");

            lock.lock();  // block until condition holds
            try
               {
               // change the extension back to the default
               final File defaultFilename = changeFileExtension(uploadedFile, DataFileStatus.UPLOADING.getFilenameExtension(), DataFileStatus.DOWNLOADED.getFilenameExtension());
               if (defaultFilename == null)
                  {
                  LOG.error("DataSampleManager.handleFileUploadedEvent(): Failed to rename file [" + uploadedFile + "] back to the default name.  Aborting.");
                  CONSOLE_LOG.error("Failed to upload data file because the defaultFilename is null.");
                  }
               else
                  {
                  if (LOG.isDebugEnabled())
                     {
                     LOG.debug("DataSampleManager.handleFileUploadedEvent(): Renamed file [" + uploadedFile + "] to [" + defaultFilename + "].  Will retry upload in 1 minute.");
                     }
                  CONSOLE_LOG.error("Failed to upload data file " + defaultFilename.getName() + ".  Will retry upload in 1 minute.");

                  // schedule the upload again
                  executor.schedule(
                        new Runnable()
                        {
                        @Override
                        public void run()
                           {
                           submitUploadFileTask(defaultFilename);
                           }
                        },
                        1,
                        TimeUnit.MINUTES);
                  }
               }
            finally
               {
               lock.unlock();
               }
            }
         else
            {
            if (uploadResponse.wasSuccessful())
               {
               // update statistics
               statistics.incrementUploadsSuccessful();

               // no failures!  rename the file to signify that the upload was successful...
               lock.lock();  // block until condition holds
               try
                  {
                  // change the extension to the one used for uploaded files
                  final File newFile = changeFileExtension(uploadedFile, DataFileStatus.UPLOADING.getFilenameExtension(), DataFileStatus.UPLOADED.getFilenameExtension());
                  if (newFile == null)
                     {
                     LOG.error("DataSampleManager.handleFileUploadedEvent(): Failed to rename successfully uploaded file [" + uploadedFile.getName() + "]");
                     CONSOLE_LOG.error("File " + uploadedFile.getName() + " uploaded successfully, but could not rename to have the '" + DataFileStatus.UPLOADED + "' file extension.");
                     }
                  else
                     {
                     if (LOG.isDebugEnabled())
                        {
                        LOG.debug("DataSampleManager.handleFileUploadedEvent(): Renamed file [" + uploadedFile + "] to [" + newFile + "]");
                        }
                     if (CONSOLE_LOG.isInfoEnabled())
                        {
                        CONSOLE_LOG.info("File " + newFile.getName() + " uploaded successfully.");
                        }
                     }
                  }
               finally
                  {
                  lock.unlock();
                  }

               // Don't worry about telling the downloader that the file can be deleted here--that'll be handled elsewhere
               }
            else
               {
               // update statistics
               statistics.incrementUploadsFailed();

               final String failureMessage = uploadResponse.getMessage();
               final DataSampleUploadResponse.Payload payload = uploadResponse.getPayload();
               final String payloadFailureMessage = (payload == null) ? null : payload.getFailureMessage();
               final Integer numFailures = (payload == null) ? null : payload.getNumFailedRecords();

               // we had a failure, so just rename the local file to mark it as having corrupt data
               if (LOG.isDebugEnabled())
                  {
                  LOG.debug("DataSampleManager.handleFileUploadedEvent(): num failed records is [" + numFailures + "] and failureMessage(s) [" + failureMessage + "|" + payloadFailureMessage + "], so mark the file as having corrupt data");
                  }
               lock.lock();  // block until condition holds
               try
                  {
                  final File corruptFile = changeFileExtension(uploadedFile, DataFileStatus.UPLOADING.getFilenameExtension(), DataFileStatus.CORRUPT_DATA.getFilenameExtension());
                  if (corruptFile == null)
                     {
                     LOG.error("DataSampleManager.handleFileUploadedEvent(): failed to mark file [" + uploadedFile + "] as having corrupt data!  No further action will be taken on this file.");
                     CONSOLE_LOG.error("File " + uploadedFile.getName() + " failed to upload.  Failed records = " + numFailures + " and failureMessage(s) [" + failureMessage + "|" + payloadFailureMessage + "].  Also failed to rename the file to have the '" + DataFileStatus.CORRUPT_DATA + "' extension");
                     }
                  else
                     {
                     LOG.info("DataSampleManager.handleFileUploadedEvent(): renamed file [" + uploadedFile + "] to [" + corruptFile + "]  to mark it as having corrupt data");
                     CONSOLE_LOG.error("File " + corruptFile.getName() + " failed to upload.  Failed records = " + numFailures + " and failureMessage(s) [" + failureMessage + "|" + payloadFailureMessage + "].");
                     }
                  }
               finally
                  {
                  lock.unlock();
                  }
               }
            }
         }
      */
      }

   public String getStatisticsAsString()
      {
      lock.lock();  // block until condition holds
      try
         {
         return statistics.toString();
         }
      finally
         {
         lock.unlock();
         }
      }

   private static final class StatisticsImpl implements Statistics
      {
      private final Map<Category, AtomicInteger> statisticsMap;
      private final Set<Listener> listeners = new HashSet<Listener>();

      private StatisticsImpl()
         {
         statisticsMap = new HashMap<Category, AtomicInteger>(Category.values().length);
         for (final Category category : Category.values())
            {
            statisticsMap.put(category, new AtomicInteger(0));
            }
         }

      public void addListener(@Nullable final Listener listener)
         {
         if (listener != null)
            {
            listeners.add(listener);
            }
         }

      private int incrementUploadsRequested()
         {
         return incrementValueAndPublishToListeners(Category.UPLOADS_REQUESTED);
         }

      private int incrementUploadsSuccessful()
         {
         return incrementValueAndPublishToListeners(Category.UPLOADS_SUCCESSFUL);
         }

      private int incrementUploadsFailed()
         {
         return incrementValueAndPublishToListeners(Category.UPLOADS_FAILED);
         }

      private int incrementDownloadsRequested()
         {
         return incrementValueAndPublishToListeners(Category.DOWNLOADS_REQUESTED);
         }

      private int incrementDownloadsSuccessful()
         {
         return incrementValueAndPublishToListeners(Category.DOWNLOADS_SUCCESSFUL);
         }

      private int incrementDownloadsFailed()
         {
         return incrementValueAndPublishToListeners(Category.DOWNLOADS_FAILED);
         }

      private int incrementDeletesRequested()
         {
         return incrementValueAndPublishToListeners(Category.DELETES_REQUESTED);
         }

      private int incrementDeletesSuccessful()
         {
         return incrementValueAndPublishToListeners(Category.DELETES_SUCCESSFUL);
         }

      private int incrementDeletesFailed()
         {
         return incrementValueAndPublishToListeners(Category.DELETES_FAILED);
         }

      private int incrementSavesRequested()
         {
         return incrementValueAndPublishToListeners(Category.SAVES_REQUESTED);
         }

      private int incrementSavesSuccessful()
         {
         return incrementValueAndPublishToListeners(Category.SAVES_SUCCESSFUL);
         }

      private int incrementSavesFailed()
         {
         return incrementValueAndPublishToListeners(Category.SAVES_FAILED);
         }

      private int incrementValueAndPublishToListeners(final Category category)
         {
         final int newValue = statisticsMap.get(category).incrementAndGet();
         for (final Listener listener : listeners)
            {
            listener.handleValueChange(category, newValue);
            }
         return newValue;
         }

      @Override
      public String toString()
         {
         final StringWriter stringWriter = new StringWriter();
         final PrintWriter printWriter = new PrintWriter(stringWriter);

         printWriter.printf("\n");
         printWriter.printf(" _________________________________________________________ \n");
         printWriter.printf("|                                                         |\n");
         printWriter.printf("|                         Requested   Successful   Failed |\n");
         printWriter.printf("|                         ---------   ----------   ------ |\n");
         printWriter.printf("| Downloads from Device      %6d       %6d   %6d |\n", statisticsMap.get(Category.DOWNLOADS_REQUESTED).get(), statisticsMap.get(Category.DOWNLOADS_SUCCESSFUL).get(), statisticsMap.get(Category.DOWNLOADS_FAILED).get());
         printWriter.printf("| Deletes from Device        %6d       %6d   %6d |\n", statisticsMap.get(Category.DELETES_REQUESTED).get(), statisticsMap.get(Category.DELETES_SUCCESSFUL).get(), statisticsMap.get(Category.DELETES_FAILED).get());
         printWriter.printf("| Saves to Computer          %6d       %6d   %6d |\n", statisticsMap.get(Category.SAVES_REQUESTED).get(), statisticsMap.get(Category.SAVES_SUCCESSFUL).get(), statisticsMap.get(Category.SAVES_FAILED).get());
         printWriter.printf("| Uploads to Server          %6d       %6d   %6d |\n", statisticsMap.get(Category.UPLOADS_REQUESTED).get(), statisticsMap.get(Category.UPLOADS_SUCCESSFUL).get(), statisticsMap.get(Category.UPLOADS_FAILED).get());
         printWriter.printf("|_________________________________________________________|\n");

         return stringWriter.toString();
         }
      }
   }
