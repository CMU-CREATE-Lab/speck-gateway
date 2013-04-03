package org.specksensor;

/**
 * <p>
 * <code>CommunicationException</code> is thrown to signify that a problem occurred while trying to communicate with the
 * device.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class CommunicationException extends Exception
   {
   public CommunicationException()
      {
      }

   public CommunicationException(final String s)
      {
      super(s);
      }

   public CommunicationException(final String s, final Throwable throwable)
      {
      super(s, throwable);
      }

   public CommunicationException(final Throwable throwable)
      {
      super(throwable);
      }
   }
