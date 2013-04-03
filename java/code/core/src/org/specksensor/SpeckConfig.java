package org.specksensor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface SpeckConfig
   {
   /** Returns the Speck's unique ID. */
   @NotNull
   String getId();

   /** Returns the Speck's protocol version. */
   int getProtocolVersion();
   }
