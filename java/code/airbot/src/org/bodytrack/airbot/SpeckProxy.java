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
import org.bodytrack.airbot.commands.GetSpeckConfigCommandStrategy;
import org.bodytrack.airbot.commands.GetDataSampleCommandStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
class SpeckProxy implements Speck
   {
   private static final Logger LOG = Logger.getLogger(SpeckProxy.class);
   private static final Logger CONSOLE_LOG = Logger.getLogger("ConsoleLog");

   private static final int DELAY_IN_SECONDS_BETWEEN_PINGS = 5;

   /**
    * Tries to create a <code>SpeckProxy</code>. Returns <code>null</code> if the connection could not be established.
    */
   @Nullable
   static SpeckProxy create()
      {
      try
         {
         // create the HID device
         if (LOG.isDebugEnabled())
            {
            LOG.debug("SpeckProxy.create(): creating HID device for vendor ID [" + Integer.toHexString(SpeckConstants.UsbHidConfiguration.USB_VENDOR_ID) + "] and product ID [" + Integer.toHexString(SpeckConstants.UsbHidConfiguration.USB_PRODUCT_ID) + "]");
            }
         final HIDDevice hidDevice = HIDDeviceFactory.create(SpeckConstants.UsbHidConfiguration.HID_DEVICE_DESCRIPTOR);

         LOG.debug("SpeckProxy.create(): attempting connection...");
         hidDevice.connectExclusively();

         // create the HID device command execution queue (which will attempt to connect to the device)
         final HIDCommandExecutionQueue commandQueue = new HIDCommandExecutionQueue(hidDevice);

         // create the SpeckProxy
         return new SpeckProxy(commandQueue, hidDevice);
         }
      catch (HIDConnectionException e)
         {
         LOG.error("HIDConnectionException while trying to connect to the Speck, returning null", e);
         }
      catch (HIDDeviceNotFoundException e)
         {
         LOG.error("HIDDeviceNotFoundException while trying to connect to the Speck, returning null", e);
         }
      catch (InitializationException e)
         {
         LOG.error("InitializationException while trying to connect to the Speck, returning null", e);
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
   private final SpeckConfig speckConfig;

   private SpeckProxy(final HIDCommandExecutionQueue commandQueue, final HIDDevice hidDevice) throws InitializationException
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
      final HIDDeviceReturnValueCommandExecutor<SpeckConfig> hidDeviceReturnValueCommandExecutor = new HIDDeviceReturnValueCommandExecutor<SpeckConfig>(commandQueue, commandExecutionFailureHandler);

      // we cache all the config values since the chances of the user reconfiguring the device while the program is
      // running is low and isn't supported by the devices anyway
      final SpeckConfig tempSpeckConfig =
            new RetryingActionExecutor<SpeckConfig>()
            {
            @Override
            @Nullable
            protected SpeckConfig executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts)
               {
               final String msg = "Reading device ID from Speck (attempt " + attemptNumber + " of " + maxNumberOfAttempts + ")...";
               CONSOLE_LOG.info(msg);
               if (LOG.isInfoEnabled())
                  {
                  LOG.info("SpeckProxy.executionWorkhorse(): " + msg);
                  }
               LOG.debug("SpeckProxy.executionWorkhorse(): Reading ID...");
               return hidDeviceReturnValueCommandExecutor.execute(new GetSpeckConfigCommandStrategy());
               }
            }.execute();

      if (tempSpeckConfig == null)
         {
         final String message = "Failed to read unique ID from the Speck!";
         LOG.error(message);
         CONSOLE_LOG.error(message);
         throw new InitializationException(message);
         }
      else
         {
         speckConfig = tempSpeckConfig;
         final String message = "Successfully read unique ID from the Speck.";
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
         throw new CommunicationException("Failed to read a sample from the Speck");
         }
      return dataSample;
      }

   @Override
   public boolean deleteSample(@Nullable final Speck.DataSample dataSample) throws CommunicationException
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
         throw new CommunicationException("Failed to delete a sample [" + sampleTime + "] from the Speck");
         }
      return success;
      }

   @Override
   @NotNull
   public SpeckConfig getSpeckConfig()
      {
      return speckConfig;
      }

   public void disconnect()
      {
      if (LOG.isDebugEnabled())
         {
         LOG.debug("SpeckProxy.disconnect()");
         }

      // turn off the pinger
      try
         {
         pingScheduledFuture.cancel(false);
         pingExecutorService.shutdownNow();
         LOG.debug("SpeckProxy.disconnect(): Successfully shut down the Speck pinger.");
         }
      catch (Exception e)
         {
         LOG.error("SpeckProxy.disconnect(): Exception caught while trying to shut down pinger", e);
         }

      // shut down the command queue, which closes the serial port
      try
         {
         LOG.debug("SpeckProxy.disconnect(): shutting down the SerialDeviceCommandExecutionQueue...");
         commandQueue.shutdown();
         LOG.debug("SpeckProxy.disconnect(): done shutting down the SerialDeviceCommandExecutionQueue");
         }
      catch (Exception e)
         {
         LOG.error("SpeckProxy.disconnect(): Exception while trying to shut down the SerialDeviceCommandExecutionQueue", e);
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
               LOG.debug("SpeckProxy$Pinger.setPaused(): pinger paused = [" + isPaused + "]");
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
               LOG.trace("SpeckProxy$Pinger.run(): not pinging because the pinger is paused");
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
            LOG.error("SpeckProxy$Pinger.run(): Exception caught while executing the pinger", e);
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
            LOG.debug("SpeckProxy$Pinger.handlePingFailure(): Ping failed.  Attempting to disconnect...");
            disconnect();
            LOG.debug("SpeckProxy$Pinger.handlePingFailure(): Done disconnecting from the Speck");
            }
         catch (Exception e)
            {
            LOG.error("SpeckProxy$Pinger.handlePingFailure(): Exeption caught while trying to disconnect from the Speck", e);
            }

         if (LOG.isDebugEnabled())
            {
            LOG.debug("SpeckProxy$Pinger.handlePingFailure(): Notifying " + createLabDevicePingFailureEventListeners.size() + " listeners of ping failure...");
            }
         for (final CreateLabDevicePingFailureEventListener listener : createLabDevicePingFailureEventListeners)
            {
            try
               {
               if (LOG.isDebugEnabled())
                  {
                  LOG.debug("   SpeckProxy$Pinger.handlePingFailure(): Notifying " + listener);
                  }
               listener.handlePingFailureEvent();
               }
            catch (Exception e)
               {
               LOG.error("SpeckProxy$Pinger.handlePingFailure(): Exeption caught while notifying SerialDevicePingFailureEventListener", e);
               }
            }
         }

      private void forceFailure()
         {
         handlePingFailure();
         }
      }
   }
