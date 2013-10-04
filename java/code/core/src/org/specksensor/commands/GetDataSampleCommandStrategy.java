package org.specksensor.commands;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

   private static final byte IS_GPS_VALID = (byte)'T';

   private static final Map<Byte, String> QUADRANT_CONVERSION_MAP;

   static
      {
      final Map<Byte, String> quadrantConversionMap = new HashMap<Byte, String>(4);
      quadrantConversionMap.put((byte)5, "NE");
      quadrantConversionMap.put((byte)6, "SE");
      quadrantConversionMap.put((byte)9, "NW");
      quadrantConversionMap.put((byte)10, "SW");
      QUADRANT_CONVERSION_MAP = Collections.unmodifiableMap(quadrantConversionMap);
      }

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
            final String latitude = String.valueOf(ByteBuffer.wrap(data, 13, 4).getInt()) +
                                    "." +
                                    String.valueOf(ByteBuffer.wrap(data, 17, 4).getInt());
            final String longitude = String.valueOf(ByteBuffer.wrap(data, 21, 4).getInt()) +
                                     "." +
                                     String.valueOf(ByteBuffer.wrap(data, 25, 4).getInt());

            return new DataSample(null,
                                  ByteBuffer.wrap(data, 1, 4).getInt(),          // sampleTimeUtcSeconds
                                  ByteBuffer.wrap(data, 10, 2).getShort(),       // rawParticleCount
                                  ByteBuffer.wrap(data, 5, 4).getInt(),          // particleCount
                                  0,                                             // temperature (always zero for GPS Speck)
                                  ByteUtils.unsignedByteToInt(data[9]),          // humidity
                                  data[12] == IS_GPS_VALID,                      // isGpsValid
                                  latitude,                                      // latitude
                                  longitude,                                     // longitude
                                  QUADRANT_CONVERSION_MAP.get(data[29]));        // gpsQuadrant
            }
         }
      LOG.error("GetDataSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }
   }
