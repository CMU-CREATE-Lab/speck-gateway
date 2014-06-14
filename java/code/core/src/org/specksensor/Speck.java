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

      /**
       * The particle count in particles/Liter or the concentration in 0.1 * ug/m^3.  Specks prior to protocol version 3
       * report particle count. Protocol version 3 and later report concentration.
       *
       * @see ApiSupport#hasParticleCount()
       * @see ApiSupport#hasParticleConcentration()
       */
      int getParticleCountOrConcentration();

      /**
       * Returns the particle concentration in ug/m^3. This is merely a helper method which devices the value returned
       * from {@link #getParticleCountOrConcentration} by 10.  Note that this method only has use for Specks using
       * protocol version 3 or later since Speck prior to protocol 3 report particle count, not concentration.
       *
       * @see #getParticleCountOrConcentration()
       * @see ApiSupport#hasParticleCount()
       * @see ApiSupport#hasParticleConcentration()
       */
      double getParticleConcentration();

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

      /**
       * Returns <code>true</code> if this sample is all zeros, which signifies that there's currently no data
       * available; returns <code>false</code> otherwise.
       */
      boolean isEmpty();

      /**
       * Returns this data sample as a {@link String} containing values separated by commas.  Fields returned depend on
       * the {@link ApiSupport}.  For example, the temperature field will only be included for Specks which record
       * temperature, and particle concentration will only be included for Specks which record particle concentration
       * instead of particle count.
       *
       * @see ApiSupport
       */
      @NotNull
      String toCsv(@NotNull final ApiSupport apiSupport);

      /**
       * Returns this data sample as a JSON array {@link String}. The array does not include the download time.  Fields
       * returned depend on the {@link ApiSupport}.  For example, the temperature field will only be included for Specks
       * which record temperature, and particle concentration will only be included for Specks which record particle
       * concentration instead of particle count.
       *
       * @see ApiSupport
       */
      @NotNull
      String toJsonArray(@NotNull final ApiSupport apiSupport);
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

   /**
    * Puts the Speck in bootloader mode, and forces a disconnect.
    *
    * @throws UnsupportedOperationException if this Speck does not support the command to put it into bootloader mode.
    */
   void enterBootloaderMode();

   /** Returns the {@link SpeckConfig configuration} for this <code>Speck</code>. */
   @NotNull
   SpeckConfig getSpeckConfig();
   }