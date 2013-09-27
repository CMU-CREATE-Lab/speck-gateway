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
public final class GetSpeckConfigCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<SpeckConfig>
   {
   private static final Logger LOG = Logger.getLogger(GetSpeckConfigCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'I';
   private static final int UNIQUE_ID_STARTING_BYTE_INDEX = 1;
   private static final int UNIQUE_ID_ENDING_BYTE_INDEX = 10;
   private static final int PROTOCOL_VERSION_BYTE_INDEX = 11;
   private static final int LOGGING_INTERVAL_BYTE_INDEX = 12;

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
            LOG.debug("GetSpeckConfigCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            final StringBuilder sb = new StringBuilder();
            for (int i = UNIQUE_ID_STARTING_BYTE_INDEX; i <= UNIQUE_ID_ENDING_BYTE_INDEX; i++)
               {
               sb.append(ByteUtils.byteToHexString(data[i]));
               }
            return new SpeckConfigImpl(sb.toString(), data[PROTOCOL_VERSION_BYTE_INDEX], data[LOGGING_INTERVAL_BYTE_INDEX]);
            }
         }
      LOG.error("GetSpeckConfigCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
      return null;
      }

   private static final class SpeckConfigImpl implements SpeckConfig
      {
      @NotNull
      private final String id;
      private final int protocolVersion;
      private final int loggingInterval;

      @NotNull
      private final ApiSupport apiSupport;

      private SpeckConfigImpl(@NotNull final String id, final int protocolVersion, final int loggingInterval)
         {
         this.id = id;
         this.protocolVersion = protocolVersion;
         apiSupport = ApiSupport.getInstance(protocolVersion);
         this.loggingInterval = apiSupport.hasMutableLoggingInterval() ? loggingInterval : ApiSupport.DEFAULT_LOGGING_INTERVAL_SECONDS;
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
         sb.append(", loggingInterval=").append(loggingInterval);
         sb.append('}');
         return sb.toString();
         }
      }
   }
