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
         if (LOG.isDebugEnabled())
            {
            LOG.debug("GetDataSampleCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(response.getData()));
            }

         return AirBotDataSample.create(response.getData());
         }
      LOG.error("GetDataSampleCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }

   private static final class AirBotDataSample implements AirBot.DataSample
      {
      @Nullable
      public static AirBotDataSample create(final byte[] data)
         {
         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            return new AirBotDataSample(data);
            }

         return null;
         }

      private final int sampleTime;
      private final long downloadTime = System.currentTimeMillis();
      private final int particleCount;
      private final int temperature;
      private final int humidity;

      private AirBotDataSample(final byte[] data)
         {
         sampleTime = ByteBuffer.wrap(data, 1, 4).getInt();
         particleCount = ByteBuffer.wrap(data, 5, 4).getInt();
         temperature = ByteBuffer.wrap(data, 9, 2).getShort();
         humidity = ByteUtils.unsignedByteToInt(data[11]);
         }

      @Override
      public int getSampleTime()
         {
         return sampleTime;
         }

      @Override
      public long getDownloadTime()
         {
         return downloadTime;
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
      public boolean isEmpty()
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

         final AirBotDataSample that = (AirBotDataSample)o;

         if (downloadTime != that.downloadTime)
            {
            return false;
            }
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
         int result = sampleTime;
         result = 31 * result + (int)(downloadTime ^ (downloadTime >>> 32));
         result = 31 * result + particleCount;
         result = 31 * result + temperature;
         result = 31 * result + humidity;
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder("AirBotDataSample{");
         sb.append("sampleTime=").append(sampleTime);
         sb.append(", downloadTime=").append(downloadTime);
         sb.append(", particleCount=").append(particleCount);
         sb.append(", temperature=").append(temperature);
         sb.append(", humidity=").append(humidity);
         sb.append('}');
         return sb.toString();
         }
      }
   }
