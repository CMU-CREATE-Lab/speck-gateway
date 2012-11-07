package org.bodytrack.airbot;

import java.io.IOException;
import java.io.StringWriter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* @author Chris Bartley (bartley@cmu.edu)
*/
final class DataFileUploadHelper
   {
   private static final Logger LOG = Logger.getLogger(DataFileUploadHelper.class);
   
   /**
    * Determines the timeout in milliseconds until a connection is established. A timeout value of zero is interpreted
    * as an infinite timeout.
    */
   public static final int HTTP_TIMEOUT_IN_MILLIS = 5 * 60 * 1000; // 5 minutes

   /**
    * Defines the socket timeout in milliseconds, which is the timeout for waiting for data or, put differently, a
    * maximum period inactivity between two consecutive data packets). A timeout value of zero is interpreted as an
    * infinite timeout.
    */
   public static final int SOCKET_TIMEOUT_IN_MILLIS = 5 * 60 * 1000; // 5 minutes

   @NotNull
   static String getUploadUrl(@NotNull final DataStorageCredentials dataStorageCredentials)
      {
      return "http://" + dataStorageCredentials.getHostName() + ":" + dataStorageCredentials.getHostPort() + "/api/bodytrack/jupload?dev_nickname=" + dataStorageCredentials.getDeviceName();
      }

   @NotNull
   static DataFileUploadResponse upload(@NotNull final DataStorageCredentials dataStorageCredentials,
                                               @NotNull final HttpEntity entity,
                                               @NotNull final String entityName)
      {
      // set timeouts
      final HttpParams httpParams = new BasicHttpParams();

      // This parameter expects a value of type java.lang.Integer. If this parameter is not set, connect operations
      // will not time out (infinite timeout).
      httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HTTP_TIMEOUT_IN_MILLIS);

      // This parameter expects a value of type java.lang.Integer. If this parameter is not set, read operations will
      // not time out (infinite timeout).
      httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT_IN_MILLIS);

      final HttpHost targetHost = new HttpHost(dataStorageCredentials.getHostName(), dataStorageCredentials.getHostPort(), "http");

      final DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

      @NotNull DataFileUploadResponse dataFileUploadResponse;
      try
         {
         // Set up basic auth (got this code from http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientPreemptiveBasicAuthentication.java)
         httpClient.getCredentialsProvider().setCredentials(
               new AuthScope(targetHost.getHostName(), targetHost.getPort()),
               new UsernamePasswordCredentials(dataStorageCredentials.getUsername(), dataStorageCredentials.getPassword()));

         // Create AuthCache instance
         final AuthCache authCache = new BasicAuthCache();

         // Generate BASIC scheme object and add it to the local auth cache
         final BasicScheme basicAuth = new BasicScheme();
         authCache.put(targetHost, basicAuth);

         // Add AuthCache to the execution context
         final BasicHttpContext localContext = new BasicHttpContext();
         localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

         final HttpPost httpPost = new HttpPost(getUploadUrl(dataStorageCredentials));
         httpPost.setEntity(entity);

         final HttpResponse response = httpClient.execute(targetHost, httpPost, localContext);
         final HttpEntity responseEntity = response.getEntity();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("DataFileUploadHelper.upload(): response status [" + response.getStatusLine() + "]");
            }

         if (HttpStatus.SC_UNAUTHORIZED == response.getStatusLine().getStatusCode())
            {
            final String message = "Authorization Failed (HTTP " + HttpStatus.SC_UNAUTHORIZED + ")";
            LOG.error("DataFileUploadHelper.upload(): " + message);
            dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
            }
         else
            {
            if (responseEntity == null)
               {
               final String message = "HTTP entity response is null";
               LOG.error("DataFileUploadHelper.upload(): " + message);
               dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
               }
            else
               {
               try
                  {
                  // read the response into a String
                  final StringWriter writer = new StringWriter();
                  IOUtils.copy(responseEntity.getContent(), writer, "UTF-8");
                  final String responseStr = writer.toString();

                  // Skip everything before the first curly brace
                  String json = null;
                  try
                     {
                     final int openBracePosition = responseStr.indexOf("{");
                     if (openBracePosition >= 0)
                        {
                        json = responseStr.substring(openBracePosition);
                        }
                     else
                        {
                        LOG.error("DataFileUploadHelper.upload(): Error while parsing the JSON response: open brace not found");
                        }
                     }
                  catch (Exception e)
                     {
                     LOG.error("DataFileUploadHelper.upload(): Exception while parsing the JSON response", e);
                     }

                  if (LOG.isDebugEnabled())
                     {
                     LOG.debug("DataFileUploadHelper.upload(): response [" + json + "]");
                     }

                  if (json == null)
                     {
                     final String message = "Response not recognized as JSON (no opening curly brace)";
                     LOG.error("DataFileUploadHelper.upload(): " + message);
                     dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
                     }
                  else
                     {
                     // now parse the response, converting the JSON into a DataFileUploadResponse
                     try
                        {
                        final ObjectMapper mapper = new ObjectMapper();
                        dataFileUploadResponse = mapper.readValue(json, DataFileUploadResponseImpl.class);
                        }
                     catch (IOException e)
                        {
                        final String message = "IOException while trying to parse the response as JSON";
                        LOG.error("DataFileUploadHelper.upload(): " + message, e);
                        dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
                        }
                     }
                  }
               catch (IOException e)
                  {
                  final String message = "IOException while reading or parsing the response";
                  LOG.error("DataFileUploadHelper.upload(): " + message, e);
                  dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
                  }
               catch (IllegalStateException e)
                  {
                  final String message = "IllegalStateException while reading the response";
                  LOG.error("DataFileUploadHelper.upload(): " + message, e);
                  dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
                  }
               catch (Exception e)
                  {
                  final String message = "Exception while reading the response";
                  LOG.error("DataFileUploadHelper.upload(): " + message, e);
                  dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
                  }
               }
            }

         EntityUtils.consume(responseEntity);
         }
      catch (ClientProtocolException e)
         {
         final String message = "ClientProtocolException while trying to upload data file [" + entityName + "]";
         LOG.error("DataFileUploadHelper.upload(): " + message, e);
         dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
         }
      catch (IOException e)
         {
         final String message = "IOException while trying to upload data file [" + entityName + "]";
         LOG.error("DataFileUploadHelper.upload(): " + message, e);
         dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
         }
      catch (Exception e)
         {
         final String message = "Exception while trying to upload data file [" + entityName + "]";
         LOG.error("DataFileUploadHelper.upload(): " + message, e);
         dataFileUploadResponse = DataFileUploadResponseImpl.createFailedResponse(message);
         }
      finally
         {
         // When the HttpClient instance is no longer needed, shut down the connection manager to ensure immediate
         // deallocation of all system resources
         httpClient.getConnectionManager().shutdown();
         }

      return dataFileUploadResponse;
      }

   private DataFileUploadHelper()
      {
      // private to prevent instantiation
      }
   
   private static class PayloadImpl implements DataFileUploadResponse.Payload
      {
      private Integer numSuccessfulRecords;
      private Integer numFailedRecords;
      private String failureMessage;

      @Nullable
      @Override
      @JsonProperty("successful_records")
      public Integer getNumSuccessfulRecords()
         {
         return numSuccessfulRecords;
         }

      @JsonProperty("successful_records")
      private void setNumSuccessfulRecords(@Nullable final Integer numSuccessfulRecords)
         {
         this.numSuccessfulRecords = numSuccessfulRecords;
         }

      @Nullable
      @Override
      @JsonProperty("failed_records")
      public Integer getNumFailedRecords()
         {
         return numFailedRecords;
         }

      @JsonProperty("failed_records")
      private void setNumFailedRecords(@Nullable final Integer numFailedRecords)
         {
         this.numFailedRecords = numFailedRecords;
         }

      @Nullable
      @Override
      @JsonProperty("failure")
      public String getFailureMessage()
         {
         return failureMessage;
         }

      @JsonProperty("failure")
      private void setFailureMessage(@Nullable final String failureMessage)
         {
         this.failureMessage = failureMessage;
         }
      }

   private static class DataFileUploadResponseImpl implements DataFileUploadResponse
      {

      private static final String RESULT_SUCCESS = "OK";
      private static final String RESULT_FAILURE = "KO";

      @NotNull
      private static DataFileUploadResponse createFailedResponse(@NotNull final String message)
         {
         return new DataFileUploadResponseImpl(RESULT_FAILURE, message);
         }

      private String result;
      private String message;
      private PayloadImpl payload;

      @SuppressWarnings("UnusedDeclaration")
      private DataFileUploadResponseImpl()
         {
         }

      private DataFileUploadResponseImpl(final String result, final String message)
         {
         this.result = result;
         this.message = message;
         }

      @Override
      public boolean wasSuccessful()
         {
         return RESULT_SUCCESS.equals(result);
         }

      @Override
      public boolean hasPayload()
         {
         return payload != null;
         }

      @Nullable
      @JsonProperty("result")
      public String getResult()
         {
         return result;
         }

      @JsonProperty("result")
      private void setResult(@Nullable final String result)
         {
         this.result = result;
         }

      @Nullable
      @JsonProperty("message")
      public String getMessage()
         {
         return message;
         }

      @JsonProperty("message")
      private void setMessage(@Nullable final String message)
         {
         this.message = message;
         }

      @Nullable
      @JsonProperty("payload")
      public Payload getPayload()
         {
         return payload;
         }

      @JsonProperty("payload")
      private void setPayload(@Nullable final PayloadImpl payload)
         {
         this.payload = payload;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("DataFileUploadResponseImpl");
         sb.append("{result='").append(result).append('\'');
         sb.append(", message='").append(message).append('\'');
         sb.append(", payload=").append(payload);
         sb.append('}');
         return sb.toString();
         }
      }
   }
