package org.bodytrack.airbot;

import java.io.File;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceDescriptor;
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
   private static final String DEVICE_COMMON_NAME = "AirBot";

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

   public static final class UsbHidConfiguration
      {
      public static final short USB_VENDOR_ID = 0x2354;
      public static final short USB_PRODUCT_ID = 0x3333;

      public static final int REPORT_LENGTH_IN_BYTES = 16;

      private static final int INPUT_REPORT_LENGTH_IN_BYTES = REPORT_LENGTH_IN_BYTES + 1;  // count includes the report ID
      private static final int OUTPUT_REPORT_LENGTH_IN_BYTES = REPORT_LENGTH_IN_BYTES + 1; // count includes the report ID

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

   private AirBotUploaderConstants()
      {
      // private to prevent instantiation
      }
   }
