package org.specksensor;

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
import edu.cmu.ri.createlab.usb.hid.HIDDeviceNoReturnValueCommandExecutor;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceNotFoundException;
import edu.cmu.ri.createlab.usb.hid.HIDDeviceReturnValueCommandExecutor;
import edu.cmu.ri.createlab.util.commandexecution.CommandExecutionFailureHandler;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.specksensor.commands.DeleteSampleCommandStrategy;
import org.specksensor.commands.EnterBootloaderModeCommandStrategy;
import org.specksensor.commands.GetDataSampleCommandStrategy;
import org.specksensor.commands.GetDataSampleCountCommandStrategy;
import org.specksensor.commands.ReadExtendedSpeckConfigCommandStrategy;
import org.specksensor.commands.ReadWriteSpeckConfigCommandStrategy;

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
   private final GetDataSampleCountCommandStrategy getDataSampleCountCommandStrategy = new GetDataSampleCountCommandStrategy();
   private final HIDDeviceReturnValueCommandExecutor<SpeckConfig> speckConfigReturnValueCommandExecutor;
   private final HIDDeviceReturnValueCommandExecutor<DataSample> getSampleCommandExecutor;
   private final HIDDeviceReturnValueCommandExecutor<Boolean> booleanReturnValueCommandExecutor;
   private final HIDDeviceReturnValueCommandExecutor<Integer> integerReturnValueCommandExecutor;

   // Using SpeckConfigWrapper here lets us pass around a reference to the
   // wrapper, but also lets us modify the wrapped config
   @NotNull
   private final SpeckConfigWrapper speckConfigWrapper;

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
      booleanReturnValueCommandExecutor = new HIDDeviceReturnValueCommandExecutor<Boolean>(commandQueue, commandExecutionFailureHandler);
      integerReturnValueCommandExecutor = new HIDDeviceReturnValueCommandExecutor<Integer>(commandQueue, commandExecutionFailureHandler);
      speckConfigReturnValueCommandExecutor = new HIDDeviceReturnValueCommandExecutor<SpeckConfig>(commandQueue, commandExecutionFailureHandler);

      // we cache the config since writes are much less common than reads
      final SpeckConfig tempSpeckConfig =
            new RetryingActionExecutor<SpeckConfig>()
            {
            private final ReadWriteSpeckConfigCommandStrategy getSpeckConfigCommandStrategy = ReadWriteSpeckConfigCommandStrategy.createReadableSpeckConfigCommandStrategy();

            @Override
            @Nullable
            protected SpeckConfig executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts)
               {
               final String msg = "Reading Speck config (attempt " + attemptNumber + " of " + maxNumberOfAttempts + ")...";
               CONSOLE_LOG.info(msg);
               if (LOG.isInfoEnabled())
                  {
                  LOG.info("SpeckProxy.executionWorkhorse(): " + msg);
                  }
               LOG.debug("SpeckProxy.executionWorkhorse(): Reading config...");
               return speckConfigReturnValueCommandExecutor.execute(getSpeckConfigCommandStrategy);
               }
            }.execute();

      if (tempSpeckConfig == null)
         {
         final String message = "Failed to read the Speck config!";
         LOG.error(message);
         CONSOLE_LOG.error(message);
         throw new InitializationException(message);
         }
      else
         {

         // if we got a valid SpeckConfig, we now need to check whether we need to get the extended config
         if (tempSpeckConfig.getApiSupport().hasExtendedId())
            {
            final SpeckConfig tempExtendedSpeckConfig =
                  new RetryingActionExecutor<SpeckConfig>()
                  {
                  private final ReadExtendedSpeckConfigCommandStrategy getExtendedSpeckConfigCommandStrategy = new ReadExtendedSpeckConfigCommandStrategy(tempSpeckConfig);

                  @Override
                  @Nullable
                  protected SpeckConfig executionWorkhorse(final int attemptNumber, final int maxNumberOfAttempts)
                     {
                     final String msg = "Reading extended Speck config (attempt " + attemptNumber + " of " + maxNumberOfAttempts + ")...";
                     CONSOLE_LOG.info(msg);
                     if (LOG.isInfoEnabled())
                        {
                        LOG.info("SpeckProxy.executionWorkhorse(): " + msg);
                        }
                     LOG.debug("SpeckProxy.executionWorkhorse(): Reading extended config...");
                     return speckConfigReturnValueCommandExecutor.execute(getExtendedSpeckConfigCommandStrategy);
                     }
                  }.execute();

            if (tempExtendedSpeckConfig == null)
               {
               final String message = "Failed to read the extended Speck config!";
               LOG.error(message);
               CONSOLE_LOG.error(message);
               throw new InitializationException(message);
               }
            else
               {
               speckConfigWrapper = new SpeckConfigWrapper(tempExtendedSpeckConfig);
               }
            }
         else
            {
            speckConfigWrapper = new SpeckConfigWrapper(tempSpeckConfig);
            }

         final String message = "Successfully read the Speck config.";
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
      return dataSample != null && deleteSample(dataSample.getSampleTime());
      }

   @Override
   public boolean deleteSample(final int sampleTime) throws CommunicationException
      {
      final Boolean success = booleanReturnValueCommandExecutor.execute(new DeleteSampleCommandStrategy(sampleTime));
      if (success == null)
         {
         throw new CommunicationException("Failed to delete a sample [" + sampleTime + "] from the Speck");
         }
      return success;
      }

   @Override
   public int getNumberOfAvailableSamples() throws CommunicationException
      {
      if (speckConfigWrapper.getApiSupport().canGetNumberOfDataSamples())
         {
         final Integer count = integerReturnValueCommandExecutor.execute(getDataSampleCountCommandStrategy);
         if (count == null)
            {
            throw new CommunicationException("Failed to read the number of available samples from the Speck");
            }
         return count;
         }
      throw new UnsupportedOperationException("This Speck cannot report the number of available samples.");
      }

   @NotNull
   public SpeckConfig setLoggingInterval(final int loggingIntervalInSeconds) throws CommunicationException, UnsupportedOperationException
      {
      if (speckConfigWrapper.getApiSupport().canMutateLoggingInterval())
         {
         final SpeckConfig newConfig = speckConfigReturnValueCommandExecutor.execute(ReadWriteSpeckConfigCommandStrategy.createWriteableSpeckConfigCommandStrategy(loggingIntervalInSeconds));
         if (newConfig != null)
            {
            // get the extended config, if necessary
            if (newConfig.getApiSupport().hasExtendedId())
               {
               final SpeckConfig extendedSpeckConfig = speckConfigReturnValueCommandExecutor.execute(new ReadExtendedSpeckConfigCommandStrategy(newConfig));
               if (extendedSpeckConfig != null)
                  {
                  speckConfigWrapper.setSpeckConfig(extendedSpeckConfig);
                  }
               else
                  {
                  throw new CommunicationException("Failed to set the Specks's logging interval");
                  }
               }
            else
               {
               speckConfigWrapper.setSpeckConfig(newConfig);
               }
            return speckConfigWrapper;
            }
         throw new CommunicationException("Failed to set the Specks's logging interval");
         }
      throw new UnsupportedOperationException("The logging interval for this Speck cannot be modified.");
      }

   @Override
   public void enterBootloaderMode() throws UnsupportedOperationException
      {
      if (!speckConfigWrapper.getApiSupport().canEnterBootloaderMode())
         {
         throw new UnsupportedOperationException("This Speck does not support the command to enter bootloader mode.");
         }

      LOG.debug("SpeckProxy.enterBootloaderMode(): Pausing the pinger...");
      pinger.setPaused(true);
      LOG.debug("SpeckProxy.enterBootloaderMode(): Executing the command to enter bootloader mode...");
      final HIDDeviceNoReturnValueCommandExecutor noReturnValueCommandExecutor = new HIDDeviceNoReturnValueCommandExecutor(commandQueue,
                                                                                                                           new CommandExecutionFailureHandler()
                                                                                                                           {
                                                                                                                           @Override
                                                                                                                           public void handleExecutionFailure()
                                                                                                                              {
                                                                                                                              LOG.debug("SpeckProxy.handleExecutionFailure(): failure detected after executing command to enter bootloader mode.");
                                                                                                                              }
                                                                                                                           });

      LOG.debug("SpeckProxy.enterBootloaderMode(): execute returned [" + noReturnValueCommandExecutor.execute(new EnterBootloaderModeCommandStrategy()) + "], disconnecting...");
      disconnect();
      }

   @Override
   @NotNull
   public SpeckConfig getSpeckConfig()
      {
      return speckConfigWrapper;
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

   private final class SpeckConfigWrapper implements SpeckConfig
      {
      @NotNull
      private SpeckConfig speckConfig;

      private SpeckConfigWrapper(@NotNull final SpeckConfig speckConfig)
         {
         this.speckConfig = speckConfig;
         }

      private void setSpeckConfig(@NotNull final SpeckConfig speckConfig)
         {
         this.speckConfig = speckConfig;
         }

      @Override
      @NotNull
      public String getId()
         {
         return speckConfig.getId();
         }

      @Override
      public int getProtocolVersion()
         {
         return speckConfig.getProtocolVersion();
         }

      @Override
      public int getHardwareVersion()
         {
         return speckConfig.getHardwareVersion();
         }

      @Override
      public int getFirmwareVersion()
         {
         return speckConfig.getFirmwareVersion();
         }

      @Override
      public int getLoggingInterval()
         {
         return speckConfig.getLoggingInterval();
         }

      @Override
      @NotNull
      public ApiSupport getApiSupport()
         {
         return speckConfig.getApiSupport();
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
