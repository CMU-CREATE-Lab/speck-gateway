package org.bodytrack.airbot;

import edu.cmu.ri.createlab.device.CreateLabDeviceProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface AirBot extends CreateLabDeviceProxy
   {
   interface DataSample extends Comparable<DataSample>
      {
      /** Returns the sample's database ID, if known. */
      @Nullable
      Integer getDatabaseId();

      /** The time the sample was taken, in UTC seconds since the epoch. */
      int getSampleTime();

      /** The time the sample was downloaded from the device, in UTC milliseconds since the epoch. */
      long getDownloadTime();

      /** The particle count in particles/Liter. */
      int getParticleCount();

      /** The temperature in degrees F. */
      double getTemperatureInDegreesF();

      /** The temperature in 1/10 degrees F. */
      int getTemperatureInTenthsOfADegreeF();

      /** The humidity in percent relative humidity. */
      int getHumidity();

      /**
       * Returns <code>true</code> if this sample is all zeros, which signifies that there's currently no data
       * available; returns <code>false</code> otherwise.
       */
      boolean isEmpty();

      /**
       * Returns this data sample as {@link String} containing values separated by commas.  Values are returned in this
       * order: sample time, particle count, temperature, humidity, download time
       */
      @NotNull
      String toCsv();

      /**
       * Returns this data sample as a JSON array {@link String}. The array does not include the download time.  Values
       * are returned in this order: sample time, particle count, temperature, humidity
       * */
      @NotNull
      String toJsonArray();
      }

   /**
    * Returns a {@link DataSample}, perhaps from some time in the past, or returns <code>null</code> if no data is
    * available. Throws a {@link CommunicationException} if a sample could not be read.
    *
    * @throws CommunicationException if a sample could not be read.
    */
   @Nullable
   DataSample getSample() throws CommunicationException;

   /**
    * Returns the current conditions as a data sample, or throws a {@link CommunicationException} if the sample could
    * not be read.
    *
    * @throws CommunicationException if the current sample could not be read.
    */
   @NotNull
   DataSample getCurrentSample() throws CommunicationException;

   /**
    * Requests that the device delete the given <code>dataSample</code>. Returns <code>true</code> upon success,
    * <code>false</code> otherwise, or throws a {@link CommunicationException} if a sample could not be deleted.
    * Returns <code>false</code> if the given <code>dataSample</code> is <code>null</code>.
    *
    * @throws CommunicationException if the sample could not be deleted.
    */
   boolean deleteSample(@Nullable final DataSample dataSample) throws CommunicationException;

   /**
    * Requests that the device delete the sample associated with the given time. Returns <code>true</code> upon
    * success, <code>false</code> otherwise, or throws a {@link CommunicationException} if a sample could not be
    * deleted.
    *
    * @throws CommunicationException if the sample could not be deleted.
    */
   boolean deleteSample(final int sampleTime) throws CommunicationException;

   /** Returns the {@link AirBotConfig configuration} for this <code>AirBot</code>. */
   @NotNull
   AirBotConfig getAirBotConfig();
   }