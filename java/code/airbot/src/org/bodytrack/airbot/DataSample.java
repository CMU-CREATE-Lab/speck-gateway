package org.bodytrack.airbot;

import java.nio.ByteBuffer;
import edu.cmu.ri.createlab.util.ByteUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataSample implements AirBot.DataSample
   {
   private static final String COMMA = ",";

   @Nullable
   private final Integer databaseId;

   private final int sampleTimeUtcSeconds;
   private final long downloadTime = System.currentTimeMillis();
   private final int particleCount;
   private final int temperature;
   private final int humidity;

   public DataSample(@NotNull final byte[] data)
      {
      this(null,
           ByteBuffer.wrap(data, 1, 4).getInt(),
           ByteBuffer.wrap(data, 5, 4).getInt(),
           ByteBuffer.wrap(data, 9, 2).getShort(),
           ByteUtils.unsignedByteToInt(data[11]));
      }

   public DataSample(@Nullable final Integer databaseId, final int sampleTimeUtcSeconds, final int particleCount, final int temperature, final int humidity)
      {
      this.databaseId = databaseId;
      this.sampleTimeUtcSeconds = sampleTimeUtcSeconds;
      this.particleCount = particleCount;
      this.temperature = temperature;
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
   public int getParticleCount()
      {
      return particleCount;
      }

   @Override
   public int getTemperature()
      {
      return temperature;
      }

   @Override
   public int getHumidity()
      {
      return humidity;
      }

   @Override
   public boolean isEmpty()
      {
      return sampleTimeUtcSeconds == 0 && particleCount == 0 && temperature == 0 && humidity == 0;
      }

   @NotNull
   @Override
   public String toCsv()
      {
      final StringBuilder s = new StringBuilder();
      s.append(sampleTimeUtcSeconds);
      s.append(COMMA);
      s.append(particleCount);
      s.append(COMMA);
      s.append(temperature);
      s.append(COMMA);
      s.append(humidity);
      s.append(COMMA);
      s.append(downloadTime);
      return s.toString();
      }

   @NotNull
   @Override
   public String toJsonArray()
      {
      final StringBuilder s = new StringBuilder("[");
      s.append(sampleTimeUtcSeconds);
      s.append(COMMA);
      s.append(particleCount);
      s.append(COMMA);
      s.append(temperature);
      s.append(COMMA);
      s.append(humidity);
      s.append("]");
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
      final StringBuilder sb = new StringBuilder("AirBotDataSample{");
      sb.append("id=").append(databaseId);
      sb.append(", sampleTime=").append(sampleTimeUtcSeconds);
      sb.append(", downloadTime=").append(downloadTime);
      sb.append(", particleCount=").append(particleCount);
      sb.append(", temperature=").append(temperature);
      sb.append(", humidity=").append(humidity);
      sb.append('}');
      return sb.toString();
      }

   /** Comparison is perform solely based on the {@link #getSampleTime() sample time}. */
   @Override
   public int compareTo(@NotNull final AirBot.DataSample dataSample)
      {
      if (this.sampleTimeUtcSeconds < dataSample.getSampleTime())
         {
         return -1;
         }
      if (this.sampleTimeUtcSeconds > dataSample.getSampleTime())
         {
         return -1;
         }

      return 0;
      }
   }
