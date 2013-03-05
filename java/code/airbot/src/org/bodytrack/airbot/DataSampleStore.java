package org.bodytrack.airbot;

import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>DataSampleStore</code> handles storage and retrieval of {@link AirBot.DataSample data samples}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
interface DataSampleStore
   {
   /**
    * Saves the given {@link AirBot.DataSample sample} and returns <code>true</code> upon success, <code>false</code>
    * otherwise.
    */
   boolean save(@NotNull AirBot.DataSample dataSample);

   /** Finds all samples which are in the uploading state, and resets them so that an upload will be retried. */
   void resetStateOfUploadingSamples();

   /** Perform any required shutdown tasks. */
   void shutdown();
   }