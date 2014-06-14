package org.specksensor.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDReturnValueCommandStrategy;
import edu.cmu.ri.createlab.usb.hid.HIDCommandResponse;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.ApiSupport;
import org.specksensor.SpeckConfig;
import org.specksensor.SpeckConstants;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ReadWriteSpeckConfigCommandStrategy extends CreateLabHIDReturnValueCommandStrategy<SpeckConfig>
   {
   private static final Logger LOG = Logger.getLogger(ReadWriteSpeckConfigCommandStrategy.class);

   private static final byte COMMAND_PREFIX = 'I';
   private static final int UNIQUE_ID_STARTING_BYTE_INDEX = 1;
   private static final int UNIQUE_ID_ENDING_BYTE_INDEX = 10;
   private static final int UNIQUE_ID_ENDING_BYTE_INDEX_EXTENDED_ID = 8;
   private static final int PROTOCOL_VERSION_BYTE_INDEX = 11;
   private static final int HARDWARE_VERSION_BYTE_INDEX = 10;
   private static final int FIRMWARE_VERSION_BYTE_INDEX = 13;
   private static final int LOGGING_INTERVAL_BYTE_INDEX_WHEN_READING = 12;
   private static final int LOGGING_INTERVAL_BYTE_INDEX_WHEN_WRITING = 5;
   private final int loggingIntervalInSeconds;

   // A logging interval of 0 will cause this strategy to be read only
   private static final ReadWriteSpeckConfigCommandStrategy READ_ONLY_INSTANCE = new ReadWriteSpeckConfigCommandStrategy(0);

   /**
    * Returns a <code>ReadWriteSpeckConfigCommandStrategy</code> which can be used to read the Speck's config.  No
    * config values will be changed.
    */
   public static ReadWriteSpeckConfigCommandStrategy createReadableSpeckConfigCommandStrategy()
      {
      return READ_ONLY_INSTANCE;
      }

   /**
    * Clamps the given <code>loggingIntervalInSeconds</code> to the range
    * [{@link SpeckConstants.LoggingInterval#MIN}, {@link SpeckConstants.LoggingInterval#MAX}] and then returns a
    * <code>ReadWriteSpeckConfigCommandStrategy</code> which can be used to set the logging interval and then read the
    * new config.
    */
   public static ReadWriteSpeckConfigCommandStrategy createWriteableSpeckConfigCommandStrategy(final int loggingIntervalInSeconds)
      {
      return new ReadWriteSpeckConfigCommandStrategy(Math.min(SpeckConstants.LoggingInterval.MAX,
                                                              Math.max(loggingIntervalInSeconds, SpeckConstants.LoggingInterval.MIN)));
      }

   private ReadWriteSpeckConfigCommandStrategy(final int loggingIntervalInSeconds)
      {
      this.loggingIntervalInSeconds = loggingIntervalInSeconds;
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

      // The base command defaults to a logging interval of 0, so don't bother setting it if the requested value is 0.
      if (loggingIntervalInSeconds != 0)
         {
         // copy the logging interval to the command
         command[LOGGING_INTERVAL_BYTE_INDEX_WHEN_WRITING] = ByteUtils.intToUnsignedByte(this.loggingIntervalInSeconds);

         // update the command checksum
         CommandStrategyHelper.updateCommandChecksum(command);

         if (LOG.isDebugEnabled())
            {
            LOG.debug("ReadWriteSpeckConfigCommandStrategy.getCommand(): command = " + CommandStrategyHelper.byteArrayToString(command));
            }
         }

      return command;
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
            LOG.debug("ReadWriteSpeckConfigCommandStrategy.convertResponse(): response = " + CommandStrategyHelper.byteArrayToString(data));
            }

         if (CommandStrategyHelper.isResponseDataValid(data))
            {
            final ApiSupport apiSupport = ApiSupport.getInstance(data[PROTOCOL_VERSION_BYTE_INDEX]);

            final int uniqueIdEndingByteIndex = apiSupport.hasExtendedId() ? UNIQUE_ID_ENDING_BYTE_INDEX_EXTENDED_ID : UNIQUE_ID_ENDING_BYTE_INDEX;
            final StringBuilder sb = new StringBuilder();
            for (int i = UNIQUE_ID_STARTING_BYTE_INDEX; i <= uniqueIdEndingByteIndex; i++)
               {
               sb.append(ByteUtils.byteToHexString(data[i]));
               }

            return new SpeckConfigImpl(sb.toString(),
                                       apiSupport,
                                       ByteUtils.unsignedByteToInt(data[HARDWARE_VERSION_BYTE_INDEX]),
                                       ByteUtils.unsignedByteToInt(data[FIRMWARE_VERSION_BYTE_INDEX]),
                                       ByteUtils.unsignedByteToInt(data[LOGGING_INTERVAL_BYTE_INDEX_WHEN_READING])
            );
            }
         }
      LOG.error("ReadWriteSpeckConfigCommandStrategy.convertResponse(): Failure!  response = [" + response + "]");
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
                              @NotNull final ApiSupport apiSupport,
                              final int hardwareVersion,
                              final int firmwareVersion,
                              final int loggingInterval)
         {
         this.id = id;
         this.protocolVersion = apiSupport.getProtocolVersion();
         this.apiSupport = apiSupport;
         if (getApiSupport().hasDeviceVersionInfo())
            {
            this.hardwareVersion = hardwareVersion;
            this.firmwareVersion = firmwareVersion;
            }
         else
            {
            this.hardwareVersion = SpeckConfig.UNKNOWN_VERSION;
            this.firmwareVersion = SpeckConfig.UNKNOWN_VERSION;
            }
         this.loggingInterval = apiSupport.canMutateLoggingInterval() ? loggingInterval : SpeckConstants.LoggingInterval.DEFAULT;
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
