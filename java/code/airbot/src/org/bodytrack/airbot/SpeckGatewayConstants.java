package org.bodytrack.airbot;

import java.io.File;
import edu.cmu.ri.createlab.CreateLabConstants;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>SpeckGatewayConstants</code> defines various constants for the Speck Gateway.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class SpeckGatewayConstants
   {
   public static final class FilePaths
      {
      private static final File SPECK_ROOT_DATA_DIRECTORY = new File(CreateLabConstants.FilePaths.CREATE_LAB_HOME_DIR, "Speck");

      static
         {
         // make sure the data directory exists
         //noinspection ResultOfMethodCallIgnored
         SPECK_ROOT_DATA_DIRECTORY.mkdirs();
         }

      /**
       * Creates (if necessary) and returns the directory into which data files for the given {@link SpeckConfig}
       * should be stored.
       */
      @NotNull
      public static File getDeviceDataDirectory(@NotNull final SpeckConfig speckConfig)
         {
         final File deviceDataFileDirectory = new File(SPECK_ROOT_DATA_DIRECTORY, "Speck" + speckConfig.getId());

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

   private SpeckGatewayConstants()
      {
      // private to prevent instantiation
      }
   }
