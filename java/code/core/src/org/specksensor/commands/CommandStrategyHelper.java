package org.specksensor.commands;

import java.nio.ByteBuffer;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.SpeckConstants;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class CommandStrategyHelper
   {
   private static final Logger LOG = Logger.getLogger(CommandStrategyHelper.class);

   /** The size of the expected response, in bytes */
   public static final int SIZE_IN_BYTES_OF_EXPECTED_RESPONSE = 15;

   /** Array index of the data byte array containing the checksum byte */
   private static final int ARRAY_INDEX_OF_CHECKSUM_BYTE = 14;

   static int getSizeOfExpectedResponse()
      {
      return SIZE_IN_BYTES_OF_EXPECTED_RESPONSE;
      }

   /** Returns the given <code>int</code> as a <code>byte</code> array. */
   static byte[] intToByteArray(final int value)
      {
      // convert the int into bytes
      final ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
      b.putInt(value);
      return b.array();
      }

   static byte[] createBaseCommand(final byte commandCharacter)
      {
      // get the current time in seconds, cast it to an int
      final int currentTimeInSecs = (int)(System.currentTimeMillis() / 1000);

      final byte[] timeBytes = intToByteArray(currentTimeInSecs);

      // build the command
      final byte[] command = new byte[SpeckConstants.UsbHidConfiguration.REPORT_LENGTH_IN_BYTES];
      command[0] = commandCharacter;
      command[1] = timeBytes[0];
      command[2] = timeBytes[1];
      command[3] = timeBytes[2];
      command[4] = timeBytes[3];

      command[ARRAY_INDEX_OF_CHECKSUM_BYTE] = computeChecksum(command);

      if (LOG.isTraceEnabled())
         {
         LOG.trace("CommandStrategyHelper.createBaseCommand(): " + byteArrayToString(command) + ")");
         }

      return command;
      }

   static void updateCommandChecksum(@NotNull final byte[] command)
      {
      command[ARRAY_INDEX_OF_CHECKSUM_BYTE] = computeChecksum(command);
      }

   /**
    * Checks the response array for validity, returning <code>true</code> if it's non-<code>null</code>, of the correct
    * length, and the checksum is correct.
    */
   static boolean isResponseDataValid(@Nullable final byte[] data)
      {
      if (LOG.isTraceEnabled())
         {
         LOG.trace("CommandStrategyHelper.isDataValid(" + byteArrayToString(data) + ")");
         }

      if (data != null && data.length == SIZE_IN_BYTES_OF_EXPECTED_RESPONSE)
         {
         // validate the checksum
         final byte actualChecksum = computeChecksum(data);
         final byte expectedChecksum = data[ARRAY_INDEX_OF_CHECKSUM_BYTE];

         final boolean isChecksumValid = actualChecksum == expectedChecksum;
         if (!isChecksumValid && LOG.isDebugEnabled())
            {
            LOG.debug("CommandStrategyHelper.isDataValid(): Checksum failed: expected [" + expectedChecksum + "] actual [" + actualChecksum + "]");
            }

         return isChecksumValid;
         }

      LOG.error("Invalid data array.  The data array cannot be null and must consist of exactly [" + SIZE_IN_BYTES_OF_EXPECTED_RESPONSE + "] bytes.");

      return false;
      }

   private static byte computeChecksum(@NotNull final byte[] data)
      {
      // Speck checksum simply sums all the bytes and then uses the lowest 8 bits
      long sum = 0;
      for (int i = 0; i < SIZE_IN_BYTES_OF_EXPECTED_RESPONSE - 1; i++)
         {
         sum += data[i];
         }

      final ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / 8);
      final byte[] bytes = byteBuffer.putLong(sum).array();

      return bytes[bytes.length - 1];
      }

   static String byteArrayToString(@Nullable final byte[] bytes)
      {
      final StringBuilder s = new StringBuilder("[");
      if (bytes != null)
         {
         for (int i = 0; i < bytes.length; i++)
            {
            s.append(ByteUtils.byteToHexString(bytes[i]));
            if (i < bytes.length - 1)
               {
               s.append(",");
               }
            }
         }
      s.append("]");
      return s.toString();
      }

   private CommandStrategyHelper()
      {
      // private to prevent instantiation
      }
   }
