package org.specksensor;

import java.io.File;
import edu.cmu.ri.createlab.CreateLabConstants;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>SpeckConstants</code> defines various constants for the Speck.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class SpeckConstants
   {
   public static final class LoggingInterval
      {
      /** Default logging interval in seconds. */
      public static final int DEFAULT = 1;

      /** Minimum logging interval in seconds. */
      public static final int MIN = 1;

      /** Maximum logging interval in seconds. */
      public static final int MAX = 255;

      private LoggingInterval()
         {
         // private to prevent instantiation
         }
      }

   public static final class UsbHidConfiguration
      {
      public static final short USB_VENDOR_ID = 0x2354;
      public static final short USB_PRODUCT_ID = 0x3333;

      public static final int REPORT_LENGTH_IN_BYTES = 16;

      private static final int INPUT_REPORT_LENGTH_IN_BYTES = REPORT_LENGTH_IN_BYTES + 1;  // count includes the report ID
      private static final int OUTPUT_REPORT_LENGTH_IN_BYTES = REPORT_LENGTH_IN_BYTES + 1; // count includes the report ID

      private static final String DEVICE_COMMON_NAME = "Speck";

      public static final HIDDeviceDescriptor HID_DEVICE_DESCRIPTOR = new HIDDeviceDescriptor(USB_VENDOR_ID,
                                                                                              USB_PRODUCT_ID,
                                                                                              INPUT_REPORT_LENGTH_IN_BYTES,
                                                                                              OUTPUT_REPORT_LENGTH_IN_BYTES,
                                                                                              DEVICE_COMMON_NAME);

      private UsbHidConfiguration()
         {
         // private to prevent instantiation
         }
      }

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

   private SpeckConstants()
      {
      // private to prevent instantiation
      }
   }
