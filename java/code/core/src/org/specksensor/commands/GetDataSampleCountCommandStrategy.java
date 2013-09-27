package org.specksensor.commands;

import java.nio.ByteBuffer;
import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetDataSampleCountCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<Integer>
   {
   private static final Logger LOG = Logger.getLogger(GetDataSampleCountCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'P';

   @Override
   protected int getSizeOfExpectedResponse()
      {
      return CommandStrategyHelper.getSizeOfExpectedResponse();
      }

   @Override
   protected byte[] getCommand()
      {
      return CommandStrategyHelper.createBaseCommand(COMMAND_PREFIX);
      }

   @Nullable
   @Override
   public Integer convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final byte[] data = response.getData();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("GetDataSampleCountCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            // the count is 4 bytes long and lives in bytes 1-4
            return ByteBuffer.wrap(data, 1, 4).getInt();
            }
         }
      LOG.error("GetDataSampleCountCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }
   }
