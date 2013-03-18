package org.bodytrack.airbot.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.DataSample;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetDataSampleCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<AirBot.DataSample>
   {
   private static final Logger LOG = Logger.getLogger(GetDataSampleCommandStrategy.class);

   private static final byte HISTORIC_SAMPLE_COMMAND_PREFIX = 'G';
   private static final byte CURRENT_SAMPLE_COMMAND_PREFIX = 'S';

   public static GetDataSampleCommandStrategy createGetHistoricSampleCommandStrategy()
      {
      return new GetDataSampleCommandStrategy(HISTORIC_SAMPLE_COMMAND_PREFIX);
      }

   public static GetDataSampleCommandStrategy createGetCurrentSampleCommandStrategy()
      {
      return new GetDataSampleCommandStrategy(CURRENT_SAMPLE_COMMAND_PREFIX);
      }

   private final byte commandCharacter;

   private GetDataSampleCommandStrategy(final byte commandCharacter)
      {
      this.commandCharacter = commandCharacter;
      }

   @Override
   protected int getSizeOfExpectedResponse()
      {
      return CommandStrategyHelper.getSizeOfExpectedResponse();
      }

   @Override
   protected byte[] getCommand()
      {
      return CommandStrategyHelper.createBaseCommand(commandCharacter);
      }

   @Nullable
   @Override
   public AirBot.DataSample convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final byte[] data = response.getData();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("GetDataSampleCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            return new DataSample(data);
            }
         }
      LOG.error("GetDataSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }
   }
