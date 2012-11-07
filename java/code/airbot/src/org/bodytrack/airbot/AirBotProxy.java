package org.bodytrack.airbot;

import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.serial.CreateLabSerialDeviceCommandStrategy;
import edu.cmu.ri.createlab.serial.SerialDeviceCommandExecutionQueue;
import edu.cmu.ri.createlab.serial.SerialDeviceReturnValueCommandExecutor;
import edu.cmu.ri.createlab.serial.SerialDeviceReturnValueCommandStrategy;
import edu.cmu.ri.createlab.serial.config.BaudRate;
import edu.cmu.ri.createlab.serial.config.CharacterSize;
import edu.cmu.ri.createlab.serial.config.FlowControl;
import edu.cmu.ri.createlab.serial.config.Parity;
import edu.cmu.ri.createlab.serial.config.SerialIOConfiguration;
import edu.cmu.ri.createlab.serial.config.StopBits;
import edu.cmu.ri.createlab.util.commandexecution.CommandExecutionFailureHandler;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.commands.DeleteFileCommandStrategy;
import org.bodytrack.airbot.commands.DisconnectCommandStrategy;
import org.bodytrack.airbot.commands.GetAvailableFilenamesCommandStrategy;
import org.bodytrack.airbot.commands.GetCurrentTimeCommandStrategy;
import org.bodytrack.airbot.commands.GetFileCommandStrategy;
import org.bodytrack.airbot.commands.GetProtocolVersionCommandStrategy;
import org.bodytrack.airbot.commands.GetUniqueIdCommandStrategy;
import org.bodytrack.airbot.commands.HandshakeCommandStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
class AirBotProxy implements AirBot
   {
   private static final Logger LOG = Logger.getLogger(AirBotProxy.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   public static final String APPLICATION_NAME = "AirBotProxy";
   private static final int DELAY_IN_SECONDS_BETWEEN_PINGS = 2;

   /**
    * Tries to create a <code>AirBotProxy</code> for the the serial port specified by the given
    * <code>serialPortName</code>. Returns <code>null</code> if the connection could not be established.
    *
    * @param serialPortName - the name of the serial port device which should be used to establish the connection
    *
    * @throws IllegalArgumentException if the <code>serialPortName</code> is <code>null</code>
    */
   @Nullable
   static AirBotProxy create(@Nullable final String serialPortName)
      {
      // a little error checking...
      if (serialPortName == null)
         {
         throw new IllegalArgumentException("The serial port name may not be null");
         }

      // create the serial port configuration
      final SerialIOConfiguration config = new SerialIOConfiguration(serialPortName,
                                                                     BaudRate.BAUD_115200,
                                                                     CharacterSize.EIGHT,
                                                                     Parity.NONE,
                                                                     StopBits.ONE,
                                                                     FlowControl.NONE);

      try
         {
         // create the serial port command queue (passing in a null TimeUnit causes tasks to block until complete--no timeout)
         final SerialDeviceCommandExecutionQueue commandQueue = SerialDeviceCommandExecutionQueue.create(APPLICATION_NAME, config, -1, null);

         // see whether its creation was successful
         if (commandQueue == null)
            {
            if (LOG.isEnabledFor(Level.ERROR))
               {
               LOG.error("Failed to open serial port '" + serialPortName + "'");
               }
            }
         else
            {
            if (LOG.isDebugEnabled())
               {
               LOG.debug("Serial port '" + serialPortName + "' opened.");
               }

            // now try to do the handshake with the AirBot to establish communication
            final boolean wasHandshakeSuccessful = commandQueue.executeAndReturnStatus(new HandshakeCommandStrategy());

            // see if the handshake was a success
            if (wasHandshakeSuccessful)
               {
               LOG.info("AirBot handshake successful!");

               // now create and return the proxy
               try
                  {
                  return new AirBotProxy(commandQueue, serialPortName);
                  }
               catch (InitializationException e)
                  {
                  LOG.error("InitializationException while trying to create the AirBotProxy", e);
                  CONSOLE_LOG.error("Failed to initialize AirBot.");
                  }
               catch (Exception e)
                  {
                  LOG.error("Exception while trying to create the AirBotProxy", e);
                  }
               }
            else
               {
               LOG.error("Failed to handshake with the AirBot");
               }

            // the handshake failed, so shutdown the command queue to release the serial port
            commandQueue.shutdown();
            }
         }
      catch (Exception e)
         {
         LOG.error("Exception while trying to create the AirBotProxy", e);
         }

      CONSOLE_LOG.error("Connection failed.");
      return null;
      }

   private final SerialDeviceCommandExecutionQueue commandQueue;
   private final String serialPortName;
   private final CreateLabSerialDeviceCommandStrategy disconnectCommandStrategy = new DisconnectCommandStrategy();
   private final SerialDeviceReturnValueCommandStrategy<String> getAvilableFilenamesCommandStrategy = new GetAvailableFilenamesCommandStrategy();

   private final SerialDeviceReturnValueCommandExecutor<DataFile> dataFileReturnValueCommandExecutor;
   private final SerialDeviceReturnValueCommandExecutor<Boolean> booleanReturnValueCommandExecutor;
   private final SerialDeviceReturnValueCommandExecutor<String> stringReturnValueCommandExecutor;
   private final SerialDeviceReturnValueCommandExecutor<Integer> integerReturnValueCommandExecutor;

   private final Pinger pinger = new Pinger();
   private final ScheduledExecutorService pingExecutorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory(this.getClass() + ".pingExecutorService"));
   private final ScheduledFuture<?> pingScheduledFuture;
   private final Collection<CreateLabDevicePingFailureEventListener> createLabDevicePingFailureEventListeners = new HashSet<CreateLabDevicePingFailureEventListener>();

   @NotNull
   private final AirBotConfig airBotConfig;

   private AirBotProxy(final SerialDeviceCommandExecutionQueue commandQueue, final String serialPortName) throws InitializationException
      {
      this.commandQueue = commandQueue;
      this.serialPortName = serialPortName;

      final CommandExecutionFailureHandler commandExecutionFailureHandler =
            new CommandExecutionFailureHandler()
            {
            public void handleExecutionFailure()
               {
               pinger.forceFailure();
               }
            };
      dataFileReturnValueCommandExecutor = new SerialDeviceReturnValueCommandExecutor<DataFile>(commandQueue, commandExecutionFailureHandler);
      booleanReturnValueCommandExecutor = new SerialDeviceReturnValueCommandExecutor<Boolean>(commandQueue, commandExecutionFailureHandler);
      stringReturnValueCommandExecutor = new SerialDeviceReturnValueCommandExecutor<String>(commandQueue, commandExecutionFailureHandler);
      integerReturnValueCommandExecutor = new SerialDeviceReturnValueCommandExecutor<Integer>(commandQueue, commandExecutionFailureHandler);

      // we cache all the config values since the chances of the user reconfiguring the device while the program is
      // running is low and isn't supported by the devices anyway
      final AirBotConfig tempAirBotConfig =
            new RetryingActionExecutor<AirBotConfig>()
            {
            @Override
            @Nullable
            protected AirBotConfig executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts)
               {
               final String msg = "Reading time, device ID, and version number from device (attempt " + attemptNumber + " of " + maxNumberOfAttempts + ")...";
               CONSOLE_LOG.info(msg);
               if (LOG.isInfoEnabled())
                  {
                  LOG.info("AirBotProxy.executionWorkhorse(): " + msg);
                  }
               LOG.debug("AirBotProxy.executionWorkhorse(): Reading ID...");
               final String uniqueId = trim(stringReturnValueCommandExecutor.execute(new GetUniqueIdCommandStrategy()));
               LOG.debug("AirBotProxy.executionWorkhorse(): Reading Protocol Version...");
               final Integer protocolVersion = integerReturnValueCommandExecutor.execute(new GetProtocolVersionCommandStrategy());
               LOG.debug("AirBotProxy.executionWorkhorse(): Reading Time...");
               final Integer airBotTime = integerReturnValueCommandExecutor.execute(new GetCurrentTimeCommandStrategy());

               if (uniqueId != null && uniqueId.length() > 0 && protocolVersion != null && airBotTime != null)
                  {
                  return new AirBotConfigImpl(uniqueId, protocolVersion, airBotTime);
                  }

               LOG.error("AirBotProxy.executionWorkhorse(): failed to retrieve uniqueId [" + uniqueId + "] and/or protocolVersion [" + protocolVersion + "] and/or airBotTime [" + airBotTime + "].  Returning null AirBotConfig.");
               return null;
               }
            }.execute();

      if (tempAirBotConfig == null)
         {
         final String message = "Failed to read unique ID, protocol version, and/or time from the AirBot!";
         LOG.error(message);
         CONSOLE_LOG.error(message);
         throw new InitializationException(message);
         }
      else
         {
         airBotConfig = tempAirBotConfig;
         final String message = "Successfully read unique ID, protocol version, and time from the AirBot.";
         LOG.info(message);
         CONSOLE_LOG.info(message);
         }

      // schedule periodic pings
      pingScheduledFuture = pingExecutorService.scheduleAtFixedRate(pinger,
                                                                    DELAY_IN_SECONDS_BETWEEN_PINGS, // delay before first ping
                                                                    DELAY_IN_SECONDS_BETWEEN_PINGS, // delay between pings
                                                                    TimeUnit.SECONDS);
      }

   /** Trims the given String and returns it.  Returns <code>null</code> if the given String is <code>null</code>. */
   @Nullable
   private String trim(@Nullable final String s)
      {
      if (s != null)
         {
         return s.trim();
         }
      return null;
      }

   public String getPortName()
      {
      return serialPortName;
      }

   @Override
   public void addCreateLabDevicePingFailureEventListener(final CreateLabDevicePingFailureEventListener listener)
      {
      if (listener != null)
         {
         createLabDevicePingFailureEventListeners.add(listener);
         }
      }

   @Override
   public void removeCreateLabDevicePingFailureEventListener(final CreateLabDevicePingFailureEventListener listener)
      {
      if (listener != null)
         {
         createLabDevicePingFailureEventListeners.remove(listener);
         }
      }

   @Override
   @Nullable
   public SortedSet<String> getAvailableFilenames()
      {
      String commaDelimitedFilenames = null;

      try
         {
         // pause the pinger since downloading the list of filenames may take a long time
         pinger.setPaused(true);
         commaDelimitedFilenames = stringReturnValueCommandExecutor.execute(getAvilableFilenamesCommandStrategy);
         }
      catch (Exception e)
         {
         LOG.error("Exception while downloading the list of filenames", e);
         }
      finally
         {
         // make sure the pinger is unpaused
         pinger.setPaused(false);
         }

      if (commaDelimitedFilenames != null)
         {
         final SortedSet<String> availableFiles = new TreeSet<String>();

         final String[] filenames = commaDelimitedFilenames.split(",");
         for (final String rawFilename : filenames)
            {
            if (rawFilename != null)
               {
               final String filename = rawFilename.trim();
               if (filename.length() > 0)
                  {
                  availableFiles.add(filename);
                  }
               }
            }

         return availableFiles;
         }

      return null;
      }

   @Override
   @Nullable
   public DataFile getFile(final String filename) throws NoSuchFileException
      {
      DataFile dataFile = null;

      if (filename != null)
         {
         try
            {
            // pause the pinger since file transfers may take a long time
            pinger.setPaused(true);

            // get the file
            dataFile = dataFileReturnValueCommandExecutor.execute(new GetFileCommandStrategy(filename, airBotConfig));
            }
         catch (Exception e)
            {
            LOG.error("AirBotProxy.downloadFile(): Exception while trying to download file [" + filename + "]", e);
            }
         finally
            {
            // make sure the pinger is unpaused
            pinger.setPaused(false);
            }
         }

      if (dataFile != null && dataFile.isEmpty())
         {
         throw new NoSuchFileException("File '" + filename + "' not found");
         }

      return dataFile;
      }

   @Override
   public boolean deleteFile(final String filename)
      {
      if (filename != null && filename.length() > 0)
         {
         try
            {
            // pause the pinger since deletes may take a long time
            pinger.setPaused(true);
            return booleanReturnValueCommandExecutor.execute(new DeleteFileCommandStrategy(filename));
            }
         catch (Exception e)
            {
            LOG.error("Exception while deleting file [" + filename + "]", e);
            }
         finally
            {
            // make sure the pinger is unpaused
            pinger.setPaused(false);
            }
         }
      else
         {
         LOG.error("AirBotProxy.deleteFile(): filename cannot be null or empty.");
         }
      return false;
      }

   @Override
   @NotNull
   public AirBotConfig getAirBotConfig()
      {
      return airBotConfig;
      }

   public void disconnect()
      {
      disconnect(true);
      }

   private void disconnect(final boolean willAddDisconnectCommandToQueue)
      {
      if (LOG.isDebugEnabled())
         {
         LOG.debug("AirBotProxy.disconnect(" + willAddDisconnectCommandToQueue + ")");
         }

      // turn off the pinger
      try
         {
         pingScheduledFuture.cancel(false);
         pingExecutorService.shutdownNow();
         LOG.debug("AirBotProxy.disconnect(): Successfully shut down the AirBot pinger.");
         }
      catch (Exception e)
         {
         LOG.error("AirBotProxy.disconnect(): Exception caught while trying to shut down pinger", e);
         }

      // optionally send goodbye command to the AirBot
      if (willAddDisconnectCommandToQueue)
         {
         LOG.debug("AirBotProxy.disconnect(): Now attempting to send the disconnect command to the AirBot");
         try
            {
            if (commandQueue.executeAndReturnStatus(disconnectCommandStrategy))
               {
               LOG.debug("AirBotProxy.disconnect(): Successfully disconnected from the AirBot.");
               }
            else
               {
               LOG.error("AirBotProxy.disconnect(): Failed to disconnect from the AirBot.");
               }
            }
         catch (Exception e)
            {
            LOG.error("Exception caught while trying to execute the disconnect", e);
            }
         }

      // shut down the command queue, which closes the serial port
      try
         {
         LOG.debug("AirBotProxy.disconnect(): shutting down the SerialDeviceCommandExecutionQueue...");
         commandQueue.shutdown();
         LOG.debug("AirBotProxy.disconnect(): done shutting down the SerialDeviceCommandExecutionQueue");
         }
      catch (Exception e)
         {
         LOG.error("AirBotProxy.disconnect(): Exception while trying to shut down the SerialDeviceCommandExecutionQueue", e);
         }
      }

   private abstract static class RetryingActionExecutor<ReturnType>
      {
      private static final int MAX_RETRIES = 3;

      /**
       * Executes an action and returns the result, retrying if necessary up to three times.  Returns <code>null</code>
       * only if it fails to get a non-<code>null</code> result.
       */
      @SuppressWarnings({"BusyWait"})
      @Nullable
      protected final ReturnType execute()
         {
         int retryCount = 0;
         do
            {
            try
               {
               final ReturnType val = executionWorkhorse(retryCount + 1, MAX_RETRIES);
               if (val != null)
                  {
                  return val;
                  }
               }
            catch (Exception e)
               {
               LOG.error("Exception while executing the action", e);
               }
            retryCount++;
            if (retryCount < MAX_RETRIES)
               {
               try
                  {
                  Thread.sleep(200);
                  }
               catch (InterruptedException e)
                  {
                  LOG.error("InterruptedException while sleeping", e);
                  }
               }
            }
         while (retryCount < MAX_RETRIES);

         return null;
         }

      @Nullable
      protected abstract ReturnType executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts);
      }

   private static final class AirBotConfigImpl implements AirBotConfig
      {
      @NotNull
      private final String id;
      private final int protocolVersion;
      private final long powerOnTimeInMillis;

      private AirBotConfigImpl(@NotNull final String id, final int protocolVersion, final int currentAirBotTimeInSeconds)
         {
         this.id = id;
         this.protocolVersion = protocolVersion;
         this.powerOnTimeInMillis = System.currentTimeMillis() - (currentAirBotTimeInSeconds * 1000);
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
      public long getPowerOnTimeInMillis()
         {
         return powerOnTimeInMillis;
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

         final AirBotConfigImpl that = (AirBotConfigImpl)o;

         if (powerOnTimeInMillis != that.powerOnTimeInMillis)
            {
            return false;
            }
         if (protocolVersion != that.protocolVersion)
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
         result = 31 * result + (int)(powerOnTimeInMillis ^ (powerOnTimeInMillis >>> 32));
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("AirBotConfigImpl");
         sb.append("{id='").append(id).append('\'');
         sb.append(", protocolVersion=").append(protocolVersion);
         sb.append(", powerOnTimeInMillis=").append(powerOnTimeInMillis);
         sb.append('}');
         return sb.toString();
         }
      }

   private class Pinger implements Runnable
      {
      private final SerialDeviceReturnValueCommandStrategy<Integer> pingCommandStrategy = new GetProtocolVersionCommandStrategy();
      private boolean isPaused = false;
      private final Lock lock = new ReentrantLock();

      public void setPaused(final boolean isPaused)
         {
         lock.lock();  // block until condition holds
         try
            {
            this.isPaused = isPaused;
            if (LOG.isDebugEnabled())
               {
               LOG.debug("AirBotProxy$Pinger.setPaused(): pinger paused = [" + isPaused + "]");
               }
            }
         finally
            {
            lock.unlock();
            }
         }

      public void run()
         {
         lock.lock();  // block until condition holds
         try
            {
            if (isPaused)
               {
               LOG.trace("AirBotProxy$Pinger.run(): not pinging because the pinger is paused");
               }
            else
               {
               final boolean pingSuccessful = commandQueue.executeAndReturnStatus(pingCommandStrategy);

               // if the ping failed, then we know we have a problem, so disconnect (which
               // probably won't work) and then notify the listeners
               if (!pingSuccessful)
                  {
                  handlePingFailure();
                  }
               }
            }
         catch (Exception e)
            {
            LOG.error("AirBotProxy$Pinger.run(): Exception caught while executing the pinger", e);
            }
         finally
            {
            lock.unlock();
            }
         }

      private void handlePingFailure()
         {
         try
            {
            LOG.debug("AirBotProxy$Pinger.handlePingFailure(): Ping failed.  Attempting to disconnect...");
            disconnect(false);
            LOG.debug("AirBotProxy$Pinger.handlePingFailure(): Done disconnecting from the AirBot");
            }
         catch (Exception e)
            {
            LOG.error("AirBotProxy$Pinger.handlePingFailure(): Exeption caught while trying to disconnect from the AirBot", e);
            }

         if (LOG.isDebugEnabled())
            {
            LOG.debug("AirBotProxy$Pinger.handlePingFailure(): Notifying " + createLabDevicePingFailureEventListeners.size() + " listeners of ping failure...");
            }
         for (final CreateLabDevicePingFailureEventListener listener : createLabDevicePingFailureEventListeners)
            {
            try
               {
               if (LOG.isDebugEnabled())
                  {
                  LOG.debug("   AirBotProxy$Pinger.handlePingFailure(): Notifying " + listener);
                  }
               listener.handlePingFailureEvent();
               }
            catch (Exception e)
               {
               LOG.error("AirBotProxy$Pinger.handlePingFailure(): Exeption caught while notifying SerialDevicePingFailureEventListener", e);
               }
            }
         }

      private void forceFailure()
         {
         handlePingFailure();
         }
      }
   }
