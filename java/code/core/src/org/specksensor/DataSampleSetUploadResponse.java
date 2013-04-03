package org.specksensor;

import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>DataSampleSetUploadResponse</code> contains the response details from the server after uploading a {@link DataSampleSet}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface DataSampleSetUploadResponse
   {
   interface Payload
      {
      @Nullable
      Integer getNumSuccessfulRecords();

      @Nullable
      Integer getNumFailedRecords();

      @Nullable
      String getFailureMessage();
      }

   boolean wasSuccessful();

   boolean hasPayload();

   @Nullable
   String getResult();

   @Nullable
   String getMessage();

   @Nullable
   Payload getPayload();
   }