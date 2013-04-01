package org.bodytrack.airbot;

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
   private final SortedSet<AirBot.DataSample> dataSamples;

   /**
    * Creates a <code>DataSampleSet</code> from the given {@link SortedSet} of {@link AirBot.DataSample data samples}
    * by copying all the samples into a new collection.  If the given <code>dataSamples</code> is <code>null</code>,
    * this <code>DataSampleSet</code> simply has no data samples.
    */
   DataSampleSetImpl(@Nullable final SortedSet<AirBot.DataSample> dataSamples)
      {
      this.dataSamples = new TreeSet<AirBot.DataSample>();
      if (dataSamples != null)
         {
         this.dataSamples.addAll(dataSamples);
         }
      }

   /**
    * Returns an unmodifiable {@link SortedSet} of {@link AirBot.DataSample data samples}.
    */
   @NotNull
   @Override
   public SortedSet<AirBot.DataSample> getDataSamples()
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
   public String toJson()
      {
      final StringBuilder data = new StringBuilder("[");
      if (!isEmpty())
         {
         final List<String> dataSamplesAsJson = new ArrayList<String>(dataSamples.size());
         for (final AirBot.DataSample dataSample : dataSamples)
            {
            dataSamplesAsJson.add(dataSample.toJsonArray());
            }
         data.append(StringUtils.join(dataSamplesAsJson, ','));
         }
      data.append("]");
      return "{\"channel_names\":[\"raw_particles\",\"particles\",\"temperature\",\"humidity\"],\"data\":"+data+"}";
      }
   }
