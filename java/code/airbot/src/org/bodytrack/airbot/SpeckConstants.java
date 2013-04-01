package org.bodytrack.airbot;

import edu.cmu.ri.createlab.usb.hid.HIDDeviceDescriptor;

/**
 * <p>
 * <code>SpeckConstants</code> defines various constants for the Speck.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class SpeckConstants
   {
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

   private SpeckConstants()
      {
      // private to prevent instantiation
      }
   }
