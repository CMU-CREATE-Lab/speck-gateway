package org.bodytrack.airbot;

import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class AirBotUtils
   {
   private static final Logger LOG = Logger.getLogger(AirBotUtils.class);

   /** The standard <code>DataFile</code> extension (as it exists on the AirBot) */
   public static final String FILENAME_EXTENSION = ".TXT";

   private static final Pattern PROTOCOL_1_FILENAME_PATTERN = Pattern.compile("\\d+\\.TXT");
   private static final Pattern DEFAULT_FILENAME_PATTERN = Pattern.compile("[a-fA-F0-9]+\\.TXT");

   /**
    * Returns <code>true</code> if the filename is valid, <code>false</code> otherwise.
    */
   public static boolean isFilenameValid(@NotNull final String filename, @NotNull final AirBotConfig airBotConfig)
      {
      return ((airBotConfig.getProtocolVersion() == 1) ? PROTOCOL_1_FILENAME_PATTERN : DEFAULT_FILENAME_PATTERN).matcher(filename).matches();
      }

   /**
    * Attempts to map the given filename to a "proper" filename.  If the filename is
    * {@link #isFilenameValid(String, AirBotConfig) valid}, then this method maps the filename to a new name, according
    * to the {@link AirBotConfig#getProtocolVersion() protocol version}.  Some versions of the AirBot don't have a
    * real-time clock, so they can only keep track of time relative to power-on, and thus the filenames are simply the
    * number of seconds since power-on for the first data sample in the file.  Later versions of the AirBot do have an
    * RTC, so the filenames are absolute timestamps, in hex digits.  This method maps older filenames to the new hex
    * format.  For new filenames, they are simply returned unchanged.
    *
    * @see #isFilenameValid(String, AirBotConfig)
    */
   @NotNull
   public static String mapFilename(@NotNull final String filename, @NotNull final AirBotConfig airBotConfig)
      {
      if (!isFilenameValid(filename, airBotConfig))
         {
         final String message = "Invalid filename: filename [" + filename + "] does not match the expected pattern";
         LOG.error("AirBotUtils.mapFilename(): " + message);
         }

      return filename;
      }

   /**
    * Simply returns a {@link String} consisting of all characters before the first instance of a dot (.) in the given
    * <code>filename</code>.
    */
   @NotNull
   public static String computeBaseFilename(@NotNull final String filename)
      {
      // get the base filename
      final int dotPosition = filename.indexOf('.');
      final String baseFilename;
      if (dotPosition >= 0)
         {
         baseFilename = filename.substring(0, dotPosition);
         }
      else
         {
         baseFilename = filename;
         }
      return baseFilename;
      }

   private AirBotUtils()
      {
      // private to prevent instantiation
      }
   }
