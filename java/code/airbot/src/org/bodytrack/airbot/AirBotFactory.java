package org.bodytrack.airbot;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import edu.cmu.ri.createlab.device.connectivity.ConnectionException;
import edu.cmu.ri.createlab.serial.SerialPortEnumerator;
import org.apache.log4j.Logger;
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
   private static final Logger LOG = Logger.getLogger(AirBotFactory.class);

   private static final String PATH_SEPARATOR = System.getProperty("path.separator", ":");

   /**
    * Creates the {@link AirBot} by repeatedly attempting to connect to all available serial ports and
    * connecting to the first AirBot it finds.  This method will repeatedly retry and will only return when either
    * a connection is established, or an unrecoverable failure is encountered in which case <code>null</code> is returned.
    */
   public static AirBot create()
      {
      return create((List<String>)null);
      }

   /**
    * <p>
    * Creates the {@link AirBot} by repeatedly attempting to connect to an AirBot on the given serial port(s).
    * </p>
    * <p>
    * Note that if one ore more serial ports is already specified as a system property (e.g. by using the -D command
    * line switch), then the serial port(s) specified in the argument to this constructor are appended to the port names
    * specified in the system property, and the system property value is updated.  If
    * <code>userDefinedSerialPortNames</code> is <code>null</code>, then this method behaves the same as
    * {@link #create()}.
    * </p>
    * <p>
    * This method will repeatedly retry and will only return when either a connection is established, or an
    * unrecoverable failure is encountered in which case <code>null</code> is returned.
    * </p>
    */
   public static AirBot create(@Nullable final List<String> userDefinedSerialPortNames) throws ConnectionException
      {
      if (userDefinedSerialPortNames != null && userDefinedSerialPortNames.size() > 0)
         {
         final Set<String> portNames = new HashSet<String>();

         // add all the ports currently defined in the system property to the set..
         final String currentPortNamesStr = System.getProperty(SerialPortEnumerator.SERIAL_PORTS_SYSTEM_PROPERTY_KEY, "").trim();
         if (currentPortNamesStr.length() > 0)
            {
            final String[] currentPortNames = currentPortNamesStr.split(PATH_SEPARATOR);
            Collections.addAll(portNames, currentPortNames);
            }

         // now add the new user-defined ones
         for (final String portName : userDefinedSerialPortNames)
            {
            portNames.add(portName);
            }

         // now that we have a set of all the existing ports and the new ones defined by the user, update the system property...
         if (portNames.size() > 0)
            {
            // first build the string value
            String[] portNameArray = new String[portNames.size()];
            portNameArray = portNames.toArray(portNameArray);
            final StringBuilder newSystemPropertyValue = new StringBuilder(portNameArray[0]);
            for (int i = 1; i < portNameArray.length; i++)
               {
               newSystemPropertyValue.append(PATH_SEPARATOR).append(portNameArray[i]);
               }

            // now set the system property
            System.setProperty(SerialPortEnumerator.SERIAL_PORTS_SYSTEM_PROPERTY_KEY, newSystemPropertyValue.toString());
            if (LOG.isDebugEnabled())
               {
               LOG.debug("AirBotFactory.create(): System property [" + SerialPortEnumerator.SERIAL_PORTS_SYSTEM_PROPERTY_KEY + "] now set to [" + newSystemPropertyValue + "]");
               }
            }
         }

      try
         {
         LOG.debug("Connecting to the AirBot...");
         return new AirBotConnectivityManager().connect();
         }
      catch (ConnectionException e)
         {
         LOG.error("ConnectionException while trying to connect to the AirBot", e);
         }

      return null;
      }

   /**
    * Tries to create a <code>AirBot</code> by connecting to an AirBot on the serial port specified by
    * the given <code>serialPortName</code>.  Returns <code>null</code> if the connection could not be established.
    *
    * @param serialPortName - the name of the serial port device which should be used to establish the connection
    *
    * @throws IllegalArgumentException if the <code>serialPortName</code> is <code>null</code>
    */
   @Nullable
   public static AirBot create(final String serialPortName)
      {
      return AirBotProxy.create(serialPortName);
      }

   private AirBotFactory()
      {
      // private to prevent instantiation
      }
   }
