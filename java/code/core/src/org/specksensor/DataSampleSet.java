package org.specksensor;

import java.util.SortedSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
interface DataSampleSet
   {
   int DEFAULT_SIZE = 500;

   @NotNull
   SortedSet<Speck.DataSample> getDataSamples();

   /** Returns <code>true</code> if this set contains no samples. */
   boolean isEmpty();

   /** Returns the number of samples in this set. */
   int size();

   /**
    * Returns a JSON representation of this <code>DataSampleSet</code>.   If <code>includeTemperature</code> is
    * <code>true</code>, the JSON will include temperature values.
    *
    * @see ApiSupport#hasTemperatureSensor()
    */
   @NotNull
   String toJson(final boolean includeTemperature);
   }
