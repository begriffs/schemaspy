package net.sourceforge.schemaspy.view;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;

/**
 * Simple ugly hack that provides details of what was written.
 */
public class WriteStats {
    private int numTables;
    private int numViews;
    private final Set<TableColumn> excludedColumns;

    public WriteStats(Collection<Table> tables) {
        this.excludedColumns = new HashSet<TableColumn>();
        
        for (Table table : tables) {
            for (TableColumn column : table.getColumns()) {
                if (column.isExcluded()) {
                    excludedColumns.add(column);
                }
            }
        }
    }

    public WriteStats(WriteStats stats) {
        this.excludedColumns = stats.excludedColumns;
    }

    public void wroteTable(Table table) {
        if (table.isView())
            ++numViews;
        else
            ++numTables;
    }

    public int getNumTablesWritten() {
        return numTables;
    }

    public int getNumViewsWritten() {
        return numViews;
    }

    public Set<TableColumn> getExcludedColumns() {
        return excludedColumns;
    }
}