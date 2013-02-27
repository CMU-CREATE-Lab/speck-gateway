package org.bodytrack.airbot;

import edu.cmu.ri.createlab.device.CreateLabDeviceProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface AirBot extends CreateLabDeviceProxy
   {
   interface Sample
      {
      /** The time the sample was taken, in UTC millis since the epoch. */
      long getSampleTime();

      /** The particle count in particles/Liter. */
      int getParticleCount();

      /** The temperature in 1/10 degrees F. */
      int getTemperature();

      /** The humidity in percent relative humidity. */
      int getHumidity();

      /**
       * Returns <code>true</code> if this sample is all zeros, which signifies that there's currently no data
       * available; returns <code>false</code> otherwise.
       */
      boolean isNoDataAvailable();
      }

   /**
    * Returns a data sample, perhaps from some time in the past, or returns <code>null</code> if no data is available.
    * Throws a {@link CommunicationException} if a sample could not be read.
    *
    * @throws CommunicationException if a sample could not be read.
    */
   @Nullable
   Sample getSample() throws CommunicationException;

   /**
    * Returns the current conditions as a data sample, or throws a {@link CommunicationException} if a sample could not be
    * read.
    *
    * @throws CommunicationException if a sample could not be read.
    */
   @NotNull
   Sample getCurrentSample() throws CommunicationException;

   /**
    * Requests that the device deletes the sample associated with the given time. Returns <code>true</code> upon
    * success, <code>false</code> otherwise, or throws a {@link CommunicationException} if a sample could not be
    * deleted.
    *
    * @throws CommunicationException if a sample could not be read.
    */
   boolean deleteSample(final int time) throws CommunicationException;

   /**
    * Retrieves a {@link DataFile} from the AirBot specified by the given <code>filename</code>.  Returns a
    * {@link DataFile} for the file upon success, or returns <code>null</code> if the command failed or the given
    * <code>filename</code> is <code>null</code>.  If there is no such file available from the AirBot, this
    * method will throw a {@link NoSuchFileException}.
    *
    * @throws NoSuchFileException if the AirBot does not have a file with the given <code>filename</code>
    */
   @Nullable
   DataFile getFile(@Nullable final String filename) throws NoSuchFileException;

   /**
    * Requests that the device deletes the file specified by the given <code>filename</code>.  Returns <code>true</code>
    * upon success, <code>false</code> otherwise.
    */
   boolean deleteFile(@Nullable final String filename);

   /** Returns the {@link AirBotConfig configuration} for this <code>AirBot</code>. */
   @NotNull
   AirBotConfig getAirBotConfig();
   }