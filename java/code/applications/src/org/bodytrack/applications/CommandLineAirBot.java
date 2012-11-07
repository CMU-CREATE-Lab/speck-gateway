package org.bodytrack.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.SortedSet;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.serial.commandline.SerialDeviceCommandLineApplication;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotFactory;
import org.bodytrack.airbot.DataFile;
import org.bodytrack.airbot.DataFileManager;
import org.bodytrack.airbot.NoSuchFileException;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class CommandLineAirBot extends SerialDeviceCommandLineApplication
   {
   public static void main(final String[] args)
      {
      new CommandLineAirBot().run();
      }

   private AirBot device;
   private DataFileManager dataFileManager;

   private final CreateLabDevicePingFailureEventListener pingFailureEventListener =
         new CreateLabDevicePingFailureEventListener()
         {
         public void handlePingFailureEvent()
            {
            println("Device ping failure detected.  You will need to reconnect.");
            device = null;
            dataFileManager = null;
            }
         };

   private CommandLineAirBot()
      {
      super(new BufferedReader(new InputStreamReader(System.in)));

      registerActions();
      }

   private final Runnable enumeratePortsAction =
         new Runnable()
         {
         public void run()
            {
            enumeratePorts();
            }
         };

   private final Runnable scanAndConnectToDeviceAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               println("You are already connected to an AirBot.");
               }
            else
               {
               println("Scanning for an AirBot...");
               device = AirBotFactory.create();

               dataFileManager = createDataFileManagerForDevice(device);
               }
            }
         };

   private final Runnable connectToDeviceAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               println("You are already connected to an AirBot.");
               }
            else
               {
               final SortedMap<Integer, String> portMap = enumeratePorts();

               if (!portMap.isEmpty())
                  {
                  final Integer index = readInteger("Connect to port number: ");

                  if (index == null)
                     {
                     println("Invalid port");
                     }
                  else
                     {
                     final String serialPortName = portMap.get(index);

                     if (serialPortName != null)
                        {
                        device = AirBotFactory.create(serialPortName);

                        dataFileManager = createDataFileManagerForDevice(device);
                        }
                     else
                        {
                        println("Invalid port");
                        }
                     }
                  }
               }
            }
         };

   @Nullable
   private DataFileManager createDataFileManagerForDevice(@Nullable final AirBot device)
      {
      DataFileManager manager = null;

      if (device == null)
         {
         println("Connection failed!");
         }
      else
         {
         device.addCreateLabDevicePingFailureEventListener(pingFailureEventListener);
         final AirBotConfig airBotConfig = device.getAirBotConfig();

         if (airBotConfig == null)
            {
            println("Failed to obtain the AirBot config.  You will need to reconnect.");
            disconnect();
            }
         else
            {
            manager = new DataFileManager(airBotConfig);
            println("Connection successful!");
            }
         }
      return manager;
      }

   private final Runnable disconnectFromDeviceAction =
         new Runnable()
         {
         public void run()
            {
            disconnect();
            }
         };

   private final Runnable downloadFileAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               final String filename = readString("Name of file to download: ");
               if (filename == null || filename.length() <= 0)
                  {
                  println("Invalid file name");
                  return;
                  }

               try
                  {
                  final DataFile dataFile = device.getFile(filename);
                  if (dataFile == null)
                     {
                     println("Failed to transfer the file from the device.");
                     }
                  else if (dataFile.isEmpty())
                     {
                     println("No data available.");
                     }
                  else
                     {
                     println("File transferred successfully! [" + dataFile + "]");

                     try
                        {
                        final File file = dataFileManager.save(dataFile);
                        if (file == null)
                           {
                           println("Failed to save file.");
                           }
                        else
                           {
                           println("File saved successfully [" + file + "]");
                           }
                        }
                     catch (IOException ignored)
                        {
                        println("IOException while trying to download a file.");
                        }
                     }
                  }
               catch (NoSuchFileException ignored)
                  {
                  println("The file '" + filename + "' does not exist on the device.");
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
               }
            }
         };

   private final Runnable eraseFileAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               final String filename = readString("Name of file to erase: ");
               if (filename == null || filename.length() <= 0)
                  {
                  println("Invalid file name");
                  return;
                  }

               final boolean wasSuccessful = device.deleteFile(filename);
               if (wasSuccessful)
                  {
                  println("File '" + filename + "' erased successfully.");
                  }
               else
                  {
                  println("Failed to erase file '" + filename + "'.");
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
               }
            }
         };

   private final Runnable getAvailableFilesAction =
         new Runnable()
         {
         public void run()
            {
            if (isConnected())
               {
               final SortedSet<String> filenames = device.getAvailableFilenames();

               if (filenames == null)
                  {
                  println("Failed to get the list of available files.");
                  }
               else if (filenames.isEmpty())
                  {
                  println("No files available.");
                  }
               else
                  {
                  println("Found " + filenames.size() + " available file" + (filenames.size() == 1 ? "" : "s") + ":");

                  for (final String filename : filenames)
                     {
                     println("      " + filename);
                     }
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
               }
            }
         };

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

      protected GetStringAction(final String label)
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
            println("You must be connected to the AirBot first.");
            }
         }

      @Nullable
      protected abstract String getString();
      }

   private void registerActions()
      {
      registerAction("?", enumeratePortsAction);
      registerAction("C", scanAndConnectToDeviceAction);
      registerAction("c", connectToDeviceAction);
      registerAction("d", disconnectFromDeviceAction);

      registerAction("t",
                     new GetStringAction("AirBot Power On Time")
                     {
                     @Override
                     protected String getString()
                        {
                        final AirBotConfig config = device.getAirBotConfig();
                        return (config == null) ? null : String.valueOf(config.getPowerOnTimeInMillis());
                        }
                     });

      registerAction("i",
                     new GetStringAction("AirBot ID")
                     {
                     @Override
                     protected String getString()
                        {
                        final AirBotConfig config = device.getAirBotConfig();
                        return (config == null) ? null : config.getId();
                        }
                     });
      registerAction("v",
                     new GetStringAction("AirBot Protocol Version")
                     {
                     @Override
                     protected String getString()
                        {
                        final AirBotConfig config = device.getAirBotConfig();
                        return (config == null) ? null : String.valueOf(config.getProtocolVersion());
                        }
                     });

      registerAction("g", getAvailableFilesAction);
      registerAction("f", downloadFileAction);
      registerAction("e", eraseFileAction);

      registerAction(QUIT_COMMAND, quitAction);
      }

   protected final void menu()
      {
      println("COMMANDS -----------------------------------");
      println("");
      println("?         List all available serial ports");
      println("");
      println("C         Scan all serial ports and connect to the first AirBot found");
      println("c         Connect to the AirBot");
      println("d         Disconnect from the AirBot");
      println("");
      println("t         Gets the AirBot's current time");
      println("i         Gets the AirBot's unique ID");
      println("v         Gets the AirBot's protocol version");
      println("");
      println("g         Gets the set of available files from the AirBot");
      println("f         Downloads a file from the AirBot");
      println("e         Erases the specified file from the AirBot");
      println("");
      println("q         Quit");
      println("");
      println("--------------------------------------------");
      }

   protected final boolean isConnected()
      {
      return device != null;
      }

   protected final void disconnect()
      {
      if (isConnected())
         {
         device.disconnect();
         device = null;
         dataFileManager = null;
         }
      }
   }
