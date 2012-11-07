package org.bodytrack.util.json;

import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class JsonUtils
   {
   /** Returns <code>true</code> if the given JSON is non-<code>null</code> and valid; <code>false</code> otherwise. */
   // Got this from http://stackoverflow.com/questions/10226897/how-to-validate-json-with-jackson-json
   public static boolean isValid(@Nullable final String json)
      {
      if (json != null)
         {

         boolean isValid = false;
         try
            {
            final JsonParser parser = new ObjectMapper().getFactory().createJsonParser(json);
            while (parser.nextToken() != null)
               {
               }
            isValid = true;
            }
         catch (JsonParseException jpe)
            {
            jpe.printStackTrace();
            }
         catch (IOException ioe)
            {
            ioe.printStackTrace();
            }

         return isValid;
         }

      return false;
      }

   @Nullable
   public static <Clazz> Clazz fromJson(@Nullable final byte[] json, @Nullable final Class<Clazz> clazz) throws IOException
      {
      if (json != null)
         {
         return fromJson(new String(json), clazz);
         }
      return null;
      }

   @Nullable
   public static <Clazz> Clazz fromJson(@Nullable final String json, @Nullable final Class<Clazz> clazz) throws IOException
      {
      if (json != null && clazz != null)
         {
         final ObjectMapper mapper = new ObjectMapper();
         return mapper.readValue(json, clazz);
         }
      return null;
      }

   @Nullable
   public static <Clazz> Clazz fromJson(@Nullable final InputStream jsonInputStream, @Nullable final Class<Clazz> clazz) throws IOException
      {
      if (jsonInputStream != null && clazz != null)
         {
         final ObjectMapper mapper = new ObjectMapper();
         return mapper.readValue(jsonInputStream, clazz);
         }
      return null;
      }

   @Nullable
   public static byte[] toJson(@Nullable final Object object) throws JsonProcessingException
      {
      if (object != null)
         {
         final ObjectMapper mapper = new ObjectMapper();
         return mapper.writeValueAsBytes(object);
         }
      return null;
      }

   private JsonUtils()
      {
      // private to prevent instantiation
      }
   }
