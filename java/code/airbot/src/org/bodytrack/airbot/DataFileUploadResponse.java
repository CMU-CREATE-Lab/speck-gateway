package org.bodytrack.airbot;

import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>DataFileUploadResponse</code> contains the response details from the server after uploading a {@link DataFile}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
interface DataFileUploadResponse
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