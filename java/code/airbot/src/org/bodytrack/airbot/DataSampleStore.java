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
   enum SaveResult
      {
         SUCCESS("Success", true),
         FAILURE_ERROR("Error", false),
         FAILURE_DUPLICATE("Duplicate", false);

      private final String name;
      private final boolean wasSuccessfullySaved;

      private SaveResult(final String name, final boolean wasSuccessfullySaved)
         {
         this.name = name;
         this.wasSuccessfullySaved = wasSuccessfullySaved;
         }

      public String getName()
         {
         return name;
         }

      public boolean wasSuccessful()
         {
         return wasSuccessfullySaved;
         }
      }

   /** Saves the given {@link AirBot.DataSample sample} and returns the appropriate {@link SaveResult}. */
   @NotNull
   SaveResult save(@NotNull AirBot.DataSample dataSample);

   /** Finds all samples which are in the uploading state, and resets them so that an upload will be retried. */
   void resetStateOfUploadingSamples();

   /**
    * Returns a {@link DataSampleSet} containing up to <code>maxNumberRequested</code>
    * {@link AirBot.DataSample data samples}.  This method finds data samples which currently have an upload status of
    * {@link DataSampleUploadStatus#NOT_ATTEMPTED} and marks them as {@link DataSampleUploadStatus#IN_PROGRESS} before
    * returning.  Defaults to returning no more than {@link DataSampleSet#DEFAULT_SIZE} data samples if the
    * <code>maxNumberRequested</code> is non-positive.
    */
   @NotNull
   DataSampleSet getDataSamplesToUpload(final int maxNumberRequested);

   /**
    * Marks the samples in the given {@link DataSampleSet} as having been successfully uploaded
    * ({@link DataSampleUploadStatus#SUCCESS}).
    */
   void markDataSamplesAsUploaded(@NotNull final DataSampleSet dataSampleSet);

   /**
    * Marks the samples in the given {@link DataSampleSet} as having failed being uploaded
    * ({@link DataSampleUploadStatus#FAILURE}).
    */
   void markDataSamplesAsFailed(@NotNull final DataSampleSet dataSampleSet);

   /** Perform any required shutdown tasks. */
   void shutdown();
   }