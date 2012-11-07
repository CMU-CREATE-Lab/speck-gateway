package org.bodytrack.airbot;

import java.util.Set;
import java.util.SortedSet;
import edu.cmu.ri.createlab.device.CreateLabDeviceProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface AirBot extends CreateLabDeviceProxy
   {
   /**
    * Returns the available files as a {@link SortedSet}.  Will return an empty {@link Set} if there are no files
    * available.  Returns <code>null</code> if the command failed.  Filenames in the {@link Set} are guaranteed to
    * be non-<code>null</code> and have a non-zero length.
    */
   @Nullable
   SortedSet<String> getAvailableFilenames();

   /**
    * Retrieves a {@link DataFile} from the AirBot specified by the given <code>filename</code>.  Returns a
    * {@link DataFile} for the file upon success, or returns <code>null</code> if the command failed or the given
    * <code>filename</code> is <code>null</code>.  If there is no such file available from the AirBot, this
    * method will throw a {@link NoSuchFileException}.
    *
    * @throws NoSuchFileException if the AirBot does not have a file with the given <code>filename</code>
    */
   @Nullable
   DataFile getFile(@Nullable final String filename) throws NoSuchFileException;

   /**
    * Requests that the device deletes the file specified by the given <code>filename</code>.  Returns <code>true</code>
    * upon success, <code>false</code> otherwise.
    */
   boolean deleteFile(@Nullable final String filename);

   /** Returns the {@link AirBotConfig configuration} for this <code>AirBot</code>. */
   @NotNull
   AirBotConfig getAirBotConfig();
   }