package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 */
public class DotFormatter {
    /**
     * Write relationships associated with the given table
     *
     * @param table Table
     * @param dot LineWriter
     * @throws IOException
     * @return boolean <code>true</code> if implied relationships were written
     */
    public WriteStats writeRelationships(Table table, boolean includeImplied, boolean twoDegreesOfSeparation, LineWriter dot) throws IOException {
        Set tablesWritten = new HashSet();
        WriteStats stats = new WriteStats();
        boolean[] wroteImplied = new boolean[1];

        DotTableFormatter formatter = new DotTableFormatter();

        writeDotHeader(includeImplied ? "impliedTwoDegreesRelationshipsGraph" : (twoDegreesOfSeparation ? "twoDegreesRelationshipsGraph" : "oneDegreeRelationshipsGraph"), dot);

        Set relatedTables = getImmediateRelatives(table, includeImplied, wroteImplied);

        formatter.writeNode(table, "", true, true, true, dot);
        Set relationships = formatter.getRelationships(table, includeImplied);
        tablesWritten.add(table);
        stats.wroteTable(table);

        // write immediate relatives first
        Iterator iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            formatter.writeNode(relatedTable, "", true, false, false, dot);
            stats.wroteTable(relatedTable);
            relationships.addAll(formatter.getRelationships(relatedTable, table, includeImplied));
        }

        // next write 'cousins' (2nd degree of separation)
        if (twoDegreesOfSeparation) {
            iter = relatedTables.iterator();
            while (iter.hasNext()) {
                Table relatedTable = (Table)iter.next();
                Set cousins = getImmediateRelatives(relatedTable, includeImplied, wroteImplied);

                Iterator cousinsIter = cousins.iterator();
                while (cousinsIter.hasNext()) {
                    Table cousin = (Table)cousinsIter.next();
                    if (!tablesWritten.add(cousin))
                        continue; // already written
                    relationships.addAll(formatter.getRelationships(cousin, relatedTable, includeImplied));
                    formatter.writeNode(cousin, "", false, false, false, dot);
                    stats.wroteTable(cousin);
                }
            }
        }

        iter = new TreeSet(relationships).iterator();
        while (iter.hasNext())
            dot.writeln(iter.next().toString());

        dot.writeln("}");
        stats.setWroteImplied(wroteImplied[0]);
        return stats;
    }

    /**
     * I have having to use an array of one boolean to return another value...ugh
     */
    private Set getImmediateRelatives(Table table, boolean includeImplied, boolean[] foundImplied) {
        Set relatedColumns = new HashSet();
        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                boolean implied = column.getChildConstraint(childColumn).isImplied();
                foundImplied[0] |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(childColumn);
            }
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                boolean implied = column.getParentConstraint(parentColumn).isImplied();
                foundImplied[0] |= implied;
                if (!implied || includeImplied)
                    relatedColumns.add(parentColumn);
            }
        }

        Set relatedTables = new HashSet();
        iter = relatedColumns.iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            relatedTables.add(column.getTable());
        }

        relatedTables.remove(table);

        return relatedTables;
    }


    private void writeDotHeader(String name, LineWriter dot) throws IOException {
        dot.writeln("digraph " + name + " {");
        dot.writeln("  graph [");
        dot.writeln("    rankdir=\"RL\"");
        dot.writeln("    bgcolor=\"" + StyleSheet.getBodyBackground() + "\"");
        dot.writeln("    concentrate=\"true\"");
        dot.writeln("  ];");
        dot.writeln("  node [");
        dot.writeln("    fontsize=\"11\"");
        dot.writeln("    shape=\"plaintext\"");
        dot.writeln("  ];");
    }

    public int writeRelationships(Collection tables, boolean includeImplied, LineWriter dot) throws IOException {
        DotTableFormatter formatter = new DotTableFormatter();
        int numWritten = 0;
        writeDotHeader(includeImplied ? "impliedRelationshipsGraph" : "relationshipsGraph", dot);

        Iterator iter = tables.iterator();

        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(includeImplied)) {
                formatter.writeNode(table, "tables/", true, false, false, dot);
                ++numWritten;
            }
        }

        Set relationships = new TreeSet();
        iter = tables.iterator();

        while (iter.hasNext())
            relationships.addAll(formatter.getRelationships((Table)iter.next(), includeImplied));

        iter = relationships.iterator();
        while (iter.hasNext())
            dot.writeln(iter.next().toString());

        dot.writeln("}");

        return numWritten;
    }

    public void writeOrphan(Table table, LineWriter dot) throws IOException {
        DotTableFormatter formatter = new DotTableFormatter();
        writeDotHeader(table.getName(), dot);
        formatter.writeNode(table, "tables/", true, false, true, dot);
        dot.writeln("}");
    }
}
