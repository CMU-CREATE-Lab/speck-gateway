package org.specksensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
      INSTANCES.put(1,
                    new ApiSupport(
                          1,              // protocol version
                          false,          // can mutate logging interval
                          false,          // can get data sample count
                          true,           // has temperature sensor
                          true,           // has particle count
                          false,          // has particle concentration
                          false,          // has device version info
                          false,          // has extended ID
                          false           // can enter bootloader mode
                    ));
      INSTANCES.put(2,
                    new ApiSupport(
                          2,              // protocol version
                          true,           // can mutate logging interval
                          true,           // can get data sample count
                          false,          // has temperature sensor
                          true,           // has particle count
                          false,          // has particle concentration
                          false,          // has device version info
                          false,          // has extended ID
                          true            // can enter bootloader mode
                    ));
      INSTANCES.put(3,
                    new ApiSupport(
                          3,              // protocol version
                          true,           // can mutate logging interval
                          true,           // can get data sample count
                          false,          // has temperature sensor
                          false,          // has particle count
                          true,           // has particle concentration
                          true,           // has device version info
                          true,           // has extended ID
                          true            // can enter bootloader mode
                    ));
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
   private final boolean hasParticleCount;
   private final boolean hasParticleConcentration;
   private final boolean hasDeviceVersionInfo;
   private final boolean hasExtendedId;
   private final boolean canEnterBootloaderMode;
   private final List<String> dataSampleFieldNames;

   // private to prevent instantiation
   private ApiSupport(final int protocolVersion,
                      final boolean canMutateLoggingInterval,
                      final boolean canGetNumberOfDataSamples,
                      final boolean hasTemperatureSensor,
                      final boolean hasParticleCount,
                      final boolean hasParticleConcentration,
                      final boolean hasDeviceVersionInfo,
                      final boolean hasExtendedId,
                      final boolean canEnterBootloaderMode)
      {
      this.protocolVersion = protocolVersion;
      this.canMutateLoggingInterval = canMutateLoggingInterval;
      this.canGetNumberOfDataSamples = canGetNumberOfDataSamples;
      this.hasTemperatureSensor = hasTemperatureSensor;
      this.hasParticleCount = hasParticleCount;
      this.hasParticleConcentration = hasParticleConcentration;
      this.hasDeviceVersionInfo = hasDeviceVersionInfo;
      this.hasExtendedId = hasExtendedId;
      this.canEnterBootloaderMode = canEnterBootloaderMode;

      // build the field names
      dataSampleFieldNames = new ArrayList<String>();
      dataSampleFieldNames.add("sample_timestamp_utc_secs");
      dataSampleFieldNames.add("raw_particle_count");
      if (hasParticleCount)
         {
         dataSampleFieldNames.add("particle_count");
         }
      if (hasParticleConcentration)
         {
         dataSampleFieldNames.add("particle_concentration");
         }
      if (hasTemperatureSensor)
         {
         dataSampleFieldNames.add("temperature");
         }
      dataSampleFieldNames.add("humidity");
      }

   /**
    * Returns an unmodifiable {@link List} of included field names when using {@link DataSample#toCsv(ApiSupport)} or
    * {@link DataSample#toJsonArray(ApiSupport)}.
    */
   @NotNull
   public List<String> getDataSampleFieldNames()
      {
      return Collections.unmodifiableList(dataSampleFieldNames);
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

   public boolean hasParticleCount()
      {
      return hasParticleCount;
      }

   public boolean hasParticleConcentration()
      {
      return hasParticleConcentration;
      }

   public boolean hasDeviceVersionInfo()
      {
      return hasDeviceVersionInfo;
      }

   public boolean hasExtendedId()
      {
      return hasExtendedId;
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
      if (hasDeviceVersionInfo != that.hasDeviceVersionInfo)
         {
         return false;
         }
      if (hasParticleConcentration != that.hasParticleConcentration)
         {
         return false;
         }
      if (hasParticleCount != that.hasParticleCount)
         {
         return false;
         }
      if (hasExtendedId != that.hasExtendedId)
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
      result = 31 * result + (hasParticleCount ? 1 : 0);
      result = 31 * result + (hasParticleConcentration ? 1 : 0);
      result = 31 * result + (hasDeviceVersionInfo ? 1 : 0);
      result = 31 * result + (hasExtendedId ? 1 : 0);
      result = 31 * result + (canEnterBootloaderMode ? 1 : 0);
      return result;
      }
   }
