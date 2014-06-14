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
    * Returns a JSON representation of this <code>DataSampleSet</code>.  Exactly which values get included depends on
    * the given {@link ApiSupport}.
    *
    * @see ApiSupport
    */
   @NotNull
   String toJson(@NotNull final ApiSupport apiSupport);
   }
