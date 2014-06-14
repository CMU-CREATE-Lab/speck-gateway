package org.specksensor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface SpeckConfig
   {
   int UNKNOWN_VERSION = -1;

   /** Returns the Speck's unique ID. */
   @NotNull
   String getId();

   /** Returns the Speck's protocol version. */
   int getProtocolVersion();

   /**
    * Returns the Speck's hardware version. Not supported in all Specks.  Returns {@link #UNKNOWN_VERSION} if
    * unsupported.
    */
   int getHardwareVersion();

   /**
    * Returns the Speck's firmware version. Not supported in all Specks.  Returns {@link #UNKNOWN_VERSION} if
    * unsupported.
    */
   int getFirmwareVersion();

   /**
    * Returns the number of seconds between data samples when the Speck is NOT connected to the Speck Gateway. The Speck
    * will always record data samples every second when connected to the Speck Gateway.  It's only when disconnected
    * that this logging interval is honored.
    */
   int getLoggingInterval();

   @NotNull
   ApiSupport getApiSupport();
   }
