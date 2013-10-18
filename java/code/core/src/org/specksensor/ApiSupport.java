package org.specksensor;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>ApiSupport</code> provides a centralized way to check feature support based on the protocol version.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ApiSupport
   {
   public static final int DEFAULT_PROTOCOL_VERSION = 1;

   private static final Map<Integer, ApiSupport> INSTANCES = new HashMap<Integer, ApiSupport>(2);

   public static final ApiSupport DEFAULT;

   static
      {
      INSTANCES.put(1, new ApiSupport(1, false, true, false, true));
      DEFAULT = INSTANCES.get(DEFAULT_PROTOCOL_VERSION);
      }

   /**
    * Gets the <code>ApiSupport</code> for the given version number.  Returns the {@link #DEFAULT} instance if the
    * version number is not recognized.
    *
    * @see #isVersionNumberValid(int)
    */
   @NotNull
   public static ApiSupport getInstance(final int versionNumber)
      {
      if (isVersionNumberValid(versionNumber))
         {
         return INSTANCES.get(versionNumber);
         }
      return DEFAULT;
      }

   public static boolean isVersionNumberValid(final int versionNumber)
      {
      return INSTANCES.containsKey(versionNumber);
      }

   private final int protocolVersion;
   private final boolean canMutateLoggingInterval;
   private final boolean canGetNumberOfDataSamples;
   private final boolean hasTemperatureSensor;
   private final boolean canEnterBootloaderMode;

   // private to prevent instantiation
   private ApiSupport(final int protocolVersion,
                      final boolean canMutateLoggingInterval,
                      final boolean canGetNumberOfDataSamples,
                      final boolean hasTemperatureSensor,
                      final boolean canEnterBootloaderMode)
      {
      this.protocolVersion = protocolVersion;
      this.canMutateLoggingInterval = canMutateLoggingInterval;
      this.canGetNumberOfDataSamples = canGetNumberOfDataSamples;
      this.hasTemperatureSensor = hasTemperatureSensor;
      this.canEnterBootloaderMode = canEnterBootloaderMode;
      }

   public int getProtocolVersion()
      {
      return protocolVersion;
      }

   public boolean canMutateLoggingInterval()
      {
      return canMutateLoggingInterval;
      }

   public boolean canGetNumberOfDataSamples()
      {
      return canGetNumberOfDataSamples;
      }

   public boolean hasTemperatureSensor()
      {
      return hasTemperatureSensor;
      }

   public boolean canEnterBootloaderMode()
      {
      return canEnterBootloaderMode;
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

      final ApiSupport that = (ApiSupport)o;

      if (canEnterBootloaderMode != that.canEnterBootloaderMode)
         {
         return false;
         }
      if (canGetNumberOfDataSamples != that.canGetNumberOfDataSamples)
         {
         return false;
         }
      if (canMutateLoggingInterval != that.canMutateLoggingInterval)
         {
         return false;
         }
      if (hasTemperatureSensor != that.hasTemperatureSensor)
         {
         return false;
         }
      if (protocolVersion != that.protocolVersion)
         {
         return false;
         }

      return true;
      }

   @Override
   public int hashCode()
      {
      int result = protocolVersion;
      result = 31 * result + (canMutateLoggingInterval ? 1 : 0);
      result = 31 * result + (canGetNumberOfDataSamples ? 1 : 0);
      result = 31 * result + (hasTemperatureSensor ? 1 : 0);
      result = 31 * result + (canEnterBootloaderMode ? 1 : 0);
      return result;
      }
   }
