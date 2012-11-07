package org.bodytrack.airbot;

import java.io.File;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>AirBotUploaderConstants</code> defines various constants for the AirBot Uploader.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class AirBotUploaderConstants
   {
   public static final class FilePaths
      {
      public static final File BODYTRACK_HOME_DIRECTORY = new File(System.getProperty("user.home") + File.separator + "BodyTrack" + File.separator);
      public static final File AIRBOT_ROOT_DATA_DIRECTORY = new File(BODYTRACK_HOME_DIRECTORY, "AirBotData");

      static
         {
         // make sure the data directory exists
         //noinspection ResultOfMethodCallIgnored
         AIRBOT_ROOT_DATA_DIRECTORY.mkdirs();
         }

      /**
       * Creates (if necessary) and returns the directory into which data files for the given {@link AirBotConfig}
       * should be stored.
       */
      @NotNull
      public static File getDeviceDataDirectory(@NotNull final AirBotConfig airBotConfig)
         {
         final File deviceDataFileDirectory = new File(AIRBOT_ROOT_DATA_DIRECTORY, "AirBot" + airBotConfig.getId());

         // make sure the directory exists
         //noinspection ResultOfMethodCallIgnored
         deviceDataFileDirectory.mkdirs();

         return deviceDataFileDirectory;
         }

      private FilePaths()
         {
         // private to prevent instantiation
         }
      }

   private AirBotUploaderConstants()
      {
      // private to prevent instantiation
      }
   }
