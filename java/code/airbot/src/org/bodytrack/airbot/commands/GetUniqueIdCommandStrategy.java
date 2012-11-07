package org.bodytrack.airbot.commands;

import edu.cmu.ri.createlab.serial.CreateLabSerialDeviceReturnValueCommandStrategy;
import edu.cmu.ri.createlab.serial.SerialDeviceCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class GetUniqueIdCommandStrategy extends CreateLabSerialDeviceReturnValueCommandStrategy<String>
   {
   /** The command character used to request the AirBot's unique id. */
   private static final byte[] COMMAND = {'I'};

   /** The size of the expected response, in bytes */
   private static final int SIZE_IN_BYTES_OF_EXPECTED_RESPONSE = 11;

   @Override
   protected int getSizeOfExpectedResponse()
      {
      return SIZE_IN_BYTES_OF_EXPECTED_RESPONSE;
      }

   @Override
   @NotNull
   protected byte[] getCommand()
      {
      return COMMAND.clone();
      }

   @Override
   @Nullable
   public String convertResponse(final SerialDeviceCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final StringBuilder sb = new StringBuilder();
         for (final byte b : response.getData())
            {
            sb.append(ByteUtils.byteToHexString(b));
            }
         return sb.toString();
         }
      return null;
      }
   }
