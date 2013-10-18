package org.specksensor.commands;

import edu.cmu.ri.createlab.usb.hid.CreateLabHIDCommandStrategy;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class EnterBootloaderModeCommandStrategy extends CreateLabHIDCommandStrategy
   {
   private static final byte COMMAND_PREFIX = 'B';

   /** The size of the expected response, in bytes */
   private static final int SIZE_IN_BYTES_OF_EXPECTED_RESPONSE = 0;

   @Override
   protected int getSizeOfExpectedResponse()
      {
      return SIZE_IN_BYTES_OF_EXPECTED_RESPONSE;
      }

   @Override
   protected byte[] getCommand()
      {
      return CommandStrategyHelper.createBaseCommand(COMMAND_PREFIX);
      }
   }
