package org.bodytrack.airbot;

import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>DataSampleUploadStatus</code> represents the various upload states a {@link Speck.DataSample} can be in, from the
 * perspective of the {@link DataSampleManager}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
enum DataSampleUploadStatus
   {
      NOT_ATTEMPTED("not_attempted"),
      IN_PROGRESS("in_progress"),
      SUCCESS("success"),
      FAILURE("failure");

   private final String name;

   private DataSampleUploadStatus(@NotNull final String name)
      {
      this.name = name;
      }

   @NotNull
   public String getName()
      {
      return name;
      }

   /**
    * Simply returns the name.
    *
    * @see #getName()
    */
   @Override
   public String toString()
      {
      return name;
      }
   }
