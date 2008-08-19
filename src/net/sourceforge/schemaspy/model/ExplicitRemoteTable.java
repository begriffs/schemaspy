package net.sourceforge.schemaspy.model;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * A remote table (exists in another schema) that was explicitly created via XML metadata.
 * 
 * @author John Currier
 */
public class ExplicitRemoteTable extends RemoteTable {
    public ExplicitRemoteTable(Database db, String schema, String name, String baseSchema) throws SQLException {
        super(db, schema, name, baseSchema, null);
    }
    
    @Override
    public void connectForeignKeys(Map<String, Table> tables, Database db, Properties properties) throws SQLException {
        // this probably won't work, so ignore any failures...but try anyways just in case
        try {
            super.connectForeignKeys(tables, db, properties);
        } catch (SQLException ignore) {}
    }
}