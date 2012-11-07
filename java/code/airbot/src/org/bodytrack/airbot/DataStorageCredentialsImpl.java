package org.bodytrack.airbot;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DataStorageCredentialsImpl implements DataStorageCredentials
   {
   @NotNull
   private final String hostName;
   private final int hostPort;
   @NotNull
   private final String username;
   @NotNull
   private final String password;
   @NotNull
   private final String deviceName;

   public DataStorageCredentialsImpl(@NotNull final String hostName,
                                     final int hostPort,
                                     @NotNull final String username,
                                     @NotNull final String password,
                                     @NotNull final String deviceName)
      {
      this.hostName = hostName;
      this.hostPort = hostPort;
      this.username = username;
      this.password = password;
      this.deviceName = deviceName;
      }

   /** Returns the server name to which this device is configured to upload. */
   @NotNull
   @Override
   public String getHostName()
      {
      return hostName;
      }

   /** Returns the server port to which this device is configured to upload. */
   @Override
   public int getHostPort()
      {
      return hostPort;
      }

   /** Username for the user's account */
   @NotNull
   @Override
   public String getUsername()
      {
      return username;
      }

   /** Password for the user's account */
   @NotNull
   @Override
   public String getPassword()
      {
      return password;
      }

   /** Device name to which the data will be associated upon upload. */
   @NotNull
   @Override
   public String getDeviceName()
      {
      return deviceName;
      }

   @Override
   public boolean equals(final Object o)
      {
      if (this == o)
         {
         return true;
         }
      if (o == null || getClass() != o.getClass())
         {
         return false;
         }

      final DataStorageCredentialsImpl that = (DataStorageCredentialsImpl)o;

      if (hostPort != that.hostPort)
         {
         return false;
         }
      if (!deviceName.equals(that.deviceName))
         {
         return false;
         }
      if (!hostName.equals(that.hostName))
         {
         return false;
         }
      if (!password.equals(that.password))
         {
         return false;
         }
      if (!username.equals(that.username))
         {
         return false;
         }

      return true;
      }

   @Override
   public int hashCode()
      {
      int result = hostName.hashCode();
      result = 31 * result + hostPort;
      result = 31 * result + username.hashCode();
      result = 31 * result + password.hashCode();
      result = 31 * result + deviceName.hashCode();
      return result;
      }

   @Override
   public String toString()
      {
      final StringBuilder sb = new StringBuilder();
      sb.append("DataStorageCredentialsImpl");
      sb.append("{hostName='").append(hostName).append('\'');
      sb.append(", hostPort=").append(hostPort);
      sb.append(", username='").append(username).append('\'');
      sb.append(", password='").append(password).append('\'');
      sb.append(", deviceName='").append(deviceName).append('\'');
      sb.append('}');
      return sb.toString();
      }
   }
