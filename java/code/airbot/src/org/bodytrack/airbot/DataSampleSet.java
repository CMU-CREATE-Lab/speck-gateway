package org.bodytrack.airbot;

import java.util.SortedSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
interface DataSampleSet
   {
   int DEFAULT_SIZE = 200;

   @NotNull
   SortedSet<AirBot.DataSample> getDataSamples();

   /** Returns <code>true</code> if this set contains no samples. */
   boolean isEmpty();

   /** Returns the number of samples in this set. */
   int size();

   /** Returns a JSON representation of this <code>DataSampleSet</code> */
   @NotNull
   String toJson();
   }
