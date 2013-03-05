package org.bodytrack.applications.airbot;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.TreeSet;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotFactory;
import org.bodytrack.airbot.CommunicationException;
import org.bodytrack.airbot.DataSampleManager;
import org.bodytrack.airbot.DataSampleUploader;
import org.bodytrack.airbot.DataSampleDownloader;
import org.bodytrack.airbot.InitializationException;
import org.bodytrack.airbot.RemoteStorageCredentials;
import org.bodytrack.airbot.RemoteStorageCredentialsValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class AirBotUploaderHelper
   {
   private static final Logger LOG = Logger.getLogger(AirBotUploaderHelper.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   interface EventListener
      {
      void handleConnectionEvent(@NotNull final AirBotConfig airBotConfig, @NotNull final String portName);

      void handlePingFailureEvent();
      }

   private static void logInfo(@NotNull final String message)
      {
      LOG.info(message);
      CONSOLE_LOG.info(message);
      }

   private static void logError(@NotNull final String message)
      {
      LOG.error(message);
      CONSOLE_LOG.error(message);
      }

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(AirBotUploaderHelper.class.getName());

   public static final String APPLICATION_NAME = RESOURCES.getString("application.name");
   public static final String VERSION_NUMBER = RESOURCES.getString("version.number");
   public static final String APPLICATION_NAME_AND_VERSION_NUMBER = APPLICATION_NAME + " v" + VERSION_NUMBER;

   @Nullable
   private AirBot device;

   @Nullable
   private DataSampleManager dataSampleManager;

   @Nullable
   private RemoteStorageCredentials remoteStorageCredentials;

   @NotNull
   private final EventListener eventListener;

   @Nullable
   private final String pathToConfigFile;

   @NotNull
   private final CreateLabDevicePingFailureEventListener pingFailureEventListener =
         new CreateLabDevicePingFailureEventListener()
         {
         public void handlePingFailureEvent()
            {
            LOG.debug("AirBotUploaderHelper.handlePingFailureEvent(): ping failure detected, cleaning up...");

            logError("Connection failure detected.  Cleaning up...");
            disconnect(false);

            LOG.debug("AirBotUploaderHelper.handlePingFailureEvent(): ping failure detected, attempting reconnect...");

            // notify listener of the ping failure
            eventListener.handlePingFailureEvent();

            logInfo("Now attempting to reconnect to the device...");
            scanAndConnect();
            }
         };

   AirBotUploaderHelper(@NotNull final EventListener eventListener)
      {
      this(eventListener, null);
      }

   AirBotUploaderHelper(@NotNull final EventListener eventListener, @Nullable final String pathToConfigFile)
      {
      this.eventListener = eventListener;
      this.pathToConfigFile = pathToConfigFile;
      }

   public boolean isConnected()
      {
      return device != null;
      }

   public void scanAndConnect()
      {
      if (isConnected())
         {
         logInfo("You are already connected to an AirBot.");
         }
      else
         {
         if (isDownloadDisabled())
            {
            logInfo("Loading config file...");
            device = createFakeAirBot();
            }
         else
            {
            logInfo("Scanning for an AirBot...");
            device = AirBotFactory.create();
            }

         if (device == null)
            {
            logError("Connection failed.");
            }
         else
            {
            device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
            final AirBotConfig airBotConfig = device.getAirBotConfig();

            final DataSampleDownloader dataSampleDownloader;
            if (isDownloadDisabled())
               {
               logInfo("Data files will not be downloaded from an AirBot since you specified a config file for AirBot [" + airBotConfig.getId() + "]");
               dataSampleDownloader = null;
               }
            else
               {
               logInfo("Connection successful to AirBot [" + airBotConfig.getId() + "] on serial port [" + device.getPortName() + "].");
               dataSampleDownloader = new DataSampleDownloader(device);
               }

            logInfo("Starting up the DataSampleManager...");
            try
               {
               dataSampleManager = new DataSampleManager(airBotConfig, dataSampleDownloader);
               }
            catch (InitializationException e)
               {
               LOG.error("AirBotUploaderHelper.scanAndConnect(): InitializationException while trying to create the DataSampleManager.  Aborting!", e);
               System.exit(1);
               }

            if (remoteStorageCredentials != null)
               {
               if (!dataSampleManager.setDataSampleUploader(new DataSampleUploader(remoteStorageCredentials)))
                  {
                  logError("Failed to set the DataSampleUploader");
                  }
               }
            dataSampleManager.startup();
            eventListener.handleConnectionEvent(airBotConfig, device.getPortName());
            }
         }
      }

   public boolean isDownloadDisabled()
      {
      return pathToConfigFile != null;
      }

   public boolean areDataStorageCredentialsSet()
      {
      return (dataSampleManager != null && dataSampleManager.isDataSampleUploaderDefined());
      }

   public boolean validateAndSetDataStorageCredentials(@NotNull final RemoteStorageCredentials remoteStorageCredentials)
      {
      if (!areDataStorageCredentialsSet())
         {
         // now test the credentials
         logInfo("Validating host and login credentials...");
         if (RemoteStorageCredentialsValidator.isValid(remoteStorageCredentials))
            {
            logInfo("Host and login credentials validated successfully!!");

            this.remoteStorageCredentials = remoteStorageCredentials;
            if (dataSampleManager != null && !dataSampleManager.setDataSampleUploader(new DataSampleUploader(remoteStorageCredentials)))
               {
               logError("Failed to set the DataSampleUploader");
               return false;
               }
            return true;
            }
         else
            {
            logInfo("Invalid host and/or login credentials.");
            }
         }

      return false;
      }

   @Nullable
   public String getStatistics()
      {
      if (isConnected() && dataSampleManager != null)
         {
         return dataSampleManager.getStatisticsAsString();
         }
      return null;
      }

   public void addStatisticsListener(@Nullable final DataSampleManager.Statistics.Listener listener)
      {
      if (dataSampleManager != null)
         {
         dataSampleManager.addStatisticsListener(listener);
         }
      }

   public void disconnect()
      {
      if (isConnected())
         {
         disconnect(true);
         }
      }

   private void disconnect(final boolean willTryToDisconnectFromDevice)
      {
      // shutdown the data file manager
      if (dataSampleManager != null)
         {
         dataSampleManager.shutdown();
         }

      // disconnect from the device
      if (willTryToDisconnectFromDevice && device != null)
         {
         device.disconnect();
         }

      // set to null
      device = null;
      dataSampleManager = null;
      remoteStorageCredentials = null;
      }

   @Nullable
   private AirBot createFakeAirBot()
      {
      if (pathToConfigFile == null || pathToConfigFile.length() < 1)
         {
         logError("The specified config file path must not be empty.");
         }
      else
         {
         final File configFile = new File(pathToConfigFile);
         if (configFile.isFile())
            {
            final Properties properties = new Properties();
            try
               {
               properties.load(new FileReader(configFile));
               if (LOG.isDebugEnabled())
                  {
                  final StringBuilder s = new StringBuilder("\nProperties found in config file '" + pathToConfigFile + "':\n");
                  for (final Object key : new TreeSet<Object>(properties.keySet()))
                     {
                     final String val = properties.getProperty((String)key);
                     s.append("   [").append(key).append("]=[").append(val).append("]").append(System.getProperty("line.separator", "\n"));
                     }
                  LOG.debug("AirBotUploaderCommandLine.createFakeAirBot(): " + s);
                  }

               return new FakeAirBot(properties);
               }
            catch (Exception e)
               {
               LOG.error("AirBotUploaderCommandLine.createFakeAirBot(): Exception while trying to read the config file [" + pathToConfigFile + "]", e);
               logError("Failed to read the config file '" + pathToConfigFile + "'");
               }
            }
         else
            {
            logError("The specified config file path '" + pathToConfigFile + "' does not denote a valid config file.");
            }
         }
      return null;
      }

   private static final class FakeAirBot implements AirBot
      {
      private final AirBotConfig airBotConfig;

      private FakeAirBot(@NotNull final Properties properties)
         {
         airBotConfig =
               new AirBotConfig()
               {
               /** Returns the AirBot's unique ID. */
               @NotNull
               @Override
               public String getId()
                  {
                  return properties.getProperty("id", "FakeAirBot");
                  }

               /** Returns the AirBot's protocol version. */
               @Override
               public int getProtocolVersion()
                  {
                  return Integer.parseInt(properties.getProperty("version", "1"));
                  }
               };
         }

      @Nullable
      @Override
      public DataSample getSample() throws CommunicationException
         {
         throw new CommunicationException("This fake AirBot doesn't support DataSample retrieval");
         }

      @NotNull
      @Override
      public DataSample getCurrentSample() throws CommunicationException
         {
         throw new CommunicationException("This fake AirBot doesn't support DataSample retrieval");
         }

      @Override
      public boolean deleteSample(@Nullable final AirBot.DataSample dataSample)
         {
         return false;
         }

      @Override
      public boolean deleteSample(final int sampleTime)
         {
         return false;
         }

      @Override
      @NotNull
      public AirBotConfig getAirBotConfig()
         {
         return airBotConfig;
         }

      @Override
      public String getPortName()
         {
         return "FakePort";
         }

      @Override
      public void disconnect()
         {
         // do nothing
         }

      @Override
      public void addCreateLabDevicePingFailureEventListener(final CreateLabDevicePingFailureEventListener listener)
         {
         // do nothing
         }

      @Override
      public void removeCreateLabDevicePingFailureEventListener(final CreateLabDevicePingFailureEventListener listener)
         {
         // do nothing
         }
      }
   }
