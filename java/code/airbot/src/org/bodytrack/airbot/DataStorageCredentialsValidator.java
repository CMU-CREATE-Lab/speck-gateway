package org.bodytrack.airbot;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import edu.cmu.ri.createlab.util.net.HostAndPort;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataStorageCredentialsValidator
   {
   private static final Logger LOG = Logger.getLogger(DataStorageCredentialsValidator.class);
   private static final Pattern DEVICE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

   /** Returns <code>true</code> if the given {@link DataStorageCredentials} are valid; <code>false</code> otherwise. */
   public static boolean isValid(@Nullable final DataStorageCredentials dataStorageCredentials)
      {
      if (dataStorageCredentials != null && isDeviceNameValid(dataStorageCredentials.getDeviceName()))
         {
         try
            {
            // Send an empty JSON to test authenticaton
            final DataFileUploadResponse dataFileUploadResponse = DataFileUploadHelper.upload(dataStorageCredentials,
                                                                                              new StringEntity("{}"),
                                                                                              "n/a");
            return dataFileUploadResponse.wasSuccessful();
            }
         catch (UnsupportedEncodingException e)
            {
            LOG.error("UnsupportedEncodingException while trying to create the StringEntity", e);
            }
         }
      return false;
      }

   @Nullable
   public static HostAndPort extractHostAndPort(@Nullable final String hostNameAndPortStr)
      {
      if (hostNameAndPortStr != null)
         {
         return HostAndPort.createHostAndPort(hostNameAndPortStr);
         }
      return null;
      }

   public static boolean isHostAndPortValid(@Nullable final String hostNameAndPortStr)
      {
      return hostNameAndPortStr != null && (extractHostAndPort(hostNameAndPortStr) != null);
      }

   public static boolean isDeviceNameValid(@Nullable final String deviceName)
      {
      return deviceName != null && DEVICE_NAME_PATTERN.matcher(deviceName).matches();
      }

   private DataStorageCredentialsValidator()
      {
      // private to prevent instantiation
      }
   }
