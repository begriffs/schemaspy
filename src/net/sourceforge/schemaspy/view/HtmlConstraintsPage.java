package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.DbAnalyzer;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlConstraintsPage extends HtmlFormatter {
    private static HtmlConstraintsPage instance = new HtmlConstraintsPage();

    /**
     * Singleton - prevent creation
     */
    private HtmlConstraintsPage() {
    }

    public static HtmlConstraintsPage getInstance() {
        return instance;
    }

    public void write(Database database, List<ForeignKeyConstraint> constraints, Collection<Table> tables, boolean hasOrphans, LineWriter html) throws IOException {
        writeHeader(database, hasOrphans, html);
        writeForeignKeyConstraints(constraints, html);
        writeCheckConstraints(tables, html);
        writeFooter(html);
    }

    private void writeHeader(Database database, boolean hasOrphans, LineWriter html) throws IOException {
        writeHeader(database, null, "Constraints", hasOrphans, html);
        html.writeln("<div class='indent'>");
    }

    @Override
    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("</div>");
        super.writeFooter(html);
    }

    /**
     * Write specified foreign key constraints
     *
     * @param constraints List
     * @param html LineWriter
     * @throws IOException
     */
    private void writeForeignKeyConstraints(List<ForeignKeyConstraint> constraints, LineWriter html) throws IOException {
        Set<ForeignKeyConstraint> constraintsByName = new TreeSet<ForeignKeyConstraint>(new Comparator<ForeignKeyConstraint>() {
            public int compare(ForeignKeyConstraint cons1, ForeignKeyConstraint cons2) {
                return cons1.getName().compareToIgnoreCase(cons2.getName());
            }
        });
        constraintsByName.addAll(constraints);

        html.writeln("<table width='100%'>");
        html.writeln("<tr><td class='container' valign='bottom'><b>");
        html.write(String.valueOf(constraintsByName.size()));
        html.writeln(" Foreign Key Constraints:</b>");
        html.writeln("</td><td class='container' align='right'>");
        html.writeln("<table>");
        if (sourceForgeLogoEnabled())
            html.writeln("  <tr><td class='container' align='right' valign='top'><a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a></td></tr>");
        html.writeln("<tr><td class='container'>");
        writeFeedMe(html);
        html.writeln("</td></tr></table>");
        html.writeln("</td></tr>");
        html.writeln("</table><br>");
        html.writeln("<table class='dataTable' border='1' rules='groups'>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        html.writeln("  <th>Constraint Name</th>");
        html.writeln("  <th>Child Column</th>");
        html.writeln("  <th>Parent Column</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");
        for (ForeignKeyConstraint constraint : constraintsByName) {
            writeForeignKeyConstraint(constraint, html);
        }
        if (constraints.size() == 0) {
            html.writeln(" <tr>");
            html.writeln("  <td class='detail' valign='top' colspan='3'>None detected</td>");
            html.writeln(" </tr>");
        }
        html.writeln("</tbody>");
        html.writeln("</table>");
    }

    /**
     * Write specified foreign key constraint
     *
     * @param constraint ForeignKeyConstraint
     * @param html LineWriter
     * @throws IOException
     */
    private void writeForeignKeyConstraint(ForeignKeyConstraint constraint, LineWriter html) throws IOException {
        html.writeln(" <tr>");
        html.write("  <td class='detail'>");
        html.write(constraint.getName());
        html.writeln("</td>");
        html.write("  <td class='detail'>");
        for (Iterator<TableColumn> iter = constraint.getChildColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = iter.next();
            html.write("<a href='tables/");
            html.write(column.getTable().getName());
            html.write(".html'>");
            html.write(column.getTable().getName());
            html.write("</a>");
            html.write(".");
            html.write(column.getName());
            if (iter.hasNext())
                html.write("<br>");
        }
        html.writeln("</td>");
        html.write("  <td class='detail'>");
        for (Iterator<TableColumn> iter = constraint.getParentColumns().iterator(); iter.hasNext(); ) {
            TableColumn column = iter.next();
            html.write("<a href='tables/");
            html.write(column.getTable().getName());
            html.write(".html'>");
            html.write(column.getTable().getName());
            html.write("</a>");
            html.write(".");
            html.write(column.getName());
            if (iter.hasNext())
                html.write("<br>");
        }
        html.writeln("</td>");
        html.writeln(" </tr>");
    }

    /**
     * Write check constraints associated with the specified tables
     *
     * @param tables Collection
     * @param html LineWriter
     * @throws IOException
     */
    public void writeCheckConstraints(Collection<Table> tables, LineWriter html) throws IOException {
        html.writeln("<a name='checkConstraints'></a><p>");
        html.writeln("<b>Check Constraints:</b>");
        html.writeln("<TABLE class='dataTable' border='1' rules='groups'>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<colgroup>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        html.writeln("  <th>Table</th>");
        html.writeln("  <th>Constraint Name</th>");
        html.writeln("  <th>Constraint</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");

        List<Table> tablesByName = DbAnalyzer.sortTablesByName(new ArrayList<Table>(tables));

        int constraintsWritten = 0;

        // iter over all tables...only ones with check constraints will write anything
        for (Table table : tablesByName) {
            constraintsWritten += writeCheckConstraints(table, html);
        }

        if (constraintsWritten == 0) {
            html.writeln(" <tr>");
            html.writeln("  <td class='detail' valign='top' colspan='3'>None detected</td>");
            html.writeln(" </tr>");
        }

        html.writeln("</tbody>");
        html.writeln("</table>");
    }

    /**
     * Write check constraints associated with the specified table (if any)
     *
     * @param table Table
     * @param html LineWriter
     * @throws IOException
     * @return int
     */
    private int writeCheckConstraints(Table table, LineWriter html) throws IOException {
        Map<String, String> constraints = table.getCheckConstraints();  // constraint name -> text pairs
        int constraintsWritten = 0;
        for (String name : constraints.keySet()) {
            html.writeln(" <tr>");
            html.write("  <td class='detail' valign='top'><a href='tables/");
            html.write(table.getName());
            html.write(".html'>");
            html.write(table.getName());
            html.write("</a></td>");
            html.write("  <td class='detail' valign='top'>");
            html.write(name);
            html.writeln("</td>");
            html.write("  <td class='detail'>");
            html.write(constraints.get(name).toString());
            html.writeln("</td>");
            html.writeln(" </tr>");
            ++constraintsWritten;
        }

        return constraintsWritten;
    }

    @Override
    protected boolean isConstraintsPage() {
        return true;
    }
}