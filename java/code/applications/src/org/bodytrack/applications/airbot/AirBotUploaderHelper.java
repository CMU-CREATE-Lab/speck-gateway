package org.bodytrack.applications.airbot;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotFactory;
import org.bodytrack.airbot.DataFile;
import org.bodytrack.airbot.DataFileDownloader;
import org.bodytrack.airbot.DataFileManager;
import org.bodytrack.airbot.DataFileUploader;
import org.bodytrack.airbot.DataStorageCredentials;
import org.bodytrack.airbot.DataStorageCredentialsValidator;
import org.bodytrack.airbot.NoSuchFileException;
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
   private DataFileManager dataFileManager;

   @Nullable
   private DataStorageCredentials dataStorageCredentials;

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

            final DataFileDownloader dataFileDownloader;
            if (isDownloadDisabled())
               {
               logInfo("Data files will not be downloaded from an AirBot since you specified a config file for AirBot [" + airBotConfig.getId() + "]");
               dataFileDownloader = null;
               }
            else
               {
               logInfo("Connection successful to AirBot [" + airBotConfig.getId() + "] on serial port [" + device.getPortName() + "].");
               dataFileDownloader = new DataFileDownloader(device);
               }

            logInfo("Starting up the DataFileManager...");
            dataFileManager = new DataFileManager(airBotConfig, dataFileDownloader);
            if (dataStorageCredentials != null)
               {
               if (!dataFileManager.setDataFileUploader(new DataFileUploader(dataStorageCredentials)))
                  {
                  logError("Failed to set the DataFileUploader");
                  }
               }
            dataFileManager.startup();
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
      return (dataFileManager != null && dataFileManager.isDataFileUploaderDefined());
      }

   public boolean validateAndSetDataStorageCredentials(@NotNull final DataStorageCredentials dataStorageCredentials)
      {
      if (!areDataStorageCredentialsSet())
         {
         // now test the credentials
         logInfo("Validating host and login credentials...");
         if (DataStorageCredentialsValidator.isValid(dataStorageCredentials))
            {
            logInfo("Host and login credentials validated successfully!!");

            this.dataStorageCredentials = dataStorageCredentials;
            if (dataFileManager != null && !dataFileManager.setDataFileUploader(new DataFileUploader(dataStorageCredentials)))
               {
               logError("Failed to set the DataFileUploader");
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
      if (isConnected() && dataFileManager != null)
         {
         return dataFileManager.getStatisticsAsString();
         }
      return null;
      }

   public void addStatisticsListener(@Nullable final DataFileManager.Statistics.Listener listener)
      {
      if (dataFileManager != null)
         {
         dataFileManager.addStatisticsListener(listener);
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
      if (dataFileManager != null)
         {
         dataFileManager.shutdown();
         }

      // disconnect from the device
      if (willTryToDisconnectFromDevice && device != null)
         {
         device.disconnect();
         }

      // set to null
      device = null;
      dataFileManager = null;
      dataStorageCredentials = null;
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

   private class FakeAirBot implements AirBot
      {
      private final SortedSet<String> emptySetOfAvailableFilenames = Collections.unmodifiableSortedSet(new TreeSet<String>());
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

               /** Returns the time at which the AirBot was powered on (in millis since the epoch). */
               @Override
               public long getPowerOnTimeInMillis()
                  {
                  return Long.parseLong(properties.getProperty("time", String.valueOf(System.currentTimeMillis())));
                  }
               };
         }

      @Override
      @SuppressWarnings("ReturnOfCollectionOrArrayField")
      public SortedSet<String> getAvailableFilenames()
         {
         return emptySetOfAvailableFilenames;
         }

      @Override
      public DataFile getFile(@Nullable final String filename) throws NoSuchFileException
         {
         throw new NoSuchFileException("This fake AirBot doesn't support file retrieval");
         }

      @Override
      public boolean deleteFile(@Nullable final String filename)
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
