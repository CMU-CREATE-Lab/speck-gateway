package org.bodytrack.airbot.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DeleteSampleCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<Boolean>
   {
   private static final Logger LOG = Logger.getLogger(DeleteSampleCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'D';
   private final byte[] sampleTimeBytes;

   public DeleteSampleCommandStrategy(final int sampleTime)
      {
      // get the sample time as a byte array
      sampleTimeBytes = CommandStrategyHelper.intToByteArray(sampleTime);
      }

   @Override
   protected int getSizeOfExpectedResponse()
      {
      return CommandStrategyHelper.getSizeOfExpectedResponse();
      }

   @Override
   protected byte[] getCommand()
      {
      // create the base command
      final byte[] command = CommandStrategyHelper.createBaseCommand(COMMAND_PREFIX);

      // copy the sample time to the command
      command[5] = sampleTimeBytes[0];
      command[6] = sampleTimeBytes[1];
      command[7] = sampleTimeBytes[2];
      command[8] = sampleTimeBytes[3];

      // update the command checksum
      CommandStrategyHelper.updateCommandChecksum(command);

      if (LOG.isDebugEnabled())
         {
         LOG.debug("DeleteSampleCommandStrategy.getCommand(): command = " + CommandStrategyHelper.byteArrayToString(command));
         }

      return command;
      }

   @Nullable
   @Override
   public Boolean convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final byte[] data = response.getData();
         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            if (LOG.isDebugEnabled())
               {
               LOG.debug("DeleteSampleCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
               }

            // make sure the delete was successful and that the requested sample time matches the deleted sample time
            return data[5] == 1 &&
                   data[1] == sampleTimeBytes[0] &&
                   data[2] == sampleTimeBytes[1] &&
                   data[3] == sampleTimeBytes[2] &&
                   data[4] == sampleTimeBytes[3];
            }
         }
      LOG.error("DeleteSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }
   }
