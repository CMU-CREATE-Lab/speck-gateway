package org.specksensor;

import edu.cmu.ri.createlab.device.CreateLabDeviceProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface Speck extends CreateLabDeviceProxy
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

      /** The raw particle count. */
      int getRawParticleCount();

      /** The particle count in particles/Liter. */
      int getParticleCount();

      /**
       * The temperature in degrees F.  For Specks which don't support temperature, this will probably be 0, but it's
       * safest to just consider it undefined.
       *
       * @see ApiSupport#hasTemperatureSensor()
       */
      double getTemperatureInDegreesF();

      /**
       * The temperature in 1/10 degrees F.  For Specks which don't support temperature, this will probably be 0, but
       * it's safest to just consider it undefined.
       *
       * @see ApiSupport#hasTemperatureSensor()
       */
      int getTemperatureInTenthsOfADegreeF();

      /** The humidity in percent relative humidity. */
      int getHumidity();

      /** Returns <code>true</code> if the latitude/longitude should be considered valid; false otherwise. */
      boolean isGpsValid();

      /**
       * Returns the GPS latitude, or <code>null</code> if the GPS data is invalid.
       *
       * @see #isGpsValid()
       */
      @Nullable
      String getLatitude();

      /**
       * Returns the GPS longitude, or <code>null</code> if the GPS data is invalid.
       *
       * @see #isGpsValid()
       */
      @Nullable
      String getLongitude();

      /**
       * Returns the GPS quadrant (N, S, E, W), or <code>null</code> if the GPS data is invalid.
       *
       * @see #isGpsValid()
       */
      @Nullable
      String getGpsQuadrant();

      /**
       * Returns <code>true</code> if this sample has a time of 0, which signifies that there's currently no data
       * available; returns <code>false</code> otherwise.
       */
      boolean isEmpty();

      /**
       * Returns this data sample as a {@link String} containing values separated by commas.  If
       * <code>includeTemperature</code> is <code>true</code>, values are returned in this order: <code>sample time,
       * particle count, temperature, humidity, download time</code>; if <code>false</code>, the temperature field is
       * not included.
       *
       * @see ApiSupport#hasTemperatureSensor()
       */
      @NotNull
      String toCsv(final boolean includeTemperature);

      /**
       * Returns this data sample as a JSON array {@link String}. The array does not include the download time.  If
       * <code>includeTemperature</code> is <code>true</code>, values are returned in this order: <code>sample time,
       * particle count, temperature, humidity</code>; if <code>false</code>, the temperature field is not included.
       *
       * @see ApiSupport#hasTemperatureSensor()
       */
      @NotNull
      String toJsonArray(final boolean includeTemperature);
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

   /**
    * Returns the number of available data samples.  Throws a {@link CommunicationException} if the number of available
    * samples could not be read due to an error.  Throws an {@link UnsupportedOperationException} if this Speck cannot
    * report the number of available samples.
    *
    * @throws CommunicationException if the number of available samples could not be read due to an error.
    * @throws UnsupportedOperationException if this Speck cannot report the number of available samples.
    *
    * @see ApiSupport#canGetNumberOfDataSamples()
    */
   int getNumberOfAvailableSamples() throws CommunicationException, UnsupportedOperationException;

   /**
    * Sets the logging interval to the given interval, clamped to the range
    * [{@link SpeckConstants.LoggingInterval#MIN}, {@link SpeckConstants.LoggingInterval#MAX}]. Returns the new
    * {@link SpeckConfig} upon success, throws an exception otherwise.  Throws a {@link CommunicationException} if the
    * logging interval could not be set due to an error.  Throws an {@link UnsupportedOperationException} if this Speck
    * does not support setting of the logging interval.
    *
    * @throws CommunicationException if the logging intervalcould not be set due to an error.
    * @throws UnsupportedOperationException if this Speck does not support setting of the logging interval.
    *
    * @see ApiSupport#canMutateLoggingInterval()
    */
   @NotNull
   SpeckConfig setLoggingInterval(final int loggingIntervalInSeconds) throws CommunicationException, UnsupportedOperationException;

   /** Returns the {@link SpeckConfig configuration} for this <code>Speck</code>. */
   @NotNull
   SpeckConfig getSpeckConfig();
   }