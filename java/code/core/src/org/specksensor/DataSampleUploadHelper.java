package org.specksensor;

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
public final class DataSampleUploadHelper
   {
   private static final Logger LOG = Logger.getLogger(DataSampleUploadHelper.class);

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
   public static String getUploadUrl(@NotNull final RemoteStorageCredentials remoteStorageCredentials)
      {
      return "http://" + remoteStorageCredentials.getHostName() + ":" + remoteStorageCredentials.getHostPort() + "/api/bodytrack/jupload?dev_nickname=" + remoteStorageCredentials.getDeviceName();
      }

   @NotNull
   public static DataSampleSetUploadResponse upload(@NotNull final RemoteStorageCredentials remoteStorageCredentials,
                                                    @NotNull final HttpEntity entity)
      {
      // set timeouts
      final HttpParams httpParams = new BasicHttpParams();

      // This parameter expects a value of type java.lang.Integer. If this parameter is not set, connect operations
      // will not time out (infinite timeout).
      httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HTTP_TIMEOUT_IN_MILLIS);

      // This parameter expects a value of type java.lang.Integer. If this parameter is not set, read operations will
      // not time out (infinite timeout).
      httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT_IN_MILLIS);

      final HttpHost targetHost = new HttpHost(remoteStorageCredentials.getHostName(), remoteStorageCredentials.getHostPort(), "http");

      final DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

      @NotNull DataSampleSetUploadResponse dataSampleSetUploadResponse;
      try
         {
         // Set up basic auth (got this code from http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientPreemptiveBasicAuthentication.java)
         httpClient.getCredentialsProvider().setCredentials(
               new AuthScope(targetHost.getHostName(), targetHost.getPort()),
               new UsernamePasswordCredentials(remoteStorageCredentials.getUsername(), remoteStorageCredentials.getPassword()));

         // Create AuthCache instance
         final AuthCache authCache = new BasicAuthCache();

         // Generate BASIC scheme object and add it to the local auth cache
         final BasicScheme basicAuth = new BasicScheme();
         authCache.put(targetHost, basicAuth);

         // Add AuthCache to the execution context
         final BasicHttpContext localContext = new BasicHttpContext();
         localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);

         final HttpPost httpPost = new HttpPost(getUploadUrl(remoteStorageCredentials));
         httpPost.setEntity(entity);

         final HttpResponse response = httpClient.execute(targetHost, httpPost, localContext);
         final HttpEntity responseEntity = response.getEntity();
         if (LOG.isDebugEnabled())
            {
            LOG.debug("DataSampleUploadHelper.upload(): response status [" + response.getStatusLine() + "]");
            }

         if (HttpStatus.SC_UNAUTHORIZED == response.getStatusLine().getStatusCode())
            {
            final String message = "Authorization Failed (HTTP " + HttpStatus.SC_UNAUTHORIZED + ")";
            LOG.error("DataSampleUploadHelper.upload(): " + message);
            dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
            }
         else
            {
            if (responseEntity == null)
               {
               final String message = "HTTP entity response is null";
               LOG.error("DataSampleUploadHelper.upload(): " + message);
               dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
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
                        LOG.error("DataSampleUploadHelper.upload(): Error while parsing the JSON response: open brace not found");
                        }
                     }
                  catch (Exception e)
                     {
                     LOG.error("DataSampleUploadHelper.upload(): Exception while parsing the JSON response", e);
                     }

                  if (LOG.isDebugEnabled())
                     {
                     LOG.debug("DataSampleUploadHelper.upload(): response [" + json + "]");
                     }

                  if (json == null)
                     {
                     final String message = "Response not recognized as JSON (no opening curly brace)";
                     LOG.error("DataSampleUploadHelper.upload(): " + message);
                     dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
                     }
                  else
                     {
                     // now parse the response, converting the JSON into a DataSampleSetUploadResponse
                     try
                        {
                        final ObjectMapper mapper = new ObjectMapper();
                        dataSampleSetUploadResponse = mapper.readValue(json, DataSampleSetUploadResponseImpl.class);
                        }
                     catch (IOException e)
                        {
                        final String message = "IOException while trying to parse the response as JSON";
                        LOG.error("DataSampleUploadHelper.upload(): " + message, e);
                        dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
                        }
                     }
                  }
               catch (IOException e)
                  {
                  final String message = "IOException while reading or parsing the response";
                  LOG.error("DataSampleUploadHelper.upload(): " + message, e);
                  dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
                  }
               catch (IllegalStateException e)
                  {
                  final String message = "IllegalStateException while reading the response";
                  LOG.error("DataSampleUploadHelper.upload(): " + message, e);
                  dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
                  }
               catch (Exception e)
                  {
                  final String message = "Exception while reading the response";
                  LOG.error("DataSampleUploadHelper.upload(): " + message, e);
                  dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
                  }
               }
            }

         EntityUtils.consume(responseEntity);
         }
      catch (ClientProtocolException e)
         {
         final String message = "ClientProtocolException while trying to upload";
         LOG.error("DataSampleUploadHelper.upload(): " + message, e);
         dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
         }
      catch (IOException e)
         {
         final String message = "IOException while trying to upload";
         LOG.error("DataSampleUploadHelper.upload(): " + message, e);
         dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
         }
      catch (Exception e)
         {
         final String message = "Exception while trying to upload";
         LOG.error("DataSampleUploadHelper.upload(): " + message, e);
         dataSampleSetUploadResponse = DataSampleSetUploadResponseImpl.createFailedResponse(message);
         }
      finally
         {
         // When the HttpClient instance is no longer needed, shut down the connection manager to ensure immediate
         // deallocation of all system resources
         httpClient.getConnectionManager().shutdown();
         }

      return dataSampleSetUploadResponse;
      }

   private DataSampleUploadHelper()
      {
      // private to prevent instantiation
      }

   private static class PayloadImpl implements DataSampleSetUploadResponse.Payload
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

   private static class DataSampleSetUploadResponseImpl implements DataSampleSetUploadResponse
      {
      private static final String RESULT_SUCCESS = "OK";
      private static final String RESULT_FAILURE = "KO";

      @NotNull
      private static DataSampleSetUploadResponse createFailedResponse(@NotNull final String message)
         {
         return new DataSampleSetUploadResponseImpl(RESULT_FAILURE, message);
         }

      private String result;
      private String message;
      private PayloadImpl payload;
      private final long timestampUtcMillis = System.currentTimeMillis();

      @SuppressWarnings("UnusedDeclaration")
      private DataSampleSetUploadResponseImpl()
         {
         }

      private DataSampleSetUploadResponseImpl(final String result, final String message)
         {
         this.result = result;
         this.message = message;
         }

      @Override
      public long getTimestampUtcMillis()
         {
         return timestampUtcMillis;
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
         sb.append("DataSampleSetUploadResponseImpl");
         sb.append("{result='").append(result).append('\'');
         sb.append(", message='").append(message).append('\'');
         sb.append(", payload=").append(payload);
         sb.append(", timestampUtcMillis=").append(timestampUtcMillis);
         sb.append('}');
         return sb.toString();
         }
      }
   }
