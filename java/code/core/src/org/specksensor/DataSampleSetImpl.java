package org.specksensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class DataSampleSetImpl implements DataSampleSet
   {
   private final SortedSet<Speck.DataSample> dataSamples;

   /**
    * Creates a <code>DataSampleSet</code> from the given {@link SortedSet} of {@link Speck.DataSample data samples}
    * by copying all the samples into a new collection.  If the given <code>dataSamples</code> is <code>null</code>,
    * this <code>DataSampleSet</code> simply has no data samples.
    */
   DataSampleSetImpl(@Nullable final SortedSet<Speck.DataSample> dataSamples)
      {
      this.dataSamples = new TreeSet<Speck.DataSample>();
      if (dataSamples != null)
         {
         this.dataSamples.addAll(dataSamples);
         }
      }

   /**
    * Returns an unmodifiable {@link SortedSet} of {@link Speck.DataSample data samples}.
    */
   @NotNull
   @Override
   public SortedSet<Speck.DataSample> getDataSamples()
      {
      return Collections.unmodifiableSortedSet(dataSamples);
      }

   @Override
   public boolean isEmpty()
      {
      return dataSamples.isEmpty();
      }

   @Override
   public int size()
      {
      return dataSamples.size();
      }

   @NotNull
   @Override
   public String toJson(@NotNull final ApiSupport apiSupport)
      {
      final StringBuilder data = new StringBuilder("[");
      if (!isEmpty())
         {
         final List<String> dataSamplesAsJson = new ArrayList<String>(dataSamples.size());
         for (final Speck.DataSample dataSample : dataSamples)
            {
            dataSamplesAsJson.add(dataSample.toJsonArray(apiSupport));
            }
         data.append(StringUtils.join(dataSamplesAsJson, ','));
         }
      data.append("]");
      final StringBuilder channelNames = new StringBuilder("{\"channel_names\":[\"raw_particles\"");
      if (apiSupport.hasParticleCount())
         {
         channelNames.append(",\"particle_count\"");
         }
      if (apiSupport.hasParticleConcentration())
         {
         channelNames.append(",\"particle_concentration\"");
         }
      if (apiSupport.hasTemperatureSensor())
         {
         channelNames.append(",\"temperature\"");
         }
      channelNames.append(",\"humidity\"],\"data\":" + data + "}");
      return channelNames.toString();
      }
   }
