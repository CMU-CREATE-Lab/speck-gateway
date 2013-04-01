package org.bodytrack.airbot.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBotConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetAirBotConfigCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<AirBotConfig>
   {
   private static final Logger LOG = Logger.getLogger(GetAirBotConfigCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'I';
   private static final int UNIQUE_ID_STARTING_BYTE_INDEX = 1;
   private static final int UNIQUE_ID_ENDING_BYTE_INDEX = 10;
   private static final int PROTOCOL_VERSION_BYTE_INDEX = 11;

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
   public AirBotConfig convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final byte[] data = response.getData();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("GetAirBotConfigCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            final StringBuilder sb = new StringBuilder();
            for (int i = UNIQUE_ID_STARTING_BYTE_INDEX; i <= UNIQUE_ID_ENDING_BYTE_INDEX; i++)
               {
               sb.append(ByteUtils.byteToHexString(data[i]));
               }
            return new AirBotConfigImpl(sb.toString(), data[PROTOCOL_VERSION_BYTE_INDEX]);
            }
         }
      LOG.error("GetAirBotConfigCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }

   private static final class AirBotConfigImpl implements AirBotConfig
      {
      @NotNull
      private final String id;
      private final int protocolVersion;

      private AirBotConfigImpl(@NotNull final String id, final int protocolVersion)
         {
         this.id = id;
         this.protocolVersion = protocolVersion;
         }

      @Override
      @NotNull
      public String getId()
         {
         return id;
         }

      @Override
      public int getProtocolVersion()
         {
         return protocolVersion;
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

         final AirBotConfigImpl that = (AirBotConfigImpl)o;

         if (protocolVersion != that.protocolVersion)
            {
            return false;
            }
         if (!id.equals(that.id))
            {
            return false;
            }

         return true;
         }

      @Override
      public int hashCode()
         {
         int result = id.hashCode();
         result = 31 * result + protocolVersion;
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder("AirBotConfig{");
         sb.append("id='").append(id).append('\'');
         sb.append(", protocolVersion=").append(protocolVersion);
         sb.append('}');
         return sb.toString();
         }
      }
   }
