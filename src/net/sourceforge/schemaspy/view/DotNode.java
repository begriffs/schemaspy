package net.sourceforge.schemaspy.view;

import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;

public class DotNode {
    private final Table table;
    private final boolean isDetailed;
    private final boolean showColumns;
    private boolean showImplied;
    private final String path;
    private final Set excludedColumns = new HashSet();
    private final String lineSeparator = System.getProperty("line.separator");
    private final boolean displayNumRows = Boolean.getBoolean("displayNumRows");

    /**
     * Create a DotNode that is a focal point of a graph
     *
     * @param table Table
     * @param path String
     */
    public DotNode(Table table, String path) {
        this(table, true, true, path);
    }

    /**
     * Create a DotNode and specify whether it displays its columns.
     *
     * @param table Table
     * @param showColumns boolean
     * @param path String
     */
    public DotNode(Table table, boolean showColumns, String path) {
        this(table, false, showColumns, path);
    }

    private DotNode(Table table, boolean isDetailed, boolean showColumns, String path) {
        this.table = table;
        this.isDetailed = isDetailed;
        this.showColumns = showColumns;
        this.path = path;
    }

    public boolean isDetailed() {
        return isDetailed;
    }

    public void setShowImplied(boolean showImplied) {
        this.showImplied = showImplied;
    }

    public Table getTable() {
        return table;
    }

    public void excludeColumn(TableColumn column) {
        excludedColumns.add(column);
    }

    public String toString() {
        StyleSheet css = StyleSheet.getInstance();
        StringBuffer buf = new StringBuffer();
        String tableName = table.getName();
        String colspan = isDetailed ? "COLSPAN=\"2\" " : "COLSPAN=\"3\" ";

        buf.append("  \"" + tableName + "\" [" + lineSeparator);
        buf.append("    label=<" + lineSeparator);
        buf.append("    <TABLE BORDER=\"" + (isDetailed ? "2" : "0") + "\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"" + css.getTableBackground() + "\">" + lineSeparator);
        buf.append("      <TR>");
        buf.append("<TD PORT=\"" + tableName + ".heading\" COLSPAN=\"3\" BGCOLOR=\"" + css.getTableHeadBackground() + "\" ALIGN=\"CENTER\">" + tableName + "</TD>");
        buf.append("</TR>" + lineSeparator);

        if (showColumns) {
            List primaryColumns = table.getPrimaryColumns();
            Set indexColumns = new HashSet();
            Iterator iter = table.getIndexes().iterator();
            while (iter.hasNext()) {
                TableIndex index = (TableIndex)iter.next();
                indexColumns.addAll(index.getColumns());
            }
            indexColumns.removeAll(primaryColumns);

            for (iter = table.getColumns().iterator(); iter.hasNext(); ) {
                TableColumn column = (TableColumn)iter.next();
                buf.append("      <TR>");
                buf.append("<TD PORT=\"" + column.getName() + "\" " + colspan);
                if (excludedColumns.contains(column))
                    buf.append("BGCOLOR=\"" + css.getExcludedColumnBackgroundColor() + "\" ");
                else if (primaryColumns.contains(column))
                    buf.append("BGCOLOR=\"" + css.getPrimaryKeyBackground() + "\" ");
                else if (indexColumns.contains(column))
                    buf.append("BGCOLOR=\"" + css.getIndexedColumnBackground() + "\" ");
                buf.append("ALIGN=\"LEFT\">");
                buf.append(column.getName());
                buf.append("</TD>");
                if (isDetailed) {
                    buf.append("<TD PORT=\"");
                    buf.append(column.getName());
                    buf.append(".type\" ALIGN=\"LEFT\">");
                    buf.append(column.getType().toLowerCase());
                    buf.append("[");
                    buf.append(column.getDetailedSize());
                    buf.append("]</TD>");
                }
                buf.append("</TR>" + lineSeparator);
            }
        }

        buf.append("      <TR>");
        buf.append("<TD ALIGN=\"LEFT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        int numParents = showImplied ? table.getNumParents() : table.getNumRealParents();
        if (numParents > 0 || isDetailed)
            buf.append("&lt; " + numParents);
        else
            buf.append("  ");
        buf.append("</TD>");
        buf.append("<TD ALIGN=\"RIGHT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        if (table.isView())
            buf.append("view");
        else {
            final int numRows = table.getNumRows();
            if (displayNumRows && numRows != -1) {
                buf.append(NumberFormat.getInstance().format(numRows));
                buf.append(" row"); 
                if (numRows != 1)
                    buf.append('s');
            } else {
                buf.append("  ");
            }
        }
        buf.append("</TD>");
        buf.append("<TD ALIGN=\"RIGHT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        int numChildren = showImplied ? table.getNumChildren() : table.getNumRealChildren();
        if (numChildren > 0 || isDetailed)
            buf.append(numChildren + " &gt;");
        else
            buf.append("  ");
        buf.append("</TD></TR>" + lineSeparator);

        buf.append("    </TABLE>>" + lineSeparator);
        buf.append("    URL=\"" + path + toNCR(tableName) + ".html" + (path.length() == 0 && !isDetailed ? "#graph" : "#") + "\"" + lineSeparator);
        buf.append("    tooltip=\"" + toNCR(tableName) + "\"" + lineSeparator);
        buf.append("  ];");

        return buf.toString();
    }
    
    /**
     * Translates specified string to Numeric Character Reference (NCR).
     * This (hopefully) allows Unicode languages to be displayed correctly.<p>
     * The basis for this code was found 
     * <a href='http://d.hatena.ne.jp/etherealmaro/20060806#1154886500'>here</a>.
     * 
     * @param str
     * @return
     */
    private static String toNCR(String str) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch <= 127) {    // don't confuse things unless necessary
                result.append(ch);
            } else {
                result.append("&#");
                result.append(Integer.parseInt(Integer.toHexString(ch), 16));
                result.append(";");
            }
        }
        return result.toString();
    }
}