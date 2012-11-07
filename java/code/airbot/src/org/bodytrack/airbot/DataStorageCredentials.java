package org.bodytrack.airbot;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface DataStorageCredentials
   {
   /** Returns the host name to which data will be uploaded for storage. */
   @NotNull
   String getHostName();

   /** Returns the port of the host to which data will be uploaded for storage. */
   int getHostPort();

   /** Username for the user's account */
   @NotNull
   String getUsername();

   /** Password for the user's account */
   @NotNull
   String getPassword();

   /** Device name to which the data will be associated upon upload. */
   @NotNull
   String getDeviceName();
   }
