package org.bodytrack.airbot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>TextFileDataSampleStore</code> handles storage and retrieval of {@link AirBot.DataSample data samples}, storing
 * them in a text file.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class TextFileDataSampleStore implements DataSampleStore
   {
   private static final Logger LOG = Logger.getLogger(TextFileDataSampleStore.class);

   @NotNull
   private BufferedWriter writer = null;

   public TextFileDataSampleStore(@NotNull final AirBotConfig airBotConfig)
      {
      final File dataFileDirectory = AirBotUploaderConstants.FilePaths.getDeviceDataDirectory(airBotConfig);
      final File dataFile = new File(dataFileDirectory, "dataSampleStore.txt");
      try
         {
         writer = new BufferedWriter(new FileWriter(dataFile, true));
         }
      catch (FileNotFoundException e)
         {
         LOG.error("FileNotFoundException while trying to create the BufferedWriter", e);
         System.exit(1);
         }
      catch (IOException e)
         {
         LOG.error("IOException while trying to create the BufferedWriter", e);
         System.exit(1);
         }
      }

   @Override
   public boolean save(@NotNull final AirBot.DataSample dataSample)
      {
      LOG.debug("TextFileDataSampleStore.save(): saving sample " + dataSample);
      try
         {
         writer.write(dataSample.toString());
         writer.newLine();
         writer.flush();
         }
      catch (IOException e)
         {
         LOG.error("IOException while trying to write to the file", e);
         }

      return true;
      }

   @Override
   public void resetStateOfUploadingSamples()
      {
      // TODO
      }
   }
