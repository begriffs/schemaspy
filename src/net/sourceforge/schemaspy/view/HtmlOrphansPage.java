package net.sourceforge.schemaspy.view;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlOrphansPage extends HtmlGraphFormatter {
    private static HtmlOrphansPage instance = new HtmlOrphansPage();

    private HtmlOrphansPage() {
    }

    public static HtmlOrphansPage getInstance() {
        return instance;
    }

    public boolean write(Database db, List<Table> orphanTables, File graphDir, LineWriter html) throws IOException {
        Dot dot = getDot();
        if (dot == null)
            return false;

        Set<Table> orphansWithImpliedRelationships = new HashSet<Table>();
        
        for (Table table : orphanTables) {
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeHeader(db, "Utility Tables Graph", !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            StringBuffer maps = new StringBuffer(64 * 1024);

            for (Table table : orphanTables) {
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".1degree.png");

                LineWriter dotOut = new LineWriter(dotFile, Config.DOT_CHARSET);
                DotFormatter.getInstance().writeOrphan(table, dotOut);
                dotOut.close();
                try {
                    maps.append(dot.generateGraph(dotFile, graphFile));
                } catch (Dot.DotFailure dotFailure) {
                    System.err.println(dotFailure);
                    return false;
                }

                html.write("  <img src='graphs/summary/" + graphFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            html.write(maps.toString());

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeHeader(Database db, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, true, html);
        html.writeln("<table class='container' width='100%'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td>");
        html.writeln("<td class='container' align='right' valign='top' rowspan='2'>");
        writeLegend(false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr><td class='container' align='left' valign='top'>");
        if (hasImpliedRelationships) {
            html.writeln("<form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getInstance().getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }
        html.writeln("</td></tr></table>");
    }

    protected boolean isOrphansPage() {
        return true;
    }
}
