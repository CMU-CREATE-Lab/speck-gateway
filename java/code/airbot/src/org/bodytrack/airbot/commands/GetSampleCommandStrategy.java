package org.bodytrack.airbot.commands;

import java.nio.ByteBuffer;
import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBot;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetSampleCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<AirBot.Sample>
   {
   private static final Logger LOG = Logger.getLogger(GetSampleCommandStrategy.class);

   private static final byte HISTORIC_SAMPLE_COMMAND_PREFIX = 'G';
   private static final byte CURRENT_SAMPLE_COMMAND_PREFIX = 'S';

   public static GetSampleCommandStrategy createGetHistoricSampleCommandStrategy()
      {
      return new GetSampleCommandStrategy(HISTORIC_SAMPLE_COMMAND_PREFIX);
      }

   public static GetSampleCommandStrategy createGetCurrentSampleCommandStrategy()
      {
      return new GetSampleCommandStrategy(CURRENT_SAMPLE_COMMAND_PREFIX);
      }

   private final byte commandCharacter;

   private GetSampleCommandStrategy(final byte commandCharacter)
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
   public AirBot.Sample convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         if (LOG.isDebugEnabled())
            {
            LOG.debug("GetSampleCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(response.getData()));
            }

         return AirBotSample.create(response.getData());
         }
      LOG.error("GetSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }

   private static final class AirBotSample implements AirBot.Sample
      {
      @Nullable
      public static AirBotSample create(final byte[] data)
         {
         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            return new AirBotSample(data);
            }

         return null;
         }

      private final long sampleTime;
      private final int particleCount;
      private final int temperature;
      private final int humidity;

      private AirBotSample(final byte[] data)
         {
         sampleTime = ByteBuffer.wrap(data, 1, 4).getInt();
         particleCount = ByteBuffer.wrap(data, 5, 4).getInt();
         temperature = ByteBuffer.wrap(data, 9, 2).getShort();
         humidity = ByteUtils.unsignedByteToInt(data[11]);
         }

      @Override
      public long getSampleTime()
         {
         return sampleTime;
         }

      @Override
      public int getParticleCount()
         {
         return particleCount;
         }

      @Override
      public int getTemperature()
         {
         return temperature;
         }

      @Override
      public int getHumidity()
         {
         return humidity;
         }

      @Override
      public boolean isNoDataAvailable()
         {
         return sampleTime == 0 && particleCount == 0 && temperature == 0 && humidity == 0;
         }

      @Override
      public boolean equals(final Object o)
         {
         if (this == o)
            {
            return true;
            }
         if (o == null || getClass() != o.getClass())
            {
            return false;
            }

         final AirBotSample that = (AirBotSample)o;

         if (humidity != that.humidity)
            {
            return false;
            }
         if (particleCount != that.particleCount)
            {
            return false;
            }
         if (sampleTime != that.sampleTime)
            {
            return false;
            }
         if (temperature != that.temperature)
            {
            return false;
            }

         return true;
         }

      @Override
      public int hashCode()
         {
         int result = (int)(sampleTime ^ (sampleTime >>> 32));
         result = 31 * result + particleCount;
         result = 31 * result + temperature;
         result = 31 * result + humidity;
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("AirBotSample");
         sb.append("{sampleTime=").append(sampleTime);
         sb.append(", particleCount=").append(particleCount);
         sb.append(", temperature=").append(temperature);
         sb.append(", humidity=").append(humidity);
         sb.append('}');
         return sb.toString();
         }
      }
   }
