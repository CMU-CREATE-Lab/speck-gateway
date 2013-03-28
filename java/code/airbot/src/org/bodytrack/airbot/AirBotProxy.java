package org.bodytrack.airbot;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.usb.hid.HIDCommandExecutionQueue;
import edu.cmu.ri.createlab.usb.hid.HIDConnectionException;
import edu.cmu.ri.createlab.usb.hid.HIDDevice;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceFactory;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceNotFoundException;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceReturnValueCommandExecutor;
import edu.cmu.ri.createlab.util.commandexecution.CommandExecutionFailureHandler;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.commands.DeleteSampleCommandStrategy;
import org.bodytrack.airbot.commands.GetAirBotConfigCommandStrategy;
import org.bodytrack.airbot.commands.GetDataSampleCommandStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
class AirBotProxy implements AirBot
   {
   private static final Logger LOG = Logger.getLogger(AirBotProxy.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   private static final int DELAY_IN_SECONDS_BETWEEN_PINGS = 5;

   /**
    * Tries to create an <code>AirBotProxy</code>. Returns <code>null</code> if the connection could not be established.
    */
   @Nullable
   static AirBotProxy create()
      {
      try
         {
         // create the HID device
         if (LOG.isDebugEnabled())
            {
            LOG.debug("AirBotProxy.create(): creating HID device for vendor ID [" + Integer.toHexString(AirBotUploaderConstants.UsbHidConfiguration.USB_VENDOR_ID) + "] and product ID [" + Integer.toHexString(AirBotUploaderConstants.UsbHidConfiguration.USB_PRODUCT_ID) + "]");
            }
         final HIDDevice hidDevice = HIDDeviceFactory.create(AirBotUploaderConstants.UsbHidConfiguration.HID_DEVICE_DESCRIPTOR);

         LOG.debug("AirBotProxy.create(): attempting connection...");
         hidDevice.connectExclusively();

         // create the HID device command execution queue (which will attempt to connect to the device)
         final HIDCommandExecutionQueue commandQueue = new HIDCommandExecutionQueue(hidDevice);

         // create the AirBotProxy
         return new AirBotProxy(commandQueue, hidDevice);
         }
      catch (HIDConnectionException e)
         {
         LOG.error("HIDConnectionException while trying to connect to the AirBot, returning null", e);
         }
      catch (HIDDeviceNotFoundException e)
         {
         LOG.error("HIDDeviceNotFoundException while trying to connect to the AirBot, returning null", e);
         }
      catch (InitializationException e)
         {
         LOG.error("InitializationException while trying to connect to the AirBot, returning null", e);
         }
      return null;
      }

   private final HIDCommandExecutionQueue commandQueue;
   private final HIDDevice hidDevice;

   private final Pinger pinger = new Pinger();
   private final ScheduledExecutorService pingExecutorService = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory(this.getClass() + ".pingExecutorService"));
   private final ScheduledFuture<?> pingScheduledFuture;
   private final Collection<CreateLabDevicePingFailureEventListener> createLabDevicePingFailureEventListeners = new HashSet<CreateLabDevicePingFailureEventListener>();
   private final GetDataSampleCommandStrategy getCurrentSampleCommandStrategy = GetDataSampleCommandStrategy.createGetCurrentSampleCommandStrategy();
   private final GetDataSampleCommandStrategy getHistoricSampleCommandStrategy = GetDataSampleCommandStrategy.createGetHistoricSampleCommandStrategy();
   private final HIDDeviceReturnValueCommandExecutor<DataSample> getSampleCommandExecutor;
   private final HIDDeviceReturnValueCommandExecutor<Boolean> deleteSampleCommandExecutor;

   @NotNull
   private final AirBotConfig airBotConfig;

   private AirBotProxy(final HIDCommandExecutionQueue commandQueue, final HIDDevice hidDevice) throws InitializationException
      {
      this.commandQueue = commandQueue;
      this.hidDevice = hidDevice;

      final CommandExecutionFailureHandler commandExecutionFailureHandler =
            new CommandExecutionFailureHandler()
            {
            public void handleExecutionFailure()
               {
               pinger.forceFailure();
               }
            };

      getSampleCommandExecutor = new HIDDeviceReturnValueCommandExecutor<DataSample>(commandQueue, commandExecutionFailureHandler);
      deleteSampleCommandExecutor = new HIDDeviceReturnValueCommandExecutor<Boolean>(commandQueue, commandExecutionFailureHandler);
      final HIDDeviceReturnValueCommandExecutor<AirBotConfig> airBotConfigReturnValueCommandExecutor = new HIDDeviceReturnValueCommandExecutor<AirBotConfig>(commandQueue, commandExecutionFailureHandler);

      // we cache all the config values since the chances of the user reconfiguring the device while the program is
      // running is low and isn't supported by the devices anyway
      final AirBotConfig tempAirBotConfig =
            new RetryingActionExecutor<AirBotConfig>()
            {
            @Override
            @Nullable
            protected AirBotConfig executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts)
               {
               final String msg = "Reading device ID from AirBot (attempt " + attemptNumber + " of " + maxNumberOfAttempts + ")...";
               CONSOLE_LOG.info(msg);
               if (LOG.isInfoEnabled())
                  {
                  LOG.info("AirBotProxy.executionWorkhorse(): " + msg);
                  }
               LOG.debug("AirBotProxy.executionWorkhorse(): Reading ID...");
               return airBotConfigReturnValueCommandExecutor.execute(new GetAirBotConfigCommandStrategy());
               }
            }.execute();

      if (tempAirBotConfig == null)
         {
         final String message = "Failed to read unique ID from the AirBot!";
         LOG.error(message);
         CONSOLE_LOG.error(message);
         throw new InitializationException(message);
         }
      else
         {
         airBotConfig = tempAirBotConfig;
         final String message = "Successfully read unique ID from the AirBot.";
         LOG.info(message);
         CONSOLE_LOG.info(message);
         }

      // TODO: smarter pinging?
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
      return hidDevice.getDeviceFilename();
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

   @Nullable
   @Override
   public DataSample getSample() throws CommunicationException
      {
      final DataSample sample = getSample(getHistoricSampleCommandStrategy);

      // if the sample is empty, then no data is available, so return null as specified in the API
      if (sample.isEmpty())
         {
         return null;
         }

      return sample;
      }

   @NotNull
   @Override
   public DataSample getCurrentSample() throws CommunicationException
      {
      return getSample(getCurrentSampleCommandStrategy);
      }

   @NotNull
   private DataSample getSample(final GetDataSampleCommandStrategy sampleCommandStrategy) throws CommunicationException
      {
      final DataSample dataSample = getSampleCommandExecutor.execute(sampleCommandStrategy);
      if (dataSample == null)
         {
         throw new CommunicationException("Failed to read a sample from the AirBot");
         }
      return dataSample;
      }

   @Override
   public boolean deleteSample(@Nullable final AirBot.DataSample dataSample) throws CommunicationException
      {
      if (dataSample != null)
         {
         return deleteSample(dataSample.getSampleTime());
         }

      return false;
      }

   @Override
   public boolean deleteSample(final int sampleTime) throws CommunicationException
      {
      final Boolean success = deleteSampleCommandExecutor.execute(new DeleteSampleCommandStrategy(sampleTime));
      if (success == null)
         {
         throw new CommunicationException("Failed to delete a sample [" + sampleTime + "] from the AirBot");
         }
      return success;
      }

   @Override
   @NotNull
   public AirBotConfig getAirBotConfig()
      {
      return airBotConfig;
      }

   public void disconnect()
      {
      if (LOG.isDebugEnabled())
         {
         LOG.debug("AirBotProxy.disconnect()");
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
      private static final int SLEEP_DURATION_IN_MILLIS = 200;

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
                  Thread.sleep(SLEEP_DURATION_IN_MILLIS);
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

   private class Pinger implements Runnable
      {
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
               // try to read the current sample, but don't do anything with it.  If it fails, it'll throw a
               // CommunicationException, which will cause the ping to fail.
               getCurrentSample();
               }
            }
         catch (Exception e)
            {
            LOG.error("AirBotProxy$Pinger.run(): Exception caught while executing the pinger", e);
            handlePingFailure();
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
            disconnect();
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
