package org.specksensor.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.ApiSupport;
import org.specksensor.SpeckConfig;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ReadExtendedSpeckConfigCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<SpeckConfig>
   {
   private static final Logger LOG = Logger.getLogger(ReadExtendedSpeckConfigCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'i';
   private static final int UNIQUE_ID_STARTING_BYTE_INDEX = 1;
   private static final int UNIQUE_ID_ENDING_BYTE_INDEX = 8;

   @NotNull
   private final SpeckConfig baseSpeckConfig;

   public ReadExtendedSpeckConfigCommandStrategy(@NotNull final SpeckConfig baseSpeckConfig)
      {
      this.baseSpeckConfig = baseSpeckConfig;
      }

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
   public SpeckConfig convertResponse(final HIDCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         final byte[] data = response.getData();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("ReadExtendedSpeckConfigCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            final StringBuilder sb = new StringBuilder();
            for (int i = UNIQUE_ID_STARTING_BYTE_INDEX; i <= UNIQUE_ID_ENDING_BYTE_INDEX; i++)
               {
               sb.append(ByteUtils.byteToHexString(data[i]));
               }

            return new SpeckConfigImpl(sb.toString(), baseSpeckConfig);
            }
         }
      LOG.error("ReadExtendedSpeckConfigCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }

   private static final class SpeckConfigImpl implements SpeckConfig
      {
      @NotNull
      private final String id;
      private final int protocolVersion;
      private final int hardwareVersion;
      private final int firmwareVersion;
      private final int loggingInterval;

      @NotNull
      private final ApiSupport apiSupport;

      private SpeckConfigImpl(@NotNull final String id,
                              @NotNull final SpeckConfig baseSpeckConfig)
         {
         this.id = baseSpeckConfig.getId() + id;
         this.protocolVersion = baseSpeckConfig.getProtocolVersion();
         this.apiSupport = baseSpeckConfig.getApiSupport();
         this.hardwareVersion = baseSpeckConfig.getHardwareVersion();
         this.firmwareVersion = baseSpeckConfig.getFirmwareVersion();
         this.loggingInterval = baseSpeckConfig.getLoggingInterval();
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
      public int getHardwareVersion()
         {
         return hardwareVersion;
         }

      @Override
      public int getFirmwareVersion()
         {
         return firmwareVersion;
         }

      @Override
      public int getLoggingInterval()
         {
         return loggingInterval;
         }

      @NotNull
      @Override
      public ApiSupport getApiSupport()
         {
         return apiSupport;
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

         final SpeckConfigImpl that = (SpeckConfigImpl)o;

         if (firmwareVersion != that.firmwareVersion)
            {
            return false;
            }
         if (hardwareVersion != that.hardwareVersion)
            {
            return false;
            }
         if (loggingInterval != that.loggingInterval)
            {
            return false;
            }
         if (protocolVersion != that.protocolVersion)
            {
            return false;
            }
         if (!apiSupport.equals(that.apiSupport))
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
         result = 31 * result + hardwareVersion;
         result = 31 * result + firmwareVersion;
         result = 31 * result + loggingInterval;
         result = 31 * result + apiSupport.hashCode();
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder("SpeckConfig{");
         sb.append("id='").append(id).append('\'');
         sb.append(", protocolVersion=").append(protocolVersion);
         sb.append(", hardwareVersion=").append(hardwareVersion);
         sb.append(", firmwareVersion=").append(firmwareVersion);
         sb.append(", loggingInterval=").append(loggingInterval);
         sb.append('}');
         return sb.toString();
         }
      }
   }
