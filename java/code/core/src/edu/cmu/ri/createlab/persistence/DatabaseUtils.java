package edu.cmu.ri.createlab.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class DatabaseUtils
   {
   public static boolean doesTableExist(@NotNull final Connection connection, @NotNull final String tableName) throws SQLException
      {
      final Set<String> tableNames = getTableNames(connection);
      return tableNames.contains(tableName.toLowerCase());
      }

   // Got this from: http://stackoverflow.com/questions/5866154/how-to-create-table-if-it-doesnt-exist-using-derby-db
   @NotNull
   private static Set<String> getTableNames(@NotNull final Connection connection) throws SQLException
      {
      final DatabaseMetaData databaseMetaData = connection.getMetaData();
      final Set<String> tableNames = new HashSet<String>();
      tableNames.addAll(readTable(databaseMetaData, "TABLE"));
      tableNames.addAll(readTable(databaseMetaData, "VIEW"));
      return tableNames;
      }

   // Got this from: http://stackoverflow.com/questions/5866154/how-to-create-table-if-it-doesnt-exist-using-derby-db
   @NotNull
   private static Set<String> readTable(@NotNull final DatabaseMetaData databaseMetaData,
                                        @NotNull final String searchCriteria) throws SQLException
      {
      final ResultSet rs = databaseMetaData.getTables(null,                         // catalog
                                                      null,                         // schema pattern
                                                      null,                         // table name pattern
                                                      new String[]{searchCriteria}  // types
      );

      final Set<String> tableNames = new HashSet<String>();
      while (rs.next())
         {
         tableNames.add(rs.getString("TABLE_NAME").toLowerCase());
         }
      return tableNames;
      }

   private DatabaseUtils()
      {
      // private to prevent instantiation
      }
   }
