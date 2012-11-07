package org.bodytrack.airbot.commands;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.cmu.ri.createlab.serial.CreateLabSerialDeviceVariableLengthReturnValueCommandStrategy;
import edu.cmu.ri.createlab.serial.SerialDeviceCommandResponse;
import org.apache.log4j.Logger;
import org.bodytrack.airbot.AirBotConfig;
import org.bodytrack.airbot.AirBotUtils;
import org.bodytrack.airbot.DataFile;
import org.bodytrack.util.json.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class GetFileCommandStrategy extends CreateLabSerialDeviceVariableLengthReturnValueCommandStrategy<DataFile>
   {
   private static final Logger LOG = Logger.getLogger(GetFileCommandStrategy.class);

   public static final int READ_TIMEOUT = 15;
   public static final TimeUnit READ_TIMEOUT_UNITS = TimeUnit.MINUTES;

   /** The command character used to send the download file command. */
   private static final byte COMMAND_PREFIX = 'D';

   /** The size of the expected response, in bytes */
   private static final int SIZE_IN_BYTES_OF_EXPECTED_RESPONSE_HEADER = 4;

   @NotNull
   private final byte[] command;

   @NotNull
   private final String filename;

   @NotNull
   private final AirBotConfig airBotConfig;

   public GetFileCommandStrategy(@NotNull final String filename, @NotNull final AirBotConfig airBotConfig)
      {
      super(READ_TIMEOUT, READ_TIMEOUT_UNITS);
      this.filename = filename.toUpperCase();   // it uses all upper case for filenames
      this.airBotConfig = airBotConfig;

      final int lengthOfFilenameAndCRLF = this.filename.length() + 2;

      // The command consists of the command prefix, one byte containing the length of the filename plus the CRLF, then
      // the filename followed by a CRLF.
      command = new byte[1 + 1 + lengthOfFilenameAndCRLF];

      // build the command
      command[0] = COMMAND_PREFIX;
      command[1] = (byte)lengthOfFilenameAndCRLF;
      for (int i = 0; i < this.filename.length(); i++)
         {
         command[2 + i] = (byte)this.filename.charAt(i);
         }
      command[command.length - 2] = '\r';
      command[command.length - 1] = '\n';
      }

   protected byte[] getCommand()
      {
      return command.clone();
      }

   @Override
   protected int getSizeOfExpectedResponseHeader()
      {
      return SIZE_IN_BYTES_OF_EXPECTED_RESPONSE_HEADER;
      }

   @Override
   protected int getSizeOfVariableLengthResponse(final byte[] header)
      {
      // convert 4 bytes into an int
      final int size = ByteBuffer.wrap(header, 0, SIZE_IN_BYTES_OF_EXPECTED_RESPONSE_HEADER).getInt();

      if (LOG.isDebugEnabled())
         {
         LOG.debug("GetFileCommandStrategy.getSizeOfVariableLengthResponse(): size [" + size + "] extracted from header");
         }

      return size;
      }

   /**
    * Returns a {@link DataFile} (which might be {@link DataFile#isEmpty() empty}), or <code>null</code> if the command
    * failed.
    */
   @Nullable
   @Override
   public DataFile convertResponse(@Nullable final SerialDeviceCommandResponse response)
      {
      if (response != null && response.wasSuccessful())
         {
         // get the data
         final byte[] responseData = response.getData();

         // get the length of the file
         final int lengthOfFile = getSizeOfVariableLengthResponse(responseData);
         if (lengthOfFile == 0)
            {
            LOG.info("GetFileCommandStrategy.convertResponse(): No data available, returning empty DataFile.");
            return new DataFileImpl();
            }
         else
            {
            if (LOG.isTraceEnabled())
               {
               LOG.trace("GetFileCommandStrategy.convertResponse(): length of file [" + lengthOfFile + "]");
               }

            try
               {
               final DataFileImpl dataFile = new DataFileImpl(filename, responseData, SIZE_IN_BYTES_OF_EXPECTED_RESPONSE_HEADER, lengthOfFile, airBotConfig);

               if (LOG.isDebugEnabled())
                  {
                  LOG.debug("GetFileCommandStrategy.convertResponse(): file download succeeded [" + dataFile + "]");
                  }
               return dataFile;
               }
            catch (IllegalArgumentException ignored)
               {
               LOG.error("GetFileCommandStrategy.convertResponse(): IllegalArgumentException while trying to construct the DataFileImpl");
               }
            catch (Exception e)
               {
               LOG.error("GetFileCommandStrategy.convertResponse(): Exception while trying to construct the DataFileImpl", e);
               }
            }
         }
      return null;
      }

   private static final class DataFileImpl implements DataFile
      {
      private static final String EMPTY_FILENAME = "";
      private static final byte[] EMPTY_DATA = new byte[0];
      private static final int EMPTY_OFFSET = 0;
      private static final long EMPTY_TIMESTAMP = 0;
      private static final int EMPTY_LENGTH = 0;
      private static final String CHANNEL_NAME_HUMIDITY = "humidity";
      private static final String CHANNEL_NAME_TEMPERATURE = "temperature";

      private final boolean isEmpty;
      private final String baseFilename;
      private final String filename;
      private final byte[] data;
      private final int offset;
      private final int fileLength;
      private final long timestampInMillis;
      private final boolean isChecksumCorrect;

      private DataFileImpl()
         {
         this.filename = EMPTY_FILENAME;
         this.data = EMPTY_DATA;
         this.offset = EMPTY_OFFSET;
         this.fileLength = EMPTY_LENGTH;
         this.isEmpty = true;
         this.timestampInMillis = EMPTY_TIMESTAMP;
         this.baseFilename = filename;
         this.isChecksumCorrect = false;
         }

      private DataFileImpl(@NotNull final String filenameOnDevice,
                           @NotNull final byte[] data,
                           final int offset,
                           final int fileLength,
                           @NotNull final AirBotConfig airBotConfig) throws IllegalArgumentException
         {
         this.isEmpty = (EMPTY_FILENAME.equals(filenameOnDevice) && Arrays.equals(EMPTY_DATA, data) && offset == EMPTY_OFFSET && fileLength == EMPTY_LENGTH);

         // check whether we're trying to create an empty file
         if (this.isEmpty)
            {
            this.data = EMPTY_DATA;
            this.filename = EMPTY_FILENAME;
            this.offset = EMPTY_OFFSET;
            this.fileLength = EMPTY_LENGTH;
            this.timestampInMillis = EMPTY_TIMESTAMP;
            this.baseFilename = EMPTY_FILENAME;
            this.isChecksumCorrect = false;
            }
         else
            {
            if (!AirBotUtils.isFilenameValid(filenameOnDevice, airBotConfig))
               {
               final String message = "Invalid filename: filename [" + filenameOnDevice + "] does not match the expected pattern";
               LOG.error("GetFileCommandStrategy$DataFileImpl.DataFileImpl(): " + message);
               throw new IllegalArgumentException(message);
               }

            this.filename = AirBotUtils.mapFilename(filenameOnDevice, airBotConfig);

            // set the base filename
            this.baseFilename = filename.substring(0, filename.length() - AirBotUtils.FILENAME_EXTENSION.length());

            // the filename is also the timestamp, in hex
            final long seconds = Integer.parseInt(this.baseFilename, 16);
            timestampInMillis = seconds * 1000;

            if (airBotConfig.getProtocolVersion() == 1)
               {
               byte[] tempData = data;
               int tempOffset = offset;
               int tempFileLength = fileLength;

               // We now need to modify all the times, humidity, and temperatures in the JSON, changing times from
               // relative to actual and dividing the humidity and temperatures by 10.  Do so by first inflating the
               // JSON into an AirBotDataFile.  If that succeeds, then we consider the checksum to be valid.
               boolean isValid = false;
               try
                  {
                  // get an InputStream for the data, starting at offset so that we skip the 4 bytes which specify the
                  // file length
                  final ByteArrayInputStream jsonInputStream = new ByteArrayInputStream(tempData, offset, fileLength);

                  // Try to convert the data into an AirBotDataFile
                  final AirBotDataFile airBotDataFile = JsonUtils.fromJson(jsonInputStream, AirBotDataFileImpl.class);

                  // if successful, then process all the records and convert the relative times to absolute
                  if (airBotDataFile != null)
                     {
                     isValid = true;

                     // make the required modifications to the data for protocol version 1
                     final List<String> channelNames = airBotDataFile.getChannelNames();
                     final List<List<Double>> dataRecords = airBotDataFile.getData();
                     if (channelNames != null && dataRecords != null)
                        {
                        // Start by figuring out the field indeces for temp and humidity
                        int tempIndex = -1;
                        int humidityIndex = -1;
                        int index = 1; // start at one so we skip the time
                        for (final String channelName : channelNames)
                           {
                           if (CHANNEL_NAME_TEMPERATURE.equals(channelName))
                              {
                              tempIndex = index;
                              }
                           else if (CHANNEL_NAME_HUMIDITY.equals(channelName))
                              {
                              humidityIndex = index;
                              }
                           index++;
                           }

                        // Now run through all the data records and fix the timestamps, temp, and humidity
                        for (final List<Double> dataRecord : dataRecords)
                           {
                           // fix the timestamp
                           final Double relativeTimeInSeconds = dataRecord.get(0);
                           if (relativeTimeInSeconds != null)
                              {
                              final long absoluteTimeInSeconds = AirBotUtils.convertRelativeSecondsToAbsolute((long)relativeTimeInSeconds.doubleValue(), airBotConfig);
                              dataRecord.set(0, (double)absoluteTimeInSeconds);
                              }

                           // fix the temperature
                           if (tempIndex > 0)
                              {
                              final Double value = dataRecord.get(tempIndex);
                              if (value != null)
                                 {
                                 dataRecord.set(tempIndex, value / 10);
                                 }
                              }

                           // fix the humidity
                           if (humidityIndex > 0)
                              {
                              final Double value = dataRecord.get(humidityIndex);
                              if (value != null)
                                 {
                                 dataRecord.set(humidityIndex, value / 10);
                                 }
                              }
                           }
                        }

                     // now deflate back to JSON bytes
                     tempData = JsonUtils.toJson(airBotDataFile);
                     tempOffset = 0;
                     tempFileLength = (tempData == null) ? 0 : tempData.length;
                     }
                  }
               catch (IOException e)
                  {
                  LOG.error("IOException while parsing the JSON into an AirBotDataFile.", e);
                  }

               this.data = tempData;
               this.offset = tempOffset;
               this.fileLength = tempFileLength;
               isChecksumCorrect = isValid;
               }
            else
               {
               this.data = data;
               this.offset = offset;
               this.fileLength = fileLength;

               // simply do a JSON validation as a checksum
               isChecksumCorrect = JsonUtils.isValid(new String(this.data));
               }

            if (LOG.isDebugEnabled())
               {
               if (!isChecksumCorrect)
                  {
                  LOG.debug("GetFileCommandStrategy.convertResponse(): checksum verification failed.");
                  }
               }
            }
         }

      @Override
      public boolean isEmpty()
         {
         return isEmpty;
         }

      @NotNull
      @Override
      public String getBaseFilename()
         {
         return baseFilename;
         }

      @Override
      @NotNull
      public String getFilename()
         {
         return filename;
         }

      @Override
      @NotNull
      public Date getTimestamp()
         {
         return new Date(timestampInMillis);
         }

      @Override
      public void writeToOutputStream(@Nullable final DataOutputStream outputStream) throws IOException
         {
         if (outputStream != null)
            {
            outputStream.write(data, offset, fileLength);
            }
         }

      @Override
      public int getLength()
         {
         return fileLength;
         }

      public boolean isChecksumCorrect()
         {
         return isChecksumCorrect;
         }

      @Override
      public boolean equals(final Object o)
         {
         if (this == o)
            {
            return true;
            }
         if (o == null || getClass() != o.getClass())
            {
            return false;
            }

         final DataFileImpl dataFile = (DataFileImpl)o;

         if (fileLength != dataFile.fileLength)
            {
            return false;
            }
         if (isChecksumCorrect != dataFile.isChecksumCorrect)
            {
            return false;
            }
         if (isEmpty != dataFile.isEmpty)
            {
            return false;
            }
         if (offset != dataFile.offset)
            {
            return false;
            }
         if (timestampInMillis != dataFile.timestampInMillis)
            {
            return false;
            }
         if (baseFilename != null ? !baseFilename.equals(dataFile.baseFilename) : dataFile.baseFilename != null)
            {
            return false;
            }
         if (!Arrays.equals(data, dataFile.data))
            {
            return false;
            }
         if (filename != null ? !filename.equals(dataFile.filename) : dataFile.filename != null)
            {
            return false;
            }

         return true;
         }

      @Override
      public int hashCode()
         {
         int result = (isEmpty ? 1 : 0);
         result = 31 * result + (baseFilename != null ? baseFilename.hashCode() : 0);
         result = 31 * result + (filename != null ? filename.hashCode() : 0);
         result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
         result = 31 * result + offset;
         result = 31 * result + fileLength;
         result = 31 * result + (int)(timestampInMillis ^ (timestampInMillis >>> 32));
         result = 31 * result + (isChecksumCorrect ? 1 : 0);
         return result;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("DataFile");
         if (isEmpty)
            {
            sb.append("{isEmpty=").append(isEmpty).append('}');
            }
         else
            {
            sb.append("{baseFilename='").append(baseFilename).append('\'');
            sb.append(", filename='").append(filename).append('\'');
            sb.append(", length=").append(fileLength);
            sb.append(", timestampInMillis=").append(timestampInMillis);
            sb.append(", checksumCorrect=").append(isChecksumCorrect);
            sb.append('}');
            }
         return sb.toString();
         }
      }

   interface AirBotDataFile
      {
      @Nullable
      @JsonProperty("channel_names")
      List<String> getChannelNames();

      @Nullable
      @JsonProperty("data")
      List<List<Double>> getData();
      }

   @SuppressWarnings("ReturnOfCollectionOrArrayField")
   private static final class AirBotDataFileImpl implements AirBotDataFile
      {
      private List<String> channelNames = null;
      private List<List<Double>> data = null;

      @Override
      @Nullable
      @JsonProperty("channel_names")
      public List<String> getChannelNames()
         {
         return channelNames;
         }

      @JsonProperty("channel_names")
      public void setChannelNames(@Nullable final List<String> channelNames)
         {
         this.channelNames = new ArrayList<String>(channelNames);
         }

      @Override
      @Nullable
      @JsonProperty("data")
      public List<List<Double>> getData()
         {
         return data;
         }

      @JsonProperty("data")
      public void setData(@Nullable final List<List<Double>> data)
         {
         if (data == null)
            {
            this.data = null;
            }
         else
            {
            this.data = new ArrayList<List<Double>>(data.size());
            for (final List<Double> dataRecord : data)
               {
               if (dataRecord == null)
                  {
                  this.data.add(null);
                  }
               else
                  {
                  this.data.add(new ArrayList<Double>(dataRecord));
                  }
               }
            }
         }
      }
   }