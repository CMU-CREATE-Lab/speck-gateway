package org.specksensor.commands;

import java.nio.ByteBuffer;
import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.specksensor.DataSample;
import org.specksensor.Speck;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetDataSampleCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<Speck.DataSample>
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
   public Speck.DataSample convertResponse(final HIDCommandResponse response)
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
            return new DataSample(null,
                                  ByteBuffer.wrap(data, 1, 4).getInt(),          // sampleTimeUtcSeconds
                                  ByteBuffer.wrap(data, 12, 2).getShort(),       // rawParticleCount
                                  ByteBuffer.wrap(data, 5, 4).getInt(),          // particleCount
                                  ByteBuffer.wrap(data, 9, 2).getShort(),        // temperature
                                  ByteUtils.unsignedByteToInt(data[11]));        // humidity
            }
         }
      LOG.error("GetDataSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }
   }
