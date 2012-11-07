package org.bodytrack.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.serial.commandline.SerialDeviceCommandLineApplication;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotFactory;
import org.bodytrack.airbot.DataFile;
import org.bodytrack.airbot.DataFileDownloader;
import org.bodytrack.airbot.DataFileManager;
import org.bodytrack.airbot.DataFileUploader;
import org.bodytrack.airbot.DataStorageCredentials;
import org.bodytrack.airbot.DataStorageCredentialsImpl;
import org.bodytrack.airbot.DataStorageCredentialsValidator;
import org.bodytrack.airbot.NoSuchFileException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class AirBotUploaderCommandLine extends SerialDeviceCommandLineApplication
   {
   private static final Logger LOG = Logger.getLogger(AirBotUploaderCommandLine.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   @Nullable
   private AirBot device;

   @Nullable
   private DataFileManager dataFileManager;

   @Nullable
   private DataStorageCredentials dataStorageCredentials;

   @Nullable
   private final String pathToConfigFile;

   @NotNull
   private final CreateLabDevicePingFailureEventListener pingFailureEventListener =
         new CreateLabDevicePingFailureEventListener()
         {
         public void handlePingFailureEvent()
            {
            LOG.debug("AirBotUploaderCommandLine.handlePingFailureEvent(): ping failure detected, cleaning up...");

            logError("Device ping failure detected.  Cleaning up...");
            disconnect(false);

            LOG.debug("AirBotUploaderCommandLine.handlePingFailureEvent(): ping failure detected, attempting reconnect...");

            logInfo("Now attempting to reconnect to the device...");
            startup();
            }
         };

   AirBotUploaderCommandLine(@Nullable final String pathToConfigFile)
      {
      super(new BufferedReader(new InputStreamReader(System.in)));
      this.pathToConfigFile = pathToConfigFile;

      registerActions();
      }

   private final Runnable scanAndConnectToDeviceAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               CONSOLE_LOG.info("You are already connected to an AirBot.");
               }
            else
               {
               final boolean isDownloadDisabled = pathToConfigFile != null;
               if (isDownloadDisabled)
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
                  if (isDownloadDisabled)
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
                        println("Failed to set the DataFileUploader");
                        }
                     }
                  dataFileManager.startup();
                  }
               }
            }

         @Nullable
         private AirBot createFakeAirBot()
            {
            if (pathToConfigFile == null || pathToConfigFile.length() < 1)
               {
               CONSOLE_LOG.error("The specified config file path must not be empty.");
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
         };

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

   private final Runnable printStatisticsAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               CONSOLE_LOG.info(dataFileManager.getStatistics());
               }
            else
               {
               CONSOLE_LOG.info("You are not connected to an AirBot.");
               }
            }
         };

   private final Runnable setLoggingLevelAction =
         new Runnable()
         {
         public void run()
            {
            println("Choose the logging level for the log file:");
            println("   1: TRACE");
            println("   2: DEBUG (default)");
            println("   3: INFO");
            final Integer loggingLevelChoice = readInteger("Logging level (1-3): ");
            final Level chosenLevel;
            switch (loggingLevelChoice)
               {
               case (1):
                  chosenLevel = Level.TRACE;
                  break;

               case (2):
                  chosenLevel = Level.DEBUG;
                  break;

               case (3):
                  chosenLevel = Level.INFO;
                  break;

               default:
                  chosenLevel = null;
               }

            if (chosenLevel == null)
               {
               println("Invalid choice.");
               }
            else
               {
               LogManager.getRootLogger().setLevel(chosenLevel);
               logInfo("Logging level now set to '" + chosenLevel + "'.");
               }
            }
         };

   private final Runnable defineDataStorageCredentials =
         new Runnable()
         {
         public void run()
            {
            if (dataFileManager != null && dataFileManager.isDataFileUploaderDefined())
               {
               println("The host and login details can only be defined once per AirBot connection.");
               }
            else
               {
               final String hostNameAndPortStr = readString("Host name (and optional port, colon delimited): ");

               if (isNotNullAndNotEmpty(hostNameAndPortStr))
                  {
                  final String hostName;
                  final String hostPortStr;
                  if (hostNameAndPortStr.contains(":"))
                     {
                     final String[] hostNameAndPort = hostNameAndPortStr.split(":");
                     hostName = hostNameAndPort[0].trim();
                     hostPortStr = hostNameAndPort[1].trim();
                     }
                  else
                     {
                     hostName = hostNameAndPortStr.trim();
                     hostPortStr = "80";
                     }

                  int hostPort = -1;
                  try
                     {
                     hostPort = Integer.parseInt(hostPortStr, 10);
                     }
                  catch (NumberFormatException ignored)
                     {
                     LOG.error("NumberFormatException while trying to convert port [" + hostPortStr + "] to an integer");
                     }

                  if (hostName.length() <= 0 || hostPort <= 0)
                     {
                     println("Invalid host name and/or port.");
                     }
                  else
                     {
                     final String usernameStr = readString("Username: ");
                     if (isNotNullAndNotEmpty(usernameStr))
                        {
                        final String username = usernameStr.trim();

                        final String passwordStr = readString("Password: ");
                        if (isNotNullAndNotEmpty(passwordStr))
                           {
                           final String password = passwordStr.trim();

                           final String deviceNameStr = readString("Device Name: ");
                           if (isNotNullAndNotEmpty(deviceNameStr))
                              {
                              final String deviceName = deviceNameStr.trim();
                              dataStorageCredentials = new DataStorageCredentialsImpl(hostName, hostPort, username, password, deviceName);

                              // now test the credentials
                              print("Validating host and login credentials...");
                              if (DataStorageCredentialsValidator.isValid(dataStorageCredentials))
                                 {
                                 println("Success!");

                                 if (dataFileManager != null)
                                    {
                                    if (!dataFileManager.setDataFileUploader(new DataFileUploader(dataStorageCredentials)))
                                       {
                                       println("Failed to set the DataFileUploader");
                                       }
                                    }
                                 }
                              else
                                 {
                                 println("Invalid.");
                                 }
                              }
                           else
                              {
                              println("Invalid device name.");
                              }
                           }
                        else
                           {
                           println("Invalid password.");
                           }
                        }
                     else
                        {
                        println("Invalid username.");
                        }
                     }
                  }
               else
                  {
                  println("Invalid host name and port.");
                  }
               }
            }
         };

   private boolean isNotNullAndNotEmpty(@Nullable final String str)
      {
      if (str != null)
         {
         final String trimmedStr = str.trim();
         return trimmedStr.length() > 0;
         }
      return false;
      }

   private final Runnable disconnectFromDeviceAction =
         new Runnable()
         {
         public void run()
            {
            disconnect();
            }
         };

   private final Runnable quitAction =
         new Runnable()
         {
         public void run()
            {
            LOG.debug("AirBotUploaderCommandLine.run(): Quit requested by user.");
            disconnect();
            CONSOLE_LOG.info("Bye!");
            }
         };

   private void registerActions()
      {
      registerAction("c", scanAndConnectToDeviceAction);
      registerAction("u", defineDataStorageCredentials);
      registerAction("s", printStatisticsAction);
      registerAction("l", setLoggingLevelAction);
      registerAction("d", disconnectFromDeviceAction);

      registerAction(QUIT_COMMAND, quitAction);
      }

   @Override
   protected void startup()
      {
      println("");
      println(AirBotUploaderHelper.APPLICATION_NAME_AND_VERSION_NUMBER);
      println("");
      }

   protected final void menu()
      {
      println("COMMANDS -----------------------------------");
      println("");
      println("c         Scan all serial ports and connect to the first device found");
      println("u         Specify host and login credentials for uploads");
      println("s         Print statistics for files downloaded, uploaded, and deleted");
      println("l         Set the logging level for the log file (has no effect on console logging)");
      println("d         Disconnect from the device");
      println("");
      println("q         Quit");
      println("");
      println("--------------------------------------------");
      }

   private boolean isConnected()
      {
      return device != null;
      }

   protected final void disconnect()
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
