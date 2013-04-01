package org.bodytrack.airbot;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The <code>DataSampleDownloader</code> manages downloading of {@link Speck.DataSample data samples} from a Speck.
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataSampleDownloader
   {
   private static final Logger LOG = Logger.getLogger(DataSampleDownloader.class);

   public static final class DownloadResponse
      {
      public static enum Status
         {
            OK("OK"),
            NO_DATA_AVAILABLE("No Data Available"),
            COMMUNICATION_FAILURE("Communication Failure");

         @NotNull
         private final String name;

         private Status(@NotNull final String name)
            {
            this.name = name;
            }

         @SuppressWarnings("UnusedDeclaration")
         @NotNull
         private String getName()
            {
            return name;
            }

         @Override
         public String toString()
            {
            return this.getClass().getSimpleName() + "." + this.name();
            }
         }

      @NotNull
      private final Status status;

      @Nullable
      private final Speck.DataSample sample;

      public DownloadResponse(@NotNull final Status status, @Nullable final Speck.DataSample sample)
         {
         this.status = status;
         this.sample = sample;
         }

      @NotNull
      public Status getStatus()
         {
         return status;
         }

      @Nullable
      public Speck.DataSample getDataSample()
         {
         return sample;
         }
      }

   private final Speck device;

   public DataSampleDownloader(@NotNull final Speck device)
      {
      this.device = device;
      }

   @NotNull
   public DownloadResponse downloadDataSample()
      {
      if (LOG.isDebugEnabled())
         {
         LOG.debug("DataSampleDownloader.downloadDataSample()");
         }

      DownloadResponse.Status failureCause = DownloadResponse.Status.OK;
      Speck.DataSample dataSample = null;
      try
         {
         dataSample = device.getSample();

         if (dataSample == null)
            {
            // there's no data available
            LOG.debug("DataSampleDownloader.downloadDataSample(): No data available.");
            failureCause = DownloadResponse.Status.NO_DATA_AVAILABLE;
            }
         }
      catch (CommunicationException e)
         {
         // the command failed due to a communication error
         LOG.error("DataSampleDownloader.submitDownloadDataSampleTask.run(): Data sample download failed.", e);
         failureCause = DownloadResponse.Status.COMMUNICATION_FAILURE;
         }

      return new DownloadResponse(failureCause, dataSample);
      }

   public boolean deleteDataSample(@Nullable final Speck.DataSample dataSample)
      {
      boolean wasDeleteSuccessful = false;

      if (dataSample != null)
         {
         if (LOG.isDebugEnabled())
            {
            LOG.debug("DataSampleDownloader.deleteDataSample(" + dataSample + ")");
            }

         try
            {
            wasDeleteSuccessful = device.deleteSample(dataSample);
            }
         catch (CommunicationException e)
            {
            LOG.error("DataSampleDownloader.deleteDataSample(): CommunicationException while trying to delete sample " + dataSample.getSampleTime() + " from device.", e);
            wasDeleteSuccessful = false;
            }

         if (wasDeleteSuccessful)
            {
            if (LOG.isDebugEnabled())
               {
               LOG.debug("DataSampleDownloader.deleteDataSample(): sample [" + dataSample.getSampleTime() + "] successfully deleted from device.");
               }
            }
         else
            {
            LOG.error("DataSampleDownloader.deleteDataSample(): failed to delete sample [" + dataSample.getSampleTime() + "] from device.");
            }
         }

      return wasDeleteSuccessful;
      }
   }
