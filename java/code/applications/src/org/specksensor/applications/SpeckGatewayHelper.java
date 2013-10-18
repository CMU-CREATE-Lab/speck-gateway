package org.specksensor.applications;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.TreeSet;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.ApiSupport;
import org.specksensor.CommunicationException;
import org.specksensor.DataSampleDownloader;
import org.specksensor.DataSampleManager;
import org.specksensor.DataSampleUploader;
import org.specksensor.InitializationException;
import org.specksensor.RemoteStorageCredentials;
import org.specksensor.RemoteStorageCredentialsValidator;
import org.specksensor.Speck;
import org.specksensor.SpeckConfig;
import org.specksensor.SpeckConstants;
import org.specksensor.SpeckFactory;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class SpeckGatewayHelper
   {
   private static final Logger LOG = Logger.getLogger(SpeckGatewayHelper.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   interface EventListener
      {
      void handleConnectionEvent(@NotNull final SpeckConfig speckConfig, @NotNull final String portName);

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

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(SpeckGatewayHelper.class.getName());

   public static final String APPLICATION_NAME = RESOURCES.getString("application.name");
   public static final String VERSION_NUMBER = RESOURCES.getString("version.number");
   public static final String APPLICATION_NAME_AND_VERSION_NUMBER = APPLICATION_NAME + " v" + VERSION_NUMBER;

   @Nullable
   private Speck device;

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
            LOG.debug("SpeckGatewayHelper.handlePingFailureEvent(): ping failure detected, cleaning up...");

            logError("Connection failure detected.  Cleaning up...");
            disconnect(false);

            LOG.debug("SpeckGatewayHelper.handlePingFailureEvent(): ping failure detected, attempting reconnect...");

            // notify listener of the ping failure
            eventListener.handlePingFailureEvent();
            }
         };

   SpeckGatewayHelper(@NotNull final EventListener eventListener)
      {
      this(eventListener, null);
      }

   SpeckGatewayHelper(@NotNull final EventListener eventListener, @Nullable final String pathToConfigFile)
      {
      this.eventListener = eventListener;
      this.pathToConfigFile = pathToConfigFile;
      }

   public boolean isConnected()
      {
      return device != null;
      }

   public Speck scanAndConnect()
      {
      if (isConnected())
         {
         logInfo("You are already connected to a Speck.");
         }
      else
         {
         if (isDownloadDisabled())
            {
            logInfo("Loading config file...");
            device = createPropertyFileSpeck();
            }
         else
            {
            logInfo("Scanning for a Speck...");
            device = SpeckFactory.create();
            }

         if (device == null)
            {
            logError("Connection failed.");
            }
         else
            {
            device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
            final SpeckConfig speckConfig = device.getSpeckConfig();

            final DataSampleDownloader dataSampleDownloader;
            if (isDownloadDisabled())
               {
               logInfo("Data files will not be downloaded from a Speck since you specified a config file for Speck [" + speckConfig.getId() + "]");
               dataSampleDownloader = null;
               }
            else
               {
               logInfo("Connection successful to Speck [" + speckConfig.getId() + "] on serial port [" + device.getPortName() + "].");
               dataSampleDownloader = new DataSampleDownloader(device);
               }

            logInfo("Starting up the DataSampleManager...");
            try
               {
               dataSampleManager = new DataSampleManager(speckConfig, dataSampleDownloader);
               }
            catch (InitializationException e)
               {
               LOG.error("SpeckGatewayHelper.scanAndConnect(): InitializationException while trying to create the DataSampleManager.  Aborting!", e);
               System.exit(1);
               }

            if (remoteStorageCredentials != null)
               {
               if (!dataSampleManager.setDataSampleUploader(new DataSampleUploader(speckConfig, remoteStorageCredentials)))
                  {
                  logError("Failed to set the DataSampleUploader");
                  }
               }
            dataSampleManager.startup();
            eventListener.handleConnectionEvent(speckConfig, device.getPortName());
            }
         }

      return device;
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
      if (device != null)
         {
         if (!areDataStorageCredentialsSet())
            {
            // now test the credentials
            logInfo("Validating host and login credentials...");
            if (RemoteStorageCredentialsValidator.isValid(remoteStorageCredentials))
               {
               logInfo("Host and login credentials validated successfully!!");

               this.remoteStorageCredentials = remoteStorageCredentials;
               if (dataSampleManager != null && !dataSampleManager.setDataSampleUploader(new DataSampleUploader(device.getSpeckConfig(), remoteStorageCredentials)))
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
         }
      else
         {
         logInfo("You must be connected to a Speck before setting the data storage login credentials.");
         }

      return false;
      }

   @Nullable
   public Speck getSpeck()
      {
      return device;
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
   private Speck createPropertyFileSpeck()
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
                  LOG.debug("SpeckGatewayCommandLine.createFakeSpeck(): " + s);
                  }

               final String speckId = properties.getProperty("id");
               if (speckId == null)
                  {
                  final String msg = "The 'id' property must be defined in the config file!'";
                  logError(msg);
                  throw new IllegalArgumentException(msg);
                  }
               final String protocolVersionStr = properties.getProperty("protocol-version");
               if (protocolVersionStr == null)
                  {
                  final String msg = "The 'protocol-version' property must be defined in the config file!'";
                  logError(msg);
                  throw new IllegalArgumentException(msg);
                  }

               final int protocolVersion;
               try
                  {
                  protocolVersion = Integer.parseInt(protocolVersionStr);
                  }
               catch (NumberFormatException e)
                  {
                  final String msg = "Invalid 'protocol-version' property: NumberFormatException while trying to convert [" + protocolVersionStr + "] into an integer";
                  logError(msg);
                  throw new IllegalArgumentException(msg);
                  }

               return new PropertyFileSpeck(speckId, protocolVersion, configFile);
               }
            catch (Exception e)
               {
               LOG.error("SpeckGatewayCommandLine.createFakeSpeck(): Exception while trying to read the config file [" + pathToConfigFile + "]", e);
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

   private static final class PropertyFileSpeck implements Speck
      {
      private final SpeckConfig speckConfig;
      private final File configFile;

      private PropertyFileSpeck(@NotNull final String speckId, final int protocolVersion, @NotNull final File configFile)
         {
         this.configFile = configFile;
         final ApiSupport apiSupport = ApiSupport.getInstance(protocolVersion);
         speckConfig =
               new SpeckConfig()
               {
               /** Returns the Speck's unique ID. */
               @NotNull
               @Override
               public String getId()
                  {
                  return speckId;
                  }

               /** Returns the Speck's protocol version. */
               @Override
               public int getProtocolVersion()
                  {
                  return apiSupport.getProtocolVersion();
                  }

               @Override
               public int getLoggingInterval()
                  {
                  return SpeckConstants.LoggingInterval.DEFAULT;
                  }

               @NotNull
               @Override
               public ApiSupport getApiSupport()
                  {
                  return apiSupport;
                  }
               };
         }

      @Nullable
      @Override
      public DataSample getSample() throws CommunicationException
         {
         throw new CommunicationException("The PropertyFileSpeck doesn't support DataSample retrieval");
         }

      @NotNull
      @Override
      public DataSample getCurrentSample() throws CommunicationException
         {
         throw new CommunicationException("The PropertyFileSpeck doesn't support DataSample retrieval");
         }

      @Override
      public boolean deleteSample(@Nullable final Speck.DataSample dataSample)
         {
         return false;
         }

      @Override
      public boolean deleteSample(final int sampleTime)
         {
         return false;
         }

      @Override
      public int getNumberOfAvailableSamples() throws UnsupportedOperationException
         {
         throw new UnsupportedOperationException("The PropertyFileSpeck doesn't support reading the number of available samples");
         }

      @Override
      @NotNull
      public SpeckConfig setLoggingInterval(final int loggingIntervalInSeconds) throws UnsupportedOperationException
         {
         throw new UnsupportedOperationException("The PropertyFileSpeck doesn't support setting of the logging interval");
         }

      @Override
      public void enterBootloaderMode() throws UnsupportedOperationException
         {
         throw new UnsupportedOperationException("The PropertyFileSpeck doesn't support entering bootloader mode");
         }

      @Override
      @NotNull
      public SpeckConfig getSpeckConfig()
         {
         return speckConfig;
         }

      @Override
      public String getPortName()
         {
         return configFile.getAbsolutePath();
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
