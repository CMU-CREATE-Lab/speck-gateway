package org.bodytrack.airbot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import edu.cmu.ri.createlab.persistence.DatabaseUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <code>DatabaseDataSampleStore</code> handles storage and retrieval of {@link AirBot.DataSample data samples}, storing
 * them in a local database.
 * </p>
 * <p>By default, if the database doesn't exist it will be created in a subdirectory of the user's home directory
 * (e.g. <code>~/BodyTrack/AirBotData/AirBot5832343135321501101617/database</code>).  This can be overridden by
 * specifying a different directory in the <code>derby.system.home</code> system property.
 * </p>
 * <p>
 * Much of this code is taken from the Apache Derby project's <a href="http://svn.apache.org/repos/asf/db/derby/code/trunk/java/demo/simple/SimpleApp.java">SimpleApp example</a>.
 * </p>
 *
 * @author Apache Derby
 * @author Chris Bartley (bartley@cmu.edu)
 */
final class DatabaseDataSampleStore implements DataSampleStore
   {
   private static final Logger LOG = Logger.getLogger(DatabaseDataSampleStore.class);

   private static final String DERBY_SYSTEM_HOME_PROPERTY_KEY = "derby.system.home";

   private static final String DATABASE_NAME = "db";

   private static final String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
   private static final String PROTOCOL = "jdbc:derby:";

   private static final String STATEMENT_NAME_INSERT_SAMPLE = "insert_sample";
   private static final String STATEMENT_INSERT_SAMPLE = "INSERT INTO AirBotSamples (particle_count, temperature, humidity, sample_timestamp_utc_secs, download_timestamp_utc_millis) VALUES (?, ?, ?, ?, ?)";

   private static final String SQL_STATE_DUPLICATE_KEY = "23505";
   private static final int SQL_ERROR_CODE_DUPLICATE_KEY = 30000;
   private static final String LINE_SEPARATOR = System.getProperty("line.separator");

   /**
    * Returns the details of a {@link SQLException} chain as a {@link String}. Details included are SQL State, Error
    * code, Exception message.
    */
   private static String getSqlExceptionAsString(final SQLException sqlException)
      {
      final StringBuilder s = new StringBuilder(LINE_SEPARATOR);

      // Unwraps the entire exception chain to unveil the real cause of the Exception.
      SQLException e = sqlException;
      while (e != null)
         {
         s.append("----- SQLException -----").append(LINE_SEPARATOR);
         s.append("  SQL State:  ").append(e.getSQLState()).append(LINE_SEPARATOR);
         s.append("  Error Code: ").append(e.getErrorCode()).append(LINE_SEPARATOR);
         s.append("  Message:    ").append(e.getMessage()).append(LINE_SEPARATOR);
         e = e.getNextException();
         }

      return s.toString();
      }

   private Connection connection = null;
   private final Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();
   private boolean isShutDown = false;
   private final Lock lock = new ReentrantLock();

   DatabaseDataSampleStore(@NotNull final AirBotConfig airBotConfig) throws InitializationException
      {
      lock.lock();  // block until condition holds
      try
         {
         // See whether the Derby home directory is defined.  If not, then define it.  We do this because the database
         // will be created under the directory specified by the derby.system.home system property.
         LOG.info("DatabaseDataSampleStore.DatabaseDataSampleStore(): System.getProperty(" + DERBY_SYSTEM_HOME_PROPERTY_KEY + ") = [" + System.getProperty(DERBY_SYSTEM_HOME_PROPERTY_KEY) + "]");
         if (System.getProperty(DERBY_SYSTEM_HOME_PROPERTY_KEY) == null)
            {
            final File dataDirectory = AirBotUploaderConstants.FilePaths.getDeviceDataDirectory(airBotConfig);
            final File databaseParentDirectory = new File(dataDirectory, "database");
            System.setProperty(DERBY_SYSTEM_HOME_PROPERTY_KEY, databaseParentDirectory.getAbsolutePath());
            }
         LOG.info("DatabaseDataSampleStore.DatabaseDataSampleStore(): System.getProperty(" + DERBY_SYSTEM_HOME_PROPERTY_KEY + ") = [" + System.getProperty(DERBY_SYSTEM_HOME_PROPERTY_KEY) + "]");

         final String databaseParentDirectoryPath = System.getProperty(DERBY_SYSTEM_HOME_PROPERTY_KEY);
         final File databaseParentDirectory = new File(databaseParentDirectoryPath);
         final File databaseDirectory = new File(databaseParentDirectory, DATABASE_NAME);

         // Make sure the database directory parent exists
         //noinspection ResultOfMethodCallIgnored
         databaseParentDirectory.mkdirs();
         if (!databaseParentDirectory.isDirectory())
            {
            LOG.fatal("DatabaseDataSampleStore.DatabaseDataSampleStore(): Could not create the database directory.  Aborting.");
            System.exit(1);
            }

         if (LOG.isInfoEnabled())
            {
            LOG.info("DatabaseDataSampleStore.DatabaseDataSampleStore(): Database will be stored in directory [" + databaseDirectory.getAbsolutePath() + "]");
            }

         // Load the JDBC driver
         if (!loadDriver())
            {
            LOG.fatal("DatabaseDataSampleStore.DatabaseDataSampleStore(): Could not load the database driver.  Aborting.");
            System.exit(1);
            }

         // Define connection properties.  Providing a user name and password is optional in the embedded framework, but,
         // by default, the schema APP will be used when no username is provided.  Otherwise, the schema name is the same
         // as the user name.
         final Properties properties = new Properties();
         properties.put("user", "airbot");
         properties.put("password", "airbot");

         try
            {
            // This connection specifies create=true in the connection URL to cause the database to be created when connecting
            // for the first time. To remove the database, remove the directory derbyDB (the same as the database name) and
            // its contents.
            connection = DriverManager.getConnection(PROTOCOL + DATABASE_NAME + ";create=true", properties);
            }
         catch (SQLException e)
            {
            LOG.error("DatabaseDataSampleStore.DatabaseDataSampleStore(): SQLException while trying to create the database connection: " + getSqlExceptionAsString(e), e);
            }

         boolean wasSetupSuccessful = false;
         if (connection != null)
            {
            try
               {
               // We want transactions committed for us automatically. Autocommit is on by default in JDBC, but
               // there's no harm in making it explicit here.
               connection.setAutoCommit(true);

               // Creates tables, if necessary
               initializeDatabase(connection);

               // create prepared statements for insert and update
               preparedStatements.put(STATEMENT_NAME_INSERT_SAMPLE, connection.prepareStatement(STATEMENT_INSERT_SAMPLE));

               wasSetupSuccessful = true;
               }
            catch (SQLException e)
               {
               LOG.error("DatabaseDataSampleStore.DatabaseDataSampleStore(): SQLException while trying to configure or initialize the database: " + getSqlExceptionAsString(e), e);
               }

            if (LOG.isInfoEnabled())
               {
               LOG.info("DatabaseDataSampleStore.DatabaseDataSampleStore(): Connected to and created database " + DATABASE_NAME);
               }
            }

         if (!wasSetupSuccessful)
            {
            throw new InitializationException("Failed to create, configure, or initialize the database");
            }
         }
      finally
         {
         lock.unlock();
         }
      }

   @Override
   public boolean save(@NotNull final AirBot.DataSample dataSample)
      {
      final PreparedStatement insertStatement = preparedStatements.get(STATEMENT_NAME_INSERT_SAMPLE);

      if (insertStatement != null)
         {
         try
            {
            insertStatement.setInt(1, dataSample.getParticleCount());
            insertStatement.setInt(2, dataSample.getTemperature());
            insertStatement.setInt(3, dataSample.getHumidity());
            insertStatement.setInt(4, dataSample.getSampleTime());
            insertStatement.setLong(5, dataSample.getDownloadTime());
            insertStatement.executeUpdate();

            LOG.debug("DatabaseDataSampleStore.save(): Saved data sample [" + dataSample.getSampleTime() + "] to the database.");
            return true;
            }
         catch (SQLException e)
            {
            if (e.getErrorCode() == SQL_ERROR_CODE_DUPLICATE_KEY && SQL_STATE_DUPLICATE_KEY.equals(e.getSQLState()) )
               {
               LOG.error("DatabaseDataSampleStore.save(): Saved failed because a sample with timestamp [" + dataSample.getSampleTime() + "] already exists.  Duplicate sample timestamps are not allowed.");
               }
            else
               {
               LOG.error("DatabaseDataSampleStore.save(): SQLException while trying to save data sample [" + dataSample.getSampleTime() + "] " + getSqlExceptionAsString(e));
               }
            }
         }
      else
         {
         LOG.error("DatabaseDataSampleStore.save(): Save failed because no insert statement is defined!");
         }

      return false;
      }

   @Override
   public void resetStateOfUploadingSamples()
      {
      // TODO
      }

   public void shutdown()
      {
      lock.lock();  // block until condition holds
      try
         {
         if (!isShutDown)
            {
            LOG.debug("DatabaseDataSampleStore.shutdown(): Shutting down...");

            try
               {
               // the shutdown=true attribute shuts down Derby
               DriverManager.getConnection(PROTOCOL + ";shutdown=true");
               }
            catch (SQLException e)
               {
               if (((e.getErrorCode() == 50000) && ("XJ015".equals(e.getSQLState()))))
                  {
                  // we got the expected exception
                  LOG.info("DatabaseDataSampleStore.shutdown(): Derby shut down normally");
                  }
               else
                  {
                  // if the error code or SQLState is different, we have
                  // an unexpected exception (shutdown failed)
                  LOG.error("DatabaseDataSampleStore.shutdown(): Derby did not shut down normally: " + getSqlExceptionAsString(e));
                  }
               }

            // close the prepared statements
            for (final String statementName : preparedStatements.keySet())
               {
               final Statement statement = preparedStatements.get(statementName);
               if (statement != null)
                  {
                  try
                     {
                     statement.close();
                     }
                  catch (SQLException e)
                     {
                     LOG.error("DatabaseDataSampleStore.shutdown(): SQLException while trying to close the [" + statementName + "] statement.", e);
                     }
                  }
               }
            preparedStatements.clear();

            // close the connection
            try
               {
               if (connection != null)
                  {
                  connection.close();
                  connection = null;
                  LOG.info("DatabaseDataSampleStore.shutdown(): Connection closed normally");
                  }
               }
            catch (SQLException e)
               {
               LOG.error("DatabaseDataSampleStore.shutdown(): SQLException while closing the connection" + getSqlExceptionAsString(e));
               }

            isShutDown = true;
            }
         }
      finally
         {
         lock.unlock();
         }
      }

   /**
    * Loads the driver specified by {@link #DRIVER_NAME} and returns <code>true</code> upon success, <code>false</code>
    * otherwise.  Note that any static Derby system properties must be set before loading the driver in order for them
    * to have any effect.
    */
   private boolean loadDriver()
      {
        /*
         *  The JDBC driver is loaded by loading its class.
         *  If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers may
         *  be automatically loaded, making this code optional.
         *
         *  In an embedded environment, this will also start up the Derby
         *  engine (though not any databases), since it is not already
         *  running. In a client environment, the Derby engine is being run
         *  by the network server framework.
         *
         *  In an embedded environment, any static Derby system properties
         *  must be set before loading the driver to take effect.
         */
      try
         {
         Class.forName(DRIVER_NAME).newInstance();
         if (LOG.isInfoEnabled())
            {
            LOG.info("DatabaseDataSampleStore.loadDriver(): Loaded JDBC driver " + DRIVER_NAME);
            }
         return true;
         }
      catch (ClassNotFoundException e)
         {
         LOG.error("DatabaseDataSampleStore.loadDriver(): ClassNotFoundException while trying to load the JDBC driver " + DRIVER_NAME, e);
         }
      catch (InstantiationException e)
         {
         LOG.error("DatabaseDataSampleStore.loadDriver(): InstantiationException while trying to load the JDBC driver " + DRIVER_NAME, e);
         }
      catch (IllegalAccessException e)
         {
         LOG.error("DatabaseDataSampleStore.loadDriver(): IllegalAccessException while trying to load the JDBC driver " + DRIVER_NAME, e);
         }
      catch (Exception e)
         {
         LOG.error("DatabaseDataSampleStore.loadDriver(): Exception while trying to load the JDBC driver " + DRIVER_NAME, e);
         }

      return false;
      }

   private void initializeDatabase(@NotNull final Connection connection) throws SQLException
      {
      if (!DatabaseUtils.doesTableExist(connection, "AirBotSamples"))
         {
         Statement statement = null;

         try
            {
            LOG.debug("DatabaseDataSampleStore.initializeDatabase(): Creating table AirBotSamples...");
            statement = connection.createStatement();
            statement.execute("CREATE TABLE AirBotSamples (\n" +
                              "   id                            INTEGER     NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),\n" +
                              "   particle_count                INTEGER     NOT NULL,\n" +
                              "   temperature                   INTEGER     NOT NULL,\n" +
                              "   humidity                      INTEGER     NOT NULL,\n" +
                              "   sample_timestamp_utc_secs     INTEGER     NOT NULL,\n" +
                              "   download_timestamp_utc_millis BIGINT      NOT NULL,\n" +
                              "   upload_timestamp_utc_millis   BIGINT,\n" +
                              "   upload_status                 VARCHAR(13) NOT NULL DEFAULT 'not_attempted',\n" +
                              "   CONSTRAINT AirBotSamples_PrimaryKey PRIMARY KEY (id),\n" +
                              "   CONSTRAINT AirBotSamples_SampleTimestamp_Unique UNIQUE (sample_timestamp_utc_secs),\n" +
                              "   CONSTRAINT AirBotSamples_StatusContraint CHECK (upload_status IN\n" +
                              "                                                   ('not_attempted',\n" +
                              "                                                    'in_progress',\n" +
                              "                                                    'success',\n" +
                              "                                                    'failure'))\n" +
                              ")");

            LOG.debug("DatabaseDataSampleStore.initializeDatabase(): Creating indeces for table AirBotSamples...");

            statement.execute("CREATE INDEX AirBotSamples_ParticleCount ON AirBotSamples (particle_count)");
            statement.execute("CREATE INDEX AirBotSamples_Temperature ON AirBotSamples (temperature)");
            statement.execute("CREATE INDEX AirBotSamples_Humidity ON AirBotSamples (humidity)");
            statement.execute("CREATE INDEX AirBotSamples_DownloadTimestamp ON AirBotSamples (download_timestamp_utc_millis)");
            statement.execute("CREATE INDEX AirBotSamples_UploadTimestamp ON AirBotSamples (upload_timestamp_utc_millis)");
            statement.execute("CREATE INDEX AirBotSamples_UploadStatus ON AirBotSamples (upload_status)");

            LOG.debug("DatabaseDataSampleStore.initializeDatabase(): Database initialization complete!");
            }
         finally
            {
            if (statement != null)
               {
               try
                  {
                  statement.close();
                  }
               catch (SQLException e)
                  {
                  LOG.error("DatabaseDataSampleStore.initializeDatabase(): SQLException while trying to close the statement", e);
                  }
               }
            }
         }
      }

   // TODO: get rid of this eventually...
   public static void main(final String[] args) throws InitializationException
      {
      final DatabaseDataSampleStore store = new DatabaseDataSampleStore(
            new AirBotConfig()
            {
            @NotNull
            @Override
            public String getId()
               {
               return "CPBFakeAirbot";
               }

            @Override
            public int getProtocolVersion()
               {
               return 1;
               }
            });
      store.shutdown();
      }
   }
