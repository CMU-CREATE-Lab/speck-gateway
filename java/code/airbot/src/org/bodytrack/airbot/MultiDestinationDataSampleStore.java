package org.bodytrack.airbot;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class MultiDestinationDataSampleStore implements DataSampleStore
   {
   private final List<DataSampleStore> stores;

   MultiDestinationDataSampleStore(@NotNull final AirBotConfig airBotConfig) throws InitializationException
      {
      stores = new ArrayList<DataSampleStore>(2);
      stores.add(new CsvDataSampleStore(airBotConfig));
      stores.add(new DatabaseDataSampleStore(airBotConfig));
      }

   @Override
   public boolean save(@NotNull final AirBot.DataSample dataSample)
      {
      boolean wereAllSuccessful = true;
      for (final DataSampleStore store : stores)
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
      // TODO
      }

   @Override
   public void shutdown()
      {
      for (final DataSampleStore store : stores)
         {
         store.shutdown();
         }
      }
   }
