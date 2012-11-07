package org.bodytrack.airbot;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface AirBotConfig
   {
   /** Returns the AirBot's unique ID. */
   @NotNull
   String getId();

   /** Returns the AirBot's protocol version. */
   int getProtocolVersion();

   /** Returns the time at which the AirBot was powered in (in millis since the epoch). */
   long getPowerOnTimeInMillis();
   }
