package org.bodytrack.airbot;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class MultiDestinationDataSampleStore implements DataSampleStore
   {
   private static final String STORE_NAME_CSV = "CSV";
   private static final String STORE_NAME_DATABASE = "Database";

   @NotNull
   private final Map<String, DataSampleStore> stores = new HashMap<String, DataSampleStore>(2);

   @NotNull
   private final DatabaseDataSampleStore databaseDataSampleStore;

   MultiDestinationDataSampleStore(@NotNull final AirBotConfig airBotConfig) throws InitializationException
      {
      databaseDataSampleStore = new DatabaseDataSampleStore(airBotConfig);
      stores.put(STORE_NAME_CSV, new CsvDataSampleStore(airBotConfig));
      stores.put(STORE_NAME_DATABASE, databaseDataSampleStore);
      }

   @Override
   public boolean save(@NotNull final AirBot.DataSample dataSample)
      {
      boolean wereAllSuccessful = true;
      for (final DataSampleStore store : stores.values())
         {
         if (!store.save(dataSample))
            {
            wereAllSuccessful = false;
            }
         }
      return wereAllSuccessful;
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
   public void markDataSamplesAsUploaded(@NotNull final DataSampleSet dataSampleSet)
      {
      databaseDataSampleStore.markDataSamplesAsUploaded(dataSampleSet);
      }

   @Override
   public void markDataSamplesAsFailed(@NotNull final DataSampleSet dataSampleSet)
      {
      databaseDataSampleStore.markDataSamplesAsFailed(dataSampleSet);
      }

   @Override
   public void shutdown()
      {
      for (final DataSampleStore store : stores.values())
         {
         store.shutdown();
         }
      }
   }
