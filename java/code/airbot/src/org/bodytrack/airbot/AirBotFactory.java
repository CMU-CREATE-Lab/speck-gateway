package org.bodytrack.airbot;

import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>AirBotFactory</code> provides methods for creating an {@link AirBot} instance.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class AirBotFactory
   {
   /**
    * Tries to create an <code>AirBot</code> by connecting to an HID AirBot.  Returns <code>null</code> if the
    * connection could not be established.
    */

   @Nullable
   public static AirBot create()
      {
      return AirBotProxy.create();
      }

   private AirBotFactory()
      {
      // private to prevent instantiation
      }
   }
