package org.specksensor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class MultiDestinationDataSampleStore implements DataSampleStore
   {
   @NotNull
   private final DataSampleStore csvDataSampleStore;
   private final DataSampleStore databaseDataSampleStore;

   MultiDestinationDataSampleStore(@NotNull final SpeckConfig speckConfig) throws InitializationException
      {
      databaseDataSampleStore = new DatabaseDataSampleStore(speckConfig);
      csvDataSampleStore = new CsvDataSampleStore(speckConfig);
      }

   @Override
   @NotNull
   public SaveResult save(@NotNull final Speck.DataSample dataSample)
      {
      final SaveResult databaseSaveResult = databaseDataSampleStore.save(dataSample);

      // don't write to the CSV if it was a duplicate
      if (!SaveResult.FAILURE_DUPLICATE.equals(databaseSaveResult))
         {
         return csvDataSampleStore.save(dataSample);
         }

      return databaseSaveResult;
      }

   @Override
   public void resetStateOfUploadingSamples()
      {
      databaseDataSampleStore.resetStateOfUploadingSamples();
      }

   @NotNull
   @Override
   public DataSampleSet getDataSamplesToUpload(final int maxNumberRequested)
      {
      return databaseDataSampleStore.getDataSamplesToUpload(maxNumberRequested);
      }

   @Override
   public void markDataSamplesAsUploaded(@NotNull final DataSampleSet dataSampleSet, final long uploadTimestampUtcMillis)
      {
      databaseDataSampleStore.markDataSamplesAsUploaded(dataSampleSet, uploadTimestampUtcMillis);
      }

   @Override
   public void markDataSamplesAsFailed(@NotNull final DataSampleSet dataSampleSet)
      {
      databaseDataSampleStore.markDataSamplesAsFailed(dataSampleSet);
      }

   @Override
   public void shutdown()
      {
      databaseDataSampleStore.shutdown();
      csvDataSampleStore.shutdown();
      }
   }
