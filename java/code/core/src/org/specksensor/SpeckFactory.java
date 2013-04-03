package org.specksensor;

import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * <code>SpeckFactory</code> provides methods for creating a {@link Speck} instance.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class SpeckFactory
   {
   /**
    * Tries to create a {@link Speck} by connecting to a Speck.  Returns <code>null</code> if the connection could not
    * be established.
    */

   @Nullable
   public static Speck create()
      {
      return SpeckProxy.create();
      }

   private SpeckFactory()
      {
      // private to prevent instantiation
      }
   }
