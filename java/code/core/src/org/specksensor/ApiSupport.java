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
   public static final int DEFAULT_LOGGING_INTERVAL_SECONDS = 1;

   private static final Map<Integer, ApiSupport> INSTANCES = new HashMap<Integer, ApiSupport>(2);

   public static final ApiSupport DEFAULT;

   static
      {
      INSTANCES.put(1, new ApiSupport(false, false, true));
      INSTANCES.put(2, new ApiSupport(true, true, false));
      DEFAULT = INSTANCES.get(1);
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

   private boolean hasMutableLoggingInterval;
   private boolean canGetNumberOfDataSamples;
   private boolean hasTemperatureSensor;

   // private to prevent instantiation
   private ApiSupport(final boolean hasMutableLoggingInterval, final boolean canGetNumberOfDataSamples, final boolean hasTemperatureSensor)
      {
      this.hasMutableLoggingInterval = hasMutableLoggingInterval;
      this.canGetNumberOfDataSamples = canGetNumberOfDataSamples;
      this.hasTemperatureSensor = hasTemperatureSensor;
      }

   public boolean hasMutableLoggingInterval()
      {
      return hasMutableLoggingInterval;
      }

   public boolean canGetNumberOfDataSamples()
      {
      return canGetNumberOfDataSamples;
      }

   public boolean hasTemperatureSensor()
      {
      return hasTemperatureSensor;
      }
   }
