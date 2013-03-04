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
public final class RemoteStorageCredentialsValidator
   {
   private static final Logger LOG = Logger.getLogger(RemoteStorageCredentialsValidator.class);
   private static final Pattern DEVICE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

   /** Returns <code>true</code> if the given {@link RemoteStorageCredentials} are valid; <code>false</code> otherwise. */
   public static boolean isValid(@Nullable final RemoteStorageCredentials remoteStorageCredentials)
      {
      if (remoteStorageCredentials != null && isDeviceNameValid(remoteStorageCredentials.getDeviceName()))
         {
         try
            {
            // Send an empty JSON to test authenticaton
            final DataSampleUploadResponse dataSampleUploadResponse = DataSampleUploadHelper.upload(remoteStorageCredentials,
                                                                                                new StringEntity("{}"),
                                                                                                "n/a");
            return dataSampleUploadResponse.wasSuccessful();
            }
         catch (UnsupportedEncodingException e)
            {
            LOG.error("RemoteStorageCredentialsValidator.isValid(): UnsupportedEncodingException while trying to create the StringEntity", e);
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

   private RemoteStorageCredentialsValidator()
      {
      // private to prevent instantiation
      }
   }
