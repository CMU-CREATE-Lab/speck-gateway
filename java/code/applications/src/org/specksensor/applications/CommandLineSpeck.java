package org.specksensor.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.util.commandline.BaseCommandLineApplication;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.specksensor.ApiSupport;
import org.specksensor.CommunicationException;
import org.specksensor.Speck;
import org.specksensor.SpeckConfig;
import org.specksensor.SpeckConstants;
import org.specksensor.SpeckFactory;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class CommandLineSpeck extends BaseCommandLineApplication
   {
   private static final Logger LOG = Logger.getLogger(CommandLineSpeck.class);

   private static final String LINE_SEPARATOR = System.getProperty("line.separator");

   private static final String BOOTLOADER_SWITCH = "--bootloader";
   private static final String SET_INTERVAL_SWITCH = "--set-interval=";

   public static void main(final String[] args)
      {
      if (args.length > 0 && BOOTLOADER_SWITCH.equals(args[0]))
         {
         new CommandLineSpeck().runBootloaderModeHelper();
         }
      else if (args.length > 0 && args[0].startsWith(SET_INTERVAL_SWITCH))
         {
         final String[] argParts = args[0].split("=");
         if (argParts.length >= 2)
            {
            try
               {
               new CommandLineSpeck().runSetIntervalHelper(Integer.parseInt(argParts[1]));
               }
            catch (NumberFormatException ignored)
               {
               println("ERROR: Invalid logging interval [" + argParts[1] + "].  Aborting.");
               System.exit(0);
               }
            }
         else
            {
            println("ERROR: No logging interval specified.  Aborting.");
            System.exit(0);
            }
         }
      else
         {
         new CommandLineSpeck().run();
         }
      }

   private Speck device;

   private final CreateLabDevicePingFailureEventListener pingFailureEventListener =
         new CreateLabDevicePingFailureEventListener()
         {
         public void handlePingFailureEvent()
            {
            println("Device ping failure detected.  You will need to reconnect.");
            device = null;
            }
         };

   private CommandLineSpeck()
      {
      super(new BufferedReader(new InputStreamReader(System.in)));

      registerActions();
      }

   @SuppressWarnings("BusyWait")
   private void runBootloaderModeHelper()
      {
      println(" -----------------------------------------------------------------");
      println("|                                                                  |");
      println("|                     SPECK BOOTLOADER HELPER                      |");
      println("|                                                                  |");
      println("| This app helps put Specks into bootloader mode.  It searches for |");
      println("| a Speck and, once detected, will put it into bootloader mode,    |");
      println("| disconnect, and then will search for the next Speck.             |");
      println("|                                                                  |");
      println("| Type ENTER at any time to quit.                                  |");
      println("|                                                                  |");
      println(" -----------------------------------------------------------------");

      registerAction(QUIT_COMMAND, quitAction);

      try
         {
         final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
         while (true)
            {
            // check whether the user typed ENTER
            if (in.ready())
               {
               break;
               }

            // Try to connect to a Speck
            device = SpeckFactory.create();

            if (device != null)
               {
               device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
               println("Connected to Speck " + device.getSpeckConfig().getId());
               if (device.getSpeckConfig().getApiSupport().canEnterBootloaderMode())
                  {
                  println("Putting the Speck in bootloader mode, and then disconnecting...");
                  device.enterBootloaderMode();
                  device = null;
                  println("The Speck is now disconnected and in bootloader mode.  You may safely unplug the Speck now.\n");
                  }
               else
                  {
                  println("Sorry, this Speck does not support the command to enter bootloader mode.  Disconnecting...");
                  disconnect();
                  println("The Speck is now disconnected.  You may safely unplug the Speck now.\n");
                  }
               }
            Thread.sleep(1000);
            }
         }
      catch (IOException ex)
         {
         ex.printStackTrace();
         }
      catch (InterruptedException e)
         {
         LOG.error("InterruptedException while sleeping.  Aborting.", e);
         }
      }

   @SuppressWarnings("BusyWait")
   private void runSetIntervalHelper(final int newLoggingInterval)
      {
      println(" -----------------------------------------------------------------");
      println("|                                                                  |");
      println("|                  SPECK LOGGING INTERVAL HELPER                   |");
      println("|                                                                  |");
      println("| This app helps set the logging interval for multiple Specks. It  |");
      println("| searches for a Speck and, once detected, will set its logging    |");
      println("| interval, disconnect, and then will search for the next Speck.   |");
      println("|                                                                  |");
      println("| Type ENTER at any time to quit.                                  |");
      println("|                                                                  |");
      println(" -----------------------------------------------------------------");

      registerAction(QUIT_COMMAND, quitAction);

      try
         {
         final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
         while (true)
            {
            // check whether the user typed ENTER
            if (in.ready())
               {
               break;
               }

            // Try to connect to a Speck
            device = SpeckFactory.create();

            if (device != null)
               {
               device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
               println("Connected to Speck " + device.getSpeckConfig().getId());
               if (device.getSpeckConfig().getApiSupport().canMutateLoggingInterval())
                  {
                  final int currentLoggingInterval = device.getSpeckConfig().getLoggingInterval();
                  if (currentLoggingInterval == newLoggingInterval)
                     {
                     println("This Speck's logging interval is already set to " + currentLoggingInterval + ", so no change is necessary.  Disconnecting...");
                     disconnect();
                     println("The Speck is now disconnected.  You may safely unplug the Speck now.\n");
                     }
                  else
                     {
                     println("Changing this Speck's logging interval from " + currentLoggingInterval + " to " + newLoggingInterval + "...");
                     try
                        {
                        final SpeckConfig newSpeckConfig = device.setLoggingInterval(newLoggingInterval);
                        println("The Speck's logging interval is now set to " + newSpeckConfig.getLoggingInterval() + ". Disconnecting...");
                        disconnect();
                        println("The Speck is now disconnected.  You may safely unplug the Speck now.\n");
                        }
                     catch (CommunicationException e)
                        {
                        LOG.error("CommunicationException while trying to set the logging interval.", e);
                        println("Failed to set the Speck's logging interval due to a CommunicationException.  You may safely unplug the Speck now.\n");
                        }
                     }
                  }
               else
                  {
                  println("Sorry, this Speck does not support the command to set the logging interval.  Disconnecting...");
                  disconnect();
                  println("The Speck is now disconnected.  You may safely unplug the Speck now.\n");
                  }
               }
            Thread.sleep(1000);
            }
         }
      catch (IOException ex)
         {
         ex.printStackTrace();
         }
      catch (InterruptedException e)
         {
         LOG.error("InterruptedException while sleeping.  Aborting.", e);
         }
      }

   private final Runnable connectToHIDDeviceAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               println("You are already connected to a Speck.");
               }
            else
               {
               device = SpeckFactory.create();

               if (device == null)
                  {
                  println("Connection failed!");
                  }
               else
                  {
                  device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
                  println("Connection successful!");
                  }
               }
            }
         };

   private final Runnable disconnectFromDeviceAction =
         new Runnable()
         {
         public void run()
            {
            disconnect();
            }
         };

   private final Runnable getCurrentStateAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               try
                  {
                  printSample(device.getCurrentSample());
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to read current sample: " + e);
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable getDataSampleAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               try
                  {
                  printSample(device.getSample());
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to read data sample: " + e);
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable getDataSamplesAction =
         new Runnable()
         {
         @SuppressWarnings("BusyWait")
         public void run()
            {
            if (isConnected())
               {
               final Integer millisToWait = readInteger("Milliseconds to wait between data sample fetches: ");
               if (millisToWait == null || millisToWait < 0)
                  {
                  println("Invalid duration");
                  return;
                  }

               println("Press ENTER to stop reading and deleting samples...");

               final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

               try
                  {
                  final boolean hasConcentration = device.getSpeckConfig().getApiSupport().hasParticleConcentration();

                  final Set<Integer> sampleTimes = new HashSet<Integer>();
                  boolean shouldQuit = false;
                  while (!shouldQuit)
                     {
                     // check whether the user pressed a key
                     if (in.ready())
                        {
                        shouldQuit = true;
                        }

                     final Speck.DataSample sample = device.getSample();
                     final boolean isNoDataAvailable = (sample == null || sample.isEmpty());
                     if (isNoDataAvailable)
                        {
                        println("No data available, waiting 30 seconds before trying again...");
                        }
                     else
                        {
                        final int sampleTime = sample.getSampleTime();
                        println("Sample: [" +
                                sampleTime + ", " +
                                sample.getRawParticleCount() + ", " +
                                (hasConcentration ? String.valueOf(sample.getParticleConcentration()) : String.valueOf(sample.getParticleCountOrConcentration())) + ", " +
                                sample.getTemperatureInTenthsOfADegreeF() + ", " +
                                sample.getHumidity() + "]");

                        // keep track of which ones we've seen to detect duplicates
                        if (sampleTimes.contains(sampleTime))
                           {
                           println("DUPLICATE SAMPLE!!!");
                           }
                        else
                           {
                           sampleTimes.add(sampleTime);
                           }

                        device.deleteSample(sampleTime);
                        }

                     final long sleepUntil = System.currentTimeMillis() + (isNoDataAvailable ? 30000 : millisToWait);

                     final int sleepDuration = Math.min(10, millisToWait);
                     while (System.currentTimeMillis() < sleepUntil && !shouldQuit)
                        {
                        try
                           {
                           Thread.sleep(sleepDuration);
                           }
                        catch (InterruptedException e)
                           {
                           println("InterruptedException while sleeping: " + e);
                           }

                        // check whether the user pressed a key
                        if (in.ready())
                           {
                           shouldQuit = true;
                           }
                        }
                     }
                  }
               catch (IOException e)
                  {
                  println("IOException while trying to read keyboard input: " + e);
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to read data sample. Aborting: " + e);
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable deleteDataSampleAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               final String timestampStr = readString("Timestamp of sample to delete: ");
               if (timestampStr == null || timestampStr.length() <= 0)
                  {
                  println("Invalid timestamp");
                  return;
                  }

               try
                  {
                  if (device.deleteSample(Integer.parseInt(timestampStr)))
                     {
                     println("Sample deleted successfully");
                     }
                  else
                     {
                     println("Failed to delete sample");
                     }
                  }
               catch (NumberFormatException ignored)
                  {
                  println("Invalid timestamp");
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to delete data sample: " + e);
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable wipeStorageAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               println("Wiping all samples from Speck storage...");
               print("Deleting");
               int numSamplesRead = 0;
               int numSamplesDeleted = 0;
               int missesDetected = 0;

               Speck.DataSample previousSample = null;
               Speck.DataSample firstNonEmptySample = null;
               Speck.DataSample lastNonEmptySample = null;
               Speck.DataSample sample = null;
               do
                  {
                  try
                     {
                     sample = device.getSample();
                     if (sample != null && !sample.isEmpty())
                        {
                        if (previousSample != null)
                           {
                           if (sample.getSampleTime() - previousSample.getSampleTime() > 1)
                              {
                              missesDetected++;
                              }
                           }

                        if (firstNonEmptySample == null)
                           {
                           firstNonEmptySample = sample;
                           }

                        numSamplesRead++;
                        lastNonEmptySample = sample;

                        if (device.deleteSample(sample))
                           {
                           numSamplesDeleted++;
                           }
                        if (numSamplesDeleted % 10 == 0)
                           {
                           print(".");
                           }

                        previousSample = sample;
                        }
                     }
                  catch (CommunicationException e)
                     {
                     println("CommunicationException while trying to get a sample: " + e);
                     }
                  }
               while (sample != null && !sample.isEmpty());
               println("");
               println("Read " + numSamplesRead + " samples, deleted " + numSamplesDeleted);
               if (firstNonEmptySample != null)
                  {
                  println("First non-empty sample read: " + firstNonEmptySample.getSampleTime());
                  }
               if (lastNonEmptySample != null)
                  {
                  println("Last non-empty sample read:  " + lastNonEmptySample.getSampleTime());
                  }
               println("Misses detected: " + missesDetected);
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable wipeStorageAction2 =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               println("Wiping all samples from Speck storage...");
               print("Deleting");
               int numSamplesRead = 0;
               int missesDetected = 0;

               Speck.DataSample previousSample = null;
               Speck.DataSample firstNonEmptySample = null;
               Speck.DataSample lastNonEmptySample = null;
               Speck.DataSample sample = null;
               do
                  {
                  try
                     {
                     sample = device.getSample();
                     if (sample != null && !sample.isEmpty())
                        {
                        if (previousSample != null)
                           {
                           if (sample.getSampleTime() - previousSample.getSampleTime() > 1)
                              {
                              missesDetected++;
                              }
                           }

                        if (firstNonEmptySample == null)
                           {
                           firstNonEmptySample = sample;
                           }

                        if (!sample.equals(previousSample))
                           {
                           numSamplesRead++;

                           if (numSamplesRead % 10 == 0)
                              {
                              print(".");
                              }
                           }

                        lastNonEmptySample = sample;
                        previousSample = sample;
                        }
                     }
                  catch (CommunicationException e)
                     {
                     println("CommunicationException while trying to get a sample: " + e);
                     }
                  }
               while (sample != null && !sample.isEmpty());
               println("");
               println("Read " + numSamplesRead + " samples");
               if (firstNonEmptySample != null)
                  {
                  println("First non-empty sample read: " + firstNonEmptySample.getSampleTime());
                  }
               if (lastNonEmptySample != null)
                  {
                  println("Last non-empty sample read:  " + lastNonEmptySample.getSampleTime());
                  }
               println("Misses detected: " + missesDetected);
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable getNumberOfAvailableSamplesAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               try
                  {
                  println("Number of Available Samples: " + device.getNumberOfAvailableSamples());
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to read the number of available data samples: " + e);
                  }
               catch (UnsupportedOperationException e)
                  {
                  println("This Speck does not support reading the number of available samples.");
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable setLoggingIntervalAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               if (device.getSpeckConfig().getApiSupport().canMutateLoggingInterval())
                  {
                  final String loggingIntervalStr = readString("Logging interval when disconnected [" + SpeckConstants.LoggingInterval.MIN + "," + SpeckConstants.LoggingInterval.MAX + "] (secs): ");
                  if (loggingIntervalStr == null || loggingIntervalStr.length() <= 0)
                     {
                     println("Invalid logging interval");
                     return;
                     }

                  try
                     {
                     final int loggingIntervalInSeconds = Integer.parseInt(loggingIntervalStr);
                     try
                        {
                        final SpeckConfig newConfig = device.setLoggingInterval(loggingIntervalInSeconds);
                        println("The logging interval is now set to " + newConfig.getLoggingInterval() + " second(s)");
                        }
                     catch (CommunicationException e)
                        {
                        println("Failed to set the logging interval: " + e);
                        }
                     }
                  catch (NumberFormatException ignored)
                     {
                     println("Invalid logging interval");
                     }
                  }
               else
                  {
                  println("Sorry, this Speck does not support changing of the logging interval");
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private final Runnable enterBootloaderModeAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               if (device.getSpeckConfig().getApiSupport().canEnterBootloaderMode())
                  {
                  println("Entering bootloader mode, and then disconnecting...");
                  device.enterBootloaderMode();
                  device = null;
                  println("The Speck is now disconnected and in bootloader mode.");
                  }
               else
                  {
                  println("Sorry, this Speck does not support the command to enter bootloader mode.");
                  }
               }
            else
               {
               println("You must be connected to the Speck first.");
               }
            }
         };

   private void printSample(@Nullable final Speck.DataSample dataSample)
      {
      if (dataSample == null || dataSample.isEmpty())
         {
         println("No data available.");
         }
      else
         {
         println("Sample Time:                     " + dataSample.getSampleTime());
         println("Raw Particle Count:              " + dataSample.getRawParticleCount());
         if (device.getSpeckConfig().getApiSupport().hasParticleCount())
            {
            println("Particle Count (particles/L):    " + dataSample.getParticleCountOrConcentration());
            }
         if (device.getSpeckConfig().getApiSupport().hasParticleConcentration())
            {
            println("Particle Concentration (ug/m^3): " + (dataSample.getParticleConcentration()));
            }
         println("Temperature:                     " + dataSample.getTemperatureInTenthsOfADegreeF());
         println("Humidity:                        " + dataSample.getHumidity());
         }
      }

   private final Runnable quitAction =
         new Runnable()
         {
         public void run()
            {
            disconnect();
            println("Bye!");
            }
         };

   private abstract class GetStringAction implements Runnable
      {
      private final String label;

      private GetStringAction(final String label)
         {
         this.label = label;
         }

      @Override
      public void run()
         {
         if (isConnected())
            {
            println(label + ": " + getString());
            }
         else
            {
            println("You must be connected to the Speck first.");
            }
         }

      @Nullable
      protected abstract String getString();
      }

   private void registerActions()
      {
      registerAction("c", connectToHIDDeviceAction);
      registerAction("d", disconnectFromDeviceAction);
      registerAction("s", getCurrentStateAction);
      registerAction("g", getDataSampleAction);
      registerAction("G", getDataSamplesAction);
      registerAction("x", deleteDataSampleAction);
      registerAction("w", wipeStorageAction);
      registerAction("w2", wipeStorageAction2);
      registerAction("n", getNumberOfAvailableSamplesAction);
      registerAction("i",
                     new GetStringAction("Speck ID")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        return config.getId();
                        }
                     });
      registerAction("p",
                     new GetStringAction("Speck Protocol Version")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        return String.valueOf(config.getProtocolVersion());
                        }
                     });
      registerAction("h",
                     new GetStringAction("Speck Hardware Version")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        return config.getApiSupport().hasDeviceVersionInfo() ? String.valueOf(config.getHardwareVersion()) : "unknown";
                        }
                     });
      registerAction("f",
                     new GetStringAction("Speck Firmware Version")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        return config.getApiSupport().hasDeviceVersionInfo() ? String.valueOf(config.getFirmwareVersion()) : "unknown";
                        }
                     });
      registerAction("a",
                     new GetStringAction("Speck API Support")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        final ApiSupport apiSupport = config.getApiSupport();
                        final StringBuilder s = new StringBuilder(LINE_SEPARATOR);
                        s.append("   Can mutate logging interval:  ").append(apiSupport.canMutateLoggingInterval()).append(LINE_SEPARATOR);
                        s.append("   Can report data sample count: ").append(apiSupport.canGetNumberOfDataSamples()).append(LINE_SEPARATOR);
                        s.append("   Has temperature sensor:       ").append(apiSupport.hasTemperatureSensor()).append(LINE_SEPARATOR);
                        s.append("   Has particle count:           ").append(apiSupport.hasParticleCount()).append(LINE_SEPARATOR);
                        s.append("   Has particle concentration:   ").append(apiSupport.hasParticleConcentration()).append(LINE_SEPARATOR);
                        s.append("   Has device version info:      ").append(apiSupport.hasDeviceVersionInfo()).append(LINE_SEPARATOR);
                        s.append("   Can enter bootloader mode:    ").append(apiSupport.canEnterBootloaderMode());
                        return s.toString();
                        }
                     });
      registerAction("l",
                     new GetStringAction("Speck Logging Interval")
                     {
                     @Override
                     protected String getString()
                        {
                        final SpeckConfig config = device.getSpeckConfig();
                        return String.valueOf(config.getLoggingInterval());
                        }
                     });
      registerAction("L", setLoggingIntervalAction);
      registerAction("B", enterBootloaderModeAction);

      registerAction(QUIT_COMMAND, quitAction);
      }

   protected final void menu()
      {
      println("COMMANDS -----------------------------------");
      println("");
      println("c         Connect to the Speck");
      println("d         Disconnect from the Speck");
      println("");
      println("s         Gets the current state");
      println("g         Gets a data sample");
      println("G         Repeatedly gets (and deletes) a data sample every N milliseconds");
      println("x         Delete a data sample");
      println("w         Wipe Speck's storage by getting and deleting all saved samples");
      println("w2        Wipe Speck's storage by getting (but not deleting) all saved samples");
      println("n         Gets the number of available samples");
      println("");
      println("i         Gets the Speck's unique ID");
      println("p         Gets the Speck's protocol version");
      println("h         Gets the Speck's hardware version");
      println("f         Gets the Speck's firmware version");
      println("a         Gets the Speck's API support");
      println("l         Gets the Speck's logging interval when disconnected (secs)");
      println("L         Sets the Speck's logging interval when disconnected (secs)");
      println("");
      println("B         Puts the Speck into bootloader mode, and disconnects.");
      println("");
      println("q         Quit");
      println("");
      println("--------------------------------------------");
      }

   private boolean isConnected()
      {
      return device != null;
      }

   private void disconnect()
      {
      if (isConnected())
         {
         device.disconnect();
         device = null;
         }
      }
   }
