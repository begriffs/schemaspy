package net.sourceforge.schemaspy.view;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlMainIndexPage extends HtmlFormatter {
    private static HtmlMainIndexPage instance = new HtmlMainIndexPage();
    private NumberFormat integerFormatter = NumberFormat.getIntegerInstance();

    /**
     * Singleton - prevent creation
     */
    private HtmlMainIndexPage() {
    }

    public static HtmlMainIndexPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection tables, boolean showRelationshipsGraph, boolean showOrphansGraph, LineWriter html) throws IOException, SQLException {
        Set byName = new TreeSet(new Comparator() {
            public int compare(Object object1, Object object2) {
                return ((Table)object1).getName().compareTo(((Table)object2).getName());
            }
        });
        byName.addAll(tables);

        Table tempTable = tables.isEmpty() ? null : (Table)tables.iterator().next();
        boolean showIds = tempTable != null && tempTable.getId() != null;

        int numViews = 0;
        for (Iterator iter = byName.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            if (table.isView())
                ++numViews;
        }

        writeHeader(database, byName.size() - numViews, numViews, showIds, showRelationshipsGraph, showOrphansGraph, html);

        int numRows = 0;
        for (Iterator iter = byName.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            numRows += writeLineItem(table, html);
        }

        writeFooter(numRows, html);
    }

    private void writeHeader(Database db, int numberOfTables, int numberOfViews, boolean showIds, boolean hasRelationships, boolean hasOrphans, LineWriter html) throws IOException, SQLException {
        writeHeader(db, null, null, hasRelationships, hasOrphans, html);
        html.writeln("<table width='100%'>");
        html.writeln(" <tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln(" </td></tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>Database Type: ");
        html.write(db.getDatabaseProduct());
        html.writeln("  </td>");
        html.writeln("  <td class='container' align='right' valign='top' rowspan='3'>");
        if (sourceForgeLogoEnabled())
            html.writeln("    <a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a><br>");
        html.write("    <br/>");
        writeFeedMe(html);
        html.writeln("  </td>");
        html.writeln(" </tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'><br/>");
        html.write("<a href='insertionOrder.txt' title='Useful for loading data into a database'>Insertion Order</a>&nbsp;");
        html.write("<a href='deletionOrder.txt' title='Useful for purging data from a database'>Deletion Order</a>");
        html.write("&nbsp;(for database loading/purging scripts)");
        html.writeln("</td>");
        html.writeln(" </tr>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<p/><b>");
        html.write(String.valueOf(numberOfTables));
        html.write(" Tables");
        if (numberOfViews > 0) {
            html.write(" and ");
            html.write(String.valueOf(numberOfViews));
            html.write(" View");
            if (numberOfViews != 1)
                html.write("s");
        }
        html.writeln(":</b>");
        html.writeln("<TABLE class='dataTable' border='1' rules='groups'>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        if (showIds)
            html.writeln("<colgroup>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        html.writeln("  <th valign='bottom'>Table</th>");
        if (showIds)
            html.writeln("  <th align='center' valign='bottom'>ID</th>");
        html.writeln("  <th align='right' valign='bottom'>Children</th>");
        html.writeln("  <th align='right' valign='bottom'>Parents</th>");
        html.writeln("  <th align='right' valign='bottom'>Rows</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");
    }

    private int writeLineItem(Table table, LineWriter html) throws IOException {
        html.writeln(" <tr>");
        html.write("  <td class='detail'><a href='tables/");
        html.write(table.getName());
        html.write(".html'>");
        html.write(table.getName());
        html.writeln("</a></td>");

        if (table.getId() != null) {
            html.write("  <td class='detail' align='right'>");
            html.write(String.valueOf(table.getId()));
            html.writeln("</td>");
        }

        html.write("  <td class='detail' align='right'>");
        int numRelatives = table.getNumRealChildren();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");
        html.write("  <td class='detail' align='right'>");
        numRelatives = table.getNumRealParents();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");

        html.write("  <td class='detail' align='right'>");
        if (!table.isView())
            html.write(String.valueOf(integerFormatter.format(table.getNumRows())));
        else
            html.write("<span title='Views contain no real rows'>view</span>");
        html.writeln("</td>");
        html.writeln("  </tr>");

        return table.getNumRows();
    }

    protected void writeFooter(int numRows, LineWriter html) throws IOException {
        html.writeln("</TABLE>");
        html.write("<p/>Total rows: ");
        html.write(String.valueOf(integerFormatter.format(numRows)));
        html.writeln("</div>");
        super.writeFooter(html);
    }

    protected boolean isMainIndex() {
        return true;
    }
}
