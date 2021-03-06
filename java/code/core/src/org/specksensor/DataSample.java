package org.specksensor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataSample implements Speck.DataSample
   {
   private static final String COMMA = ",";

   @Nullable
   private final Integer databaseId;

   private final int sampleTimeUtcSeconds;
   private final long downloadTime = System.currentTimeMillis();
   private final int rawParticleCount;
   private final int particleCountOrConcentration;
   private final int temperatureInTenthsOfDegreeF;
   private final int humidity;

   public DataSample(@Nullable final Integer databaseId,
                     final int sampleTimeUtcSeconds,
                     final int rawParticleCount,
                     final int particleCountOrConcentration,
                     final int temperatureInTenthsOfDegreeF,
                     final int humidity)
      {
      this.databaseId = databaseId;
      this.sampleTimeUtcSeconds = sampleTimeUtcSeconds;
      this.rawParticleCount = rawParticleCount;
      this.particleCountOrConcentration = particleCountOrConcentration;
      this.temperatureInTenthsOfDegreeF = temperatureInTenthsOfDegreeF;
      this.humidity = humidity;
      }

   @Override
   @Nullable
   public Integer getDatabaseId()
      {
      return databaseId;
      }

   @Override
   public int getSampleTime()
      {
      return sampleTimeUtcSeconds;
      }

   @Override
   public long getDownloadTime()
      {
      return downloadTime;
      }

   @Override
   public int getRawParticleCount()
      {
      return rawParticleCount;
      }

   @Override
   public int getParticleCountOrConcentration()
      {
      return particleCountOrConcentration;
      }

   @Override
   public double getParticleConcentration()
      {
      return getParticleCountOrConcentration() / 10.0;
      }

   @Override
   public double getTemperatureInDegreesF()
      {
      return temperatureInTenthsOfDegreeF / 10.0;
      }

   @Override
   public int getTemperatureInTenthsOfADegreeF()
      {
      return temperatureInTenthsOfDegreeF;
      }

   @Override
   public int getHumidity()
      {
      return humidity;
      }

   @Override
   public boolean isEmpty()
      {
      return sampleTimeUtcSeconds == 0 &&
             rawParticleCount == 0 &&
             particleCountOrConcentration == 0 &&
             temperatureInTenthsOfDegreeF == 0 &&
             humidity == 0;
      }

   @NotNull
   @Override
   public String toCsv(@NotNull final ApiSupport apiSupport)
      {
      return buildExportString(apiSupport);
      }

   @NotNull
   @Override
   public String toJsonArray(@NotNull final ApiSupport apiSupport)
      {
      return "[" + buildExportString(apiSupport) + "]";
      }

   @NotNull
   private String buildExportString(@NotNull final ApiSupport apiSupport)
      {
      final StringBuilder s = new StringBuilder();
      s.append(sampleTimeUtcSeconds);
      s.append(COMMA);
      s.append(rawParticleCount);
      if (apiSupport.hasParticleCount())
         {
         s.append(COMMA);
         s.append(particleCountOrConcentration);
         }
      if (apiSupport.hasParticleConcentration())
         {
         s.append(COMMA);
         s.append(getParticleConcentration());
         }
      if (apiSupport.hasTemperatureSensor())
         {
         s.append(COMMA);
         s.append(getTemperatureInDegreesF());
         }
      s.append(COMMA);
      s.append(humidity);
      return s.toString();
      }

   /** Equality is determined solely based on the {@link #getSampleTime() sample time}. */
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

      final DataSample that = (DataSample)o;

      if (sampleTimeUtcSeconds != that.sampleTimeUtcSeconds)
         {
         return false;
         }

      return true;
      }

   /** Hash code is determined solely based on the {@link #getSampleTime() sample time}. */
   @Override
   public int hashCode()
      {
      return sampleTimeUtcSeconds;
      }

   @Override
   public String toString()
      {
      final StringBuilder sb = new StringBuilder("DataSample{");
      sb.append("id=").append(databaseId);
      sb.append(", sampleTime=").append(sampleTimeUtcSeconds);
      sb.append(", downloadTime=").append(downloadTime);
      sb.append(", rawParticleCount=").append(rawParticleCount);
      sb.append(", particleCountOrConcentration=").append(particleCountOrConcentration);
      sb.append(", temperatureInTenthsOfDegreeF=").append(temperatureInTenthsOfDegreeF);
      sb.append(", humidity=").append(humidity);
      sb.append('}');
      return sb.toString();
      }

   /** Comparison is perform solely based on the {@link #getSampleTime() sample time}. */
   @Override
   public int compareTo(@NotNull final Speck.DataSample dataSample)
      {
      if (this.sampleTimeUtcSeconds < dataSample.getSampleTime())
         {
         return -1;
         }
      if (this.sampleTimeUtcSeconds > dataSample.getSampleTime())
         {
         return 1;
         }

      return 0;
      }
   }
