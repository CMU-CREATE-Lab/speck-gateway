package org.bodytrack.applications.airbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.util.commandline.BaseCommandLineApplication;
import org.bodytrack.airbot.AirBot;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotFactory;
import org.bodytrack.airbot.CommunicationException;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class CommandLineAirBot extends BaseCommandLineApplication
   {
   public static void main(final String[] args)
      {
      new CommandLineAirBot().run();
      }

   private AirBot device;

   private final CreateLabDevicePingFailureEventListener pingFailureEventListener =
         new CreateLabDevicePingFailureEventListener()
         {
         public void handlePingFailureEvent()
            {
            println("Device ping failure detected.  You will need to reconnect.");
            device = null;
            }
         };

   private CommandLineAirBot()
      {
      super(new BufferedReader(new InputStreamReader(System.in)));

      registerActions();
      }

   private final Runnable connectToHIDDeviceAction =
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
               device = AirBotFactory.create();

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
                  println("Failed to read current sample");
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
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
                  println("Failed to read data sample");
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
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
               catch (NumberFormatException e)
                  {
                  println("Invalid timestamp");
                  }
               catch (CommunicationException e)
                  {
                  println("Failed to delete data sample");
                  }
               }
            else
               {
               println("You must be connected to the AirBot first.");
               }
            }
         };

   private void printSample(@Nullable final AirBot.Sample sample)
      {
      if (sample == null)
         {
         println("Null sample");
         }
      else if (sample.isNoDataAvailable())
         {
         println("No data available.");
         }
      else
         {
         println("Sample Time:    " + sample.getSampleTime());
         println("Particle Count: " + sample.getParticleCount());
         println("Temperature:    " + sample.getTemperature());
         println("Humidity:       " + sample.getHumidity());
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
      registerAction("c", connectToHIDDeviceAction);
      registerAction("d", disconnectFromDeviceAction);
      registerAction("s", getCurrentStateAction);
      registerAction("g", getDataSampleAction);
      registerAction("x", deleteDataSampleAction);

      registerAction("i",
                     new GetStringAction("AirBot ID")
                     {
                     @Override
                     protected String getString()
                        {
                        final AirBotConfig config = device.getAirBotConfig();
                        return config.getId();
                        }
                     });
      registerAction("v",
                     new GetStringAction("AirBot Protocol Version")
                     {
                     @Override
                     protected String getString()
                        {
                        final AirBotConfig config = device.getAirBotConfig();
                        return String.valueOf(config.getProtocolVersion());
                        }
                     });

      registerAction(QUIT_COMMAND, quitAction);
      }

   protected final void menu()
      {
      println("COMMANDS -----------------------------------");
      println("");
      println("c         Connect to the AirBot");
      println("d         Disconnect from the AirBot");
      println("");
      println("s         Gets the current state");
      println("g         Gets a data sample");
      println("x         Delete a data sample");
      println("");
      println("i         Gets the AirBot's unique ID");
      println("v         Gets the AirBot's protocol version");
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
         }
      }
   }
