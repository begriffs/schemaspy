package net.sourceforge.schemaspy.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.Config;

/**
 * A table that's outside of the default schema but is referenced
 * by or references a table in the default schema.
 *
 * @author John Currier
 */
public class RemoteTable extends Table {
    private final String baseSchema;

    public RemoteTable(Database db, String schema, String name, String baseSchema, Properties properties, Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        super(db, schema, name, null, properties, excludeIndirectColumns, excludeColumns);
        this.baseSchema = baseSchema;
    }

    /**
     * Connect to the PK's referenced by this table that live in the original schema
     * @param db
     * @param tables
     */
    @Override
    public void connectForeignKeys(Map<String, Table> tables, Database db, Properties properties,
                                    Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        ResultSet rs = null;

        try {
            rs = db.getMetaData().getImportedKeys(null, getSchema(), getName());

            while (rs.next()) {
                String otherSchema = rs.getString("PKTABLE_SCHEM");
                if (otherSchema != null && otherSchema.equals(baseSchema)) {
                    addForeignKey(rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKTABLE_SCHEM"), rs.getString("PKTABLE_NAME"),
                            rs.getString("PKCOLUMN_NAME"), tables, db, properties, excludeIndirectColumns, excludeColumns);
                }
            }
        } catch (SQLException sqlExc) {
            // if explicitly asking for these details then propagate the exception
            if (Config.getInstance().isOneOfMultipleSchemas())
                throw sqlExc;

            // otherwise just report the fact that we tried & couldn't
            System.err.println("Couldn't resolve foreign keys for remote table " + getSchema() + "." + getName() + ": " + sqlExc);
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    @Override
    public boolean isRemote() {
        return true;
    }
}
