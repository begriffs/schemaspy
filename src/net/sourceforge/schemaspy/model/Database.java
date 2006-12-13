package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

public class Database {
    private final String databaseName;
    private final String schema;
    private final String description;
    private final Map tables = new HashMap();
    private final Map views = new HashMap();
    private final Map remoteTables = new HashMap(); // key: schema.tableName value: RemoteTable
    private final DatabaseMetaData meta;
    private final Connection connection;
    private final String connectTime = new SimpleDateFormat("EEE MMM dd HH:mm z yyyy").format(new Date());

    public Database(Connection connection, DatabaseMetaData meta, String name, String schema, String description, Properties properties, Pattern include, int maxThreads) throws SQLException, MissingResourceException {
        this.connection = connection;
        this.meta = meta;
        this.databaseName = name;
        this.schema = schema;
        this.description = description;
        initTables(schema, meta, properties, include, maxThreads);
        initViews(schema, meta, properties, include);
        connectTables();
    }

    public String getName() {
        return databaseName;
    }

    public String getSchema() {
        return schema;
    }
    
    /**
     * @return null if a description wasn't specified.
     */
    public String getDescription() {
        return description;
    }

    public Collection getTables() {
        return tables.values();
    }

    public Collection getViews() {
        return views.values();
    }
    
    public Collection getRemoteTables() {
        return remoteTables.values();
    }
    
    public Connection getConnection() {
        return connection;
    }

    public DatabaseMetaData getMetaData() {
        return meta;
    }

    public String getConnectTime() {
        return connectTime;
    }

    public String getDatabaseProduct() {
        try {
            return meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion();
        } catch (SQLException exc) {
            return "";
        }
    }

    private void initTables(String schema, final DatabaseMetaData metadata, final Properties properties, final Pattern include, final int maxThreads) throws SQLException {
        String[] types = {"TABLE"};
        ResultSet rs = null;

        // "macro" to validate that a table is somewhat valid
        final class TableValidator {
            boolean isValid(ResultSet rs) throws SQLException {
                // some databases (MySQL) return more than we wanted
                if (!rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE"))
                    return false;
                
                // Oracle 10g introduced problematic flashback tables
                // with bizarre illegal names
                String tableName = rs.getString("TABLE_NAME");
                if (tableName.indexOf("$") != -1)
                    return false;
    
                if (!include.matcher(tableName).matches())
                    return false;
                
                return true;
            }
        }
        TableValidator tableValidator = new TableValidator();
        
        try {
            // creating tables takes a LONG time (based on JProbe analysis).
            // it's actually DatabaseMetaData.getIndexInfo() that's the pig.

            rs = metadata.getTables(null, schema, "%", types);

            TableCreator creator;
            if (maxThreads == 1) {
                creator = new TableCreator();
            } else {
                creator = new ThreadedTableCreator(maxThreads);

                // "prime the pump" so if there's a database problem we'll probably see it now
                // and not in a secondary thread
                while (rs.next()) {
                    if (tableValidator.isValid(rs)) {
                        new TableCreator().create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), getOptionalString(rs, "REMARKS"), properties);
                        break;
                    }
                }
            }

            while (rs.next()) {
                if (tableValidator.isValid(rs)) {
                    creator.create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), getOptionalString(rs, "REMARKS"), properties);
                }
            }

            creator.join();
        } finally {
            if (rs != null)
                rs.close();
        }

        initCheckConstraints(properties);
        initTableIds(properties);
        initIndexIds(properties);
        initTableComments(properties);
        initColumnComments(properties);
    }
    
    /**
     * Some databases don't play nice with their metadata.
     * E.g. Oracle doesn't have a REMARKS column at all.
     * This method ignores those types of failures, replacing them with null.
     */
    public String getOptionalString(ResultSet rs, String columnName)
    {
        try {
            return rs.getString(columnName);
        } catch (SQLException ignore) {
            return null;
        }
    }

    private void initCheckConstraints(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectCheckConstraintsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.addCheckConstraint(rs.getString("constraint_name"), rs.getString("text"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initTableIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectTableIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.setId(rs.getObject("table_id"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initIndexIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectIndexIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null) {
                        TableIndex index = table.getIndex(rs.getString("index_name"));
                        if (index != null)
                            index.setId(rs.getObject("index_id"));
                    }
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    private void initTableComments(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectTableCommentsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.setComments(rs.getString("comments"));
                }
            } catch (SQLException sqlException) {
                // don't die just because this failed
                System.err.println();
                System.err.println("Failed to retrieve table comments: " + sqlException);
                System.err.println(sql);
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    private void initColumnComments(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectColumnCommentsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null) {
                        TableColumn column = table.getColumn(rs.getString("column_name"));
                        if (column != null)
                            column.setComments(rs.getString("comments"));
                    }
                }
            } catch (SQLException sqlException) {
                // don't die just because this failed
                System.err.println();
                System.err.println("Failed to retrieve column comments: " + sqlException);
                System.err.println(sql);
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    /**
     * Create a <code>PreparedStatement</code> from the specified SQL.
     * The SQL can contain these named parameters (but <b>not</b> question marks).
     * <ol>
     * <li>:schema - replaced with the name of the schema
     * <li>:owner - alias for :schema
     * <li>:table - replaced with the name of the table
     * </ol>
     * @param sql String - SQL without question marks
     * @param tableName String - <code>null</code> if the statement doesn't deal with <code>Table</code>-level details.
     * @throws SQLException
     * @return PreparedStatement
     */
    public PreparedStatement prepareStatement(String sql, String tableName) throws SQLException {
        StringBuffer sqlBuf = new StringBuffer(sql);
        List sqlParams = getSqlParams(sqlBuf, tableName); // modifies sqlBuf
        PreparedStatement stmt = getConnection().prepareStatement(sqlBuf.toString());

        try {
            for (int i = 0; i < sqlParams.size(); ++i) {
                stmt.setString(i + 1, sqlParams.get(i).toString());
            }
        } catch (SQLException exc) {
            stmt.close();
            throw exc;
        }

        return stmt;
    }
    
    public Table addRemoteTable(String remoteSchema, String remoteTableName, String baseSchema) throws SQLException {
        String fullName = remoteSchema + "." + remoteTableName;
        Table remoteTable = (Table)remoteTables.get(fullName);
        if (remoteTable == null) {
            remoteTable = new RemoteTable(this, remoteSchema, remoteTableName, baseSchema);
            remoteTable.connectForeignKeys(tables, this);
            remoteTables.put(fullName, remoteTable);
        }
        
        return remoteTable;
    }

    /**
     * Replaces named parameters in <code>sql</code> with question marks and
     * returns appropriate matching values in the returned <code>List</code> of <code>String</code>s.
     *
     * @param sql StringBuffer input SQL with named parameters, output named params are replaced with ?'s.
     * @param tableName String
     * @return List of Strings
     *
     * @see #prepareStatement(String, String)
     */
    private List getSqlParams(StringBuffer sql, String tableName) {
        Map namedParams = new HashMap();
        String schema = getSchema();
        if (schema == null)
            schema = getName(); // some 'schema-less' db's treat the db name like a schema (unusual case)
        namedParams.put(":schema", schema);
        namedParams.put(":owner", schema); // alias for :schema
        if (tableName != null) {
            namedParams.put(":table", tableName);
            namedParams.put(":view", tableName); // alias for :table
        }

        List sqlParams = new ArrayList();
        int nextColon = sql.indexOf(":");
        while (nextColon != -1) {
            String paramName = new StringTokenizer(sql.substring(nextColon), " ,").nextToken();
            String paramValue = (String)namedParams.get(paramName);
            if (paramValue == null)
                throw new IllegalArgumentException("Unexpected named parameter '" + paramName + "' found in SQL '" + sql + "'");
            sqlParams.add(paramValue);
            sql.replace(nextColon, nextColon + paramName.length(), "?"); // replace with a ?
            nextColon = sql.indexOf(":", nextColon);
        }

        return sqlParams;
    }


    private void initViews(String schema, DatabaseMetaData metadata, Properties properties, Pattern include) throws SQLException {
        String[] types = {"VIEW"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                if (rs.getString("TABLE_TYPE").equals("VIEW")) {  // some databases (MySQL) return more than we wanted
                    System.out.print('.');
                    
                    Table view = new View(this, rs, properties.getProperty("selectViewSql"));
                    if (include.matcher(view.getName()).matches())
                        views.put(view.getName().toUpperCase(), view);
                }
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    private void connectTables() throws SQLException {
        Iterator iter = tables.values().iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            table.connectForeignKeys(tables, this);
        }
    }

    /**
     * Single-threaded implementation of a class that creates tables
     */
    private class TableCreator {
        /**
         * Create a table and put it into <code>tables</code>
         */
        void create(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            createImpl(schemaName, tableName, remarks, properties);
        }

        protected void createImpl(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            Table table = new Table(Database.this, schemaName, tableName, remarks, properties);
            tables.put(table.getName().toUpperCase(), table);
            System.out.print('.');
        }

        /**
         * Wait for all of the tables to be created.
         * By default this does nothing since this implementation isn't threaded.
         */
        void join() {
        }
    }

    /**
     * Multi-threaded implementation of a class that creates tables
     */
    private class ThreadedTableCreator extends TableCreator {
        private final Set threads = new HashSet();
        private final int maxThreads;

        ThreadedTableCreator(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        void create(final String schemaName, final String tableName, final String remarks, final Properties properties) throws SQLException {
            Thread runner = new Thread() {
                public void run() {
                    try {
                        createImpl(schemaName, tableName, remarks, properties);
                    } catch (SQLException exc) {
                        exc.printStackTrace(); // nobody above us in call stack...dump it here
                    } finally {
                        synchronized (threads) {
                            threads.remove(this);
                            threads.notify();
                        }
                    }
                }
            };

            synchronized (threads) {
                // wait for enough 'room'
                while (threads.size() >= maxThreads) {
                    try {
                        threads.wait();
                    } catch (InterruptedException interrupted) {
                    }
                }

                threads.add(runner);
            }

            runner.start();
        }

        /**
         * Wait for all of the started threads to complete
         */
        public void join() {
            while (true) {
                Thread thread;

                synchronized (threads) {
                    Iterator iter = threads.iterator();
                    if (!iter.hasNext())
                        break;

                    thread = (Thread)iter.next();
                }

                try {
                    thread.join();
                } catch (InterruptedException exc) {
                }
            }
        }
    }
}
