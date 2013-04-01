package org.bodytrack.airbot;

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
            FILE_UPLOADS_REQUESTED,
            FILE_UPLOADS_SUCCESSFUL,
            FILE_UPLOADS_FAILED,
            SAMPLE_UPLOADS_REQUESTED,
            SAMPLE_UPLOADS_SUCCESSFUL,
            SAMPLE_UPLOADS_FAILED,
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
                     final Speck.DataSample dataSample = downloadResponse.getDataSample();
                     if (dataSample == null)
                        {
                        LOG.error("DataSampleManager.downloadDataSampleRunnable.run(): Failed to save data sample because it was null.  This shouldn't ever happen!");
                        }
                     else
                        {
                        if (CONSOLE_LOG.isInfoEnabled())
                           {
                           CONSOLE_LOG.info("Got data sample " + dataSample.getSampleTime());
                           CONSOLE_LOG.info("Saving data sample " + dataSample.getSampleTime() + "...");
                           }

                        // try to save the data sample
                        statistics.incrementSavesRequested();
                        final DataSampleStore.SaveResult saveResult = dataSampleStore.save(dataSample);

                        if (saveResult.wasSuccessful())
                           {
                           statistics.incrementSavesSuccessful();

                           LOG.debug("DataSampleManager.downloadDataSampleRunnable.run(): Saved data sample [" + dataSample.getSampleTime() + "]");

                           if (CONSOLE_LOG.isInfoEnabled())
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

                              if (CONSOLE_LOG.isInfoEnabled())
                                 {
                                 CONSOLE_LOG.info("Data sample " + dataSample.getSampleTime() + " was successfully deleted from the device.");
                                 }
                              }
                           else
                              {
                              statistics.incrementDeletesFailed();

                              if (CONSOLE_LOG.isInfoEnabled())
                                 {
                                 CONSOLE_LOG.error("Data sample " + dataSample.getSampleTime() + " could not be deleted from the device.");
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

                     delayInSecondsUntilNextDataSampleRequest = 30;

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
                  scheduleDataSampleDownload(delayInSecondsUntilNextDataSampleRequest, TimeUnit.SECONDS);
                  }
               }
            }
         };

   @NotNull
   private Runnable uploadDataSampleRunnable =
         new Runnable()
         {
         @Override
         public void run()
            {
            if (dataSampleUploader != null)
               {
               CONSOLE_LOG.info("Uploading data samples...");

               final DataSampleSet dataSampleSet = dataSampleStore.getDataSamplesToUpload(DataSampleSet.DEFAULT_SIZE);

               if (dataSampleSet.isEmpty())
                  {
                  if (LOG.isInfoEnabled() || CONSOLE_LOG.isInfoEnabled())
                     {
                     final String msg = "No samples found which need to be uploaded.  Will retry in 15 seconds.";
                     LOG.info("DataSampleManager.uploadDataSampleRunnable(): " + msg);
                     CONSOLE_LOG.info(msg);
                     }

                  scheduleDataSampleUpload(15, TimeUnit.SECONDS);
                  }
               else
                  {
                  if (LOG.isInfoEnabled() || CONSOLE_LOG.isInfoEnabled())
                     {
                     final String msg = "Found " + dataSampleSet.size() + " samples to upload.";
                     LOG.info("DataSampleManager.uploadDataSampleRunnable(): " + msg);
                     CONSOLE_LOG.info(msg);
                     }

                  dataSampleUploader.submitUploadDataSampleSetTask(dataSampleSet);

                  // update statistics
                  statistics.incrementFileUploadsRequested();
                  statistics.incrementSampleUploadsRequested(dataSampleSet.size());
                  }
               }
            }
         };

   public DataSampleManager(@NotNull final SpeckConfig speckConfig,
                            @Nullable final DataSampleDownloader dataSampleDownloader) throws InitializationException
      {
      this.dataSampleDownloader = dataSampleDownloader;
      this.dataSampleStore = new MultiDestinationDataSampleStore(speckConfig);
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
         LOG.warn("DataSampleManager.setDataSampleUploader(): " + msg);
         CONSOLE_LOG.warn(msg);
         }
      else
         {
         if (dataSampleUploader != null)
            {
            this.dataSampleUploader = dataSampleUploader;

            // register self as a listener to the uploader so we can get notified when uploads are complete
            this.dataSampleUploader.addEventListener(this);

            // make sure any existing downloaded samples are scheduled for upload, but only if we're already running. If
            // we're not, then don't worry about it since startup() will handle this itself.
            if (isRunning)
               {
               scheduleDataSampleUpload(0, TimeUnit.SECONDS);
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

            // schedule the command to upload downloaded data samples, which will reschedule itself upon completion
            scheduleDataSampleUpload(0, TimeUnit.SECONDS);

            // schedule the command to get available data samples, which will reschedule itself upon completion
            scheduleDataSampleDownload(0, TimeUnit.SECONDS);
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

   private void scheduleDataSampleDownload(final int delay, final TimeUnit timeUnit)
      {
      if (dataSampleDownloader != null)
         {
         executor.schedule(downloadDataSampleRunnable, delay, timeUnit);
         }
      }

   private void scheduleDataSampleUpload(final int delay, final TimeUnit timeUnit)
      {
      if (isDataSampleUploaderDefined())
         {
         executor.schedule(uploadDataSampleRunnable, delay, timeUnit);
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

   @Override
   public void handleDataSamplesUploadedEvent(@NotNull final DataSampleSet dataSampleSet, @Nullable final DataSampleSetUploadResponse uploadResponse)
      {
      LOG.debug("DataSampleManager.handleDataSamplesUploadedEvent(" + dataSampleSet + ", " + uploadResponse + ")");
      if (!dataSampleSet.isEmpty())
         {
         if (uploadResponse == null)
            {
            // update statistics
            statistics.incrementFileUploadsFailed();
            statistics.incrementSampleUploadsFailed(dataSampleSet.size());

            // If the response was null, then a problem occurred during upload, so just submit a new upload job for it.
            if (LOG.isInfoEnabled() || CONSOLE_LOG.isInfoEnabled())
               {
               final String msg = "Data sample upload failure detected, will retry in 1 minute.";
               LOG.info("DataSampleManager.handleDataSamplesUploadedEvent(): " + msg);
               CONSOLE_LOG.info(msg);
               }
            executor.schedule(
                  new Runnable()
                  {
                  @Override
                  public void run()
                     {
                     if (dataSampleUploader != null)
                        {
                        dataSampleUploader.submitUploadDataSampleSetTask(dataSampleSet);

                        // update statistics
                        statistics.incrementFileUploadsRequested();
                        statistics.incrementSampleUploadsRequested(dataSampleSet.size());
                        }
                     }
                  },
                  1,
                  TimeUnit.MINUTES);
            }
         else
            {
            if (uploadResponse.wasSuccessful())
               {
               // update statistics
               statistics.incrementFileUploadsSuccessful();
               statistics.incrementSampleUploadsSuccessful(dataSampleSet.size());

               // No failures!  Tell the data store to mark the samples as uploaded
               dataSampleStore.markDataSamplesAsUploaded(dataSampleSet);
               }
            else
               {
               // update statistics
               statistics.incrementFileUploadsFailed();
               statistics.incrementSampleUploadsFailed(dataSampleSet.size());

               final String failureMessage = uploadResponse.getMessage();
               final DataSampleSetUploadResponse.Payload payload = uploadResponse.getPayload();
               final String payloadFailureMessage = (payload == null) ? null : payload.getFailureMessage();
               final Integer numFailures = (payload == null) ? null : payload.getNumFailedRecords();

               // we had a failure, so just mark the samples as failed
               dataSampleStore.markDataSamplesAsFailed(dataSampleSet);
               LOG.error("DataSampleManager.handleDataSamplesUploadedEvent(): Upload failure: num failed samples is [" + numFailures + "] and failureMessage(s) [" + failureMessage + "|" + payloadFailureMessage + "]");
               CONSOLE_LOG.error("Upload failure: Failed records = " + numFailures + " and failureMessage(s) [" + failureMessage + "|" + payloadFailureMessage + "].  Samples have been flagged as failed.");
               }

            // schedule another upload (wait 15 seconds if the last set had fewer than the default size, otherwise try again right away)
            scheduleDataSampleUpload(dataSampleSet.size() < DataSampleSet.DEFAULT_SIZE ? 15 : 0, TimeUnit.SECONDS);
            }
         }
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

      private int incrementFileUploadsRequested()
         {
         return incrementValueAndPublishToListeners(Category.FILE_UPLOADS_REQUESTED);
         }

      private int incrementFileUploadsSuccessful()
         {
         return incrementValueAndPublishToListeners(Category.FILE_UPLOADS_SUCCESSFUL);
         }

      private int incrementFileUploadsFailed()
         {
         return incrementValueAndPublishToListeners(Category.FILE_UPLOADS_FAILED);
         }

      private int incrementSampleUploadsRequested(final int count)
         {
         return incrementValueAndPublishToListeners(Category.SAMPLE_UPLOADS_REQUESTED, count);
         }

      private int incrementSampleUploadsSuccessful(final int count)
         {
         return incrementValueAndPublishToListeners(Category.SAMPLE_UPLOADS_SUCCESSFUL, count);
         }

      private int incrementSampleUploadsFailed(final int count)
         {
         return incrementValueAndPublishToListeners(Category.SAMPLE_UPLOADS_FAILED, count);
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
         return incrementValueAndPublishToListeners(category, 1);
         }

      private int incrementValueAndPublishToListeners(final Category category, final int delta)
         {
         final int newValue = statisticsMap.get(category).addAndGet(delta);
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
         printWriter.printf(" __________________________________________________________________ \n");
         printWriter.printf("|                                                                  |\n");
         printWriter.printf("|                                  Requested   Successful   Failed |\n");
         printWriter.printf("|                                  ---------   ----------   ------ |\n");
         printWriter.printf("| Samples Downloaded from Device      %6d       %6d   %6d |\n", statisticsMap.get(Category.DOWNLOADS_REQUESTED).get(), statisticsMap.get(Category.DOWNLOADS_SUCCESSFUL).get(), statisticsMap.get(Category.DOWNLOADS_FAILED).get());
         printWriter.printf("| Samples Saved to Computer           %6d       %6d   %6d |\n", statisticsMap.get(Category.SAVES_REQUESTED).get(), statisticsMap.get(Category.SAVES_SUCCESSFUL).get(), statisticsMap.get(Category.SAVES_FAILED).get());
         printWriter.printf("| Samples Deleted from Device         %6d       %6d   %6d |\n", statisticsMap.get(Category.DELETES_REQUESTED).get(), statisticsMap.get(Category.DELETES_SUCCESSFUL).get(), statisticsMap.get(Category.DELETES_FAILED).get());
         printWriter.printf("| Samples Uploaded to Server          %6d       %6d   %6d |\n", statisticsMap.get(Category.SAMPLE_UPLOADS_REQUESTED).get(), statisticsMap.get(Category.SAMPLE_UPLOADS_SUCCESSFUL).get(), statisticsMap.get(Category.SAMPLE_UPLOADS_FAILED).get());
         printWriter.printf("| Files Uploaded to Server            %6d       %6d   %6d |\n", statisticsMap.get(Category.FILE_UPLOADS_REQUESTED).get(), statisticsMap.get(Category.FILE_UPLOADS_SUCCESSFUL).get(), statisticsMap.get(Category.FILE_UPLOADS_FAILED).get());
         printWriter.printf("|__________________________________________________________________|\n");

         return stringWriter.toString();
         }
      }
   }
