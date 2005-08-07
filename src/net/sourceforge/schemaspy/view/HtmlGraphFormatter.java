package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;
    private static boolean printedInvalidVersionWarning = false;

    public boolean write(Table table, File graphDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(graphDir, table.getName() + ".1degree.dot");
        File oneDegreeGraphFile = new File(graphDir, table.getName() + ".1degree.png");
        File impliedDotFile = new File(graphDir, table.getName() + ".implied2degrees.dot");
        File impliedGraphFile = new File(graphDir, table.getName() + ".implied2degrees.png");
        File twoDegreesDotFile = new File(graphDir, table.getName() + ".2degrees.dot");
        File twoDegreesGraphFile = new File(graphDir, table.getName() + ".2degrees.png");

        try {
            DotRunner dot = getDot();
            if (dot == null)
                return false;

            dot.generateGraph(oneDegreeDotFile, oneDegreeGraphFile);

            html.write("<br/><b>Close relationships");
            if (stats.wroteTwoDegrees()) {
                html.writeln("</b><span class='degrees' id='degrees'>");
                html.write("&nbsp;within <input type='radio' name='degrees' id='oneDegree' onclick=\"");
                html.write("if (!this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\" checked>one");
                html.write("  <input type='radio' name='degrees' id='twoDegrees' onclick=\"");
                html.write("if (this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\">two degrees of separation");
                html.writeln("</span><b>:</b>");
            } else {
                html.write(":</b>");
            }
            html.writeln("  <a name='graph'><img src='../graphs/" + oneDegreeGraphFile.getName() + "' usemap='#oneDegreeRelationshipsGraph' id='relationships' border='0' alt='' align='left'></a>");
            dot.writeMap(oneDegreeDotFile, html);
            if (stats.wroteImplied()) {
                dot.generateGraph(impliedDotFile, impliedGraphFile);
                dot.writeMap(impliedDotFile, html);
            } else {
                impliedDotFile.delete();
                impliedGraphFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                dot.generateGraph(twoDegreesDotFile, twoDegreesGraphFile);
                dot.writeMap(twoDegreesDotFile, html);
            } else {
                twoDegreesDotFile.delete();
                twoDegreesGraphFile.delete();
            }
        } catch (IOException noDot) {
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasOrphans, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        File relationshipsDotFile = new File(graphDir, dotBaseFilespec + ".real.dot");
        File relationshipsGraphFile = new File(graphDir, dotBaseFilespec + ".real.png");
        File impliedDotFile = new File(graphDir, dotBaseFilespec + ".implied.dot");
        File impliedGraphFile = new File(graphDir, dotBaseFilespec + ".implied.png");

        try {
            DotRunner dot = getDot();
            if (dot == null)
                return false;

            dot.generateGraph(relationshipsDotFile, relationshipsGraphFile);
            writeRelationshipsHeader(db, relationshipsGraphFile, impliedGraphFile, "Relationships Graph", hasOrphans, hasImpliedRelationships, html);
            html.writeln("  <a name='graph'><img src='graphs/summary/" + relationshipsGraphFile.getName() + "' usemap='#relationshipsGraph' id='relationships' border='0' alt=''></a>");
            dot.writeMap(relationshipsDotFile, html);

            if (hasImpliedRelationships) {
                dot.generateGraph(impliedDotFile, impliedGraphFile);
                dot.writeMap(impliedDotFile, html);
            }

            writeFooter(html);
        } catch (IOException noDot) {
            return false;
        }

        return true;
    }

    public boolean writeOrphans(Database db, List orphanTables, boolean hasRelationships, File graphDir, LineWriter html) throws IOException {
        DotRunner dot = getDot();
        if (dot == null)
            return false;

        Set orphansWithImpliedRelationships = new HashSet();
        Iterator iter = orphanTables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeOrphansHeader(db, "Utility Tables Graph", hasRelationships, !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".1degree.png");

                LineWriter dotOut = new LineWriter(new FileWriter(dotFile));
                new DotFormatter().writeOrphan(table, dotOut);
                dotOut.close();
                try {
                    if (!dot.generateGraph(dotFile, graphFile))
                        return false;
                } catch (IOException noDot) {
                    return false;
                }

                html.write("  <img src='graphs/summary/" + graphFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                dot.writeMap(dotFile, html);
            }

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeRelationshipsHeader(Database db, File relationshipsGraphFile, File impliedGraphFile, String title, boolean hasOrphans, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.write("<br/><a href='index.html'>Tables</a>&nbsp;&nbsp;");
        if (hasOrphans)
            html.write("<a href='utilities.html' title='Graphical view of tables with neither parents nor children'>Utility Tables Graph</a>&nbsp;&nbsp;");
        html.write("<a href='constraints.html' title='Useful for diagnosing error messages that just give constraint name or number'>Constraints</a>&nbsp;&nbsp;");
        html.writeln("<a href='anomalies.html' title=\"Things that aren't quite right\">Anomalies</a>");

        if (hasImpliedRelationships) {
            html.writeln("<p/><form name='options' action=''>");
            html.write("  <input type='checkbox' id='graphType' onclick=\"");
            html.write("if (!this.checked)");
            html.write(" selectGraph('graphs/summary/" + relationshipsGraphFile.getName() + "', '#relationshipsGraph'); ");
            html.write("else");
            html.write(" selectGraph('graphs/summary/" + impliedGraphFile.getName() + "', '#impliedRelationshipsGraph');");
            html.write("\">");
            html.writeln("Include implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void writeOrphansHeader(Database db, String title, boolean hasRelationships, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.writeln("<br/><a href='index.html'>Tables</a>");
        if (hasRelationships)
            html.write("<a href='relationships.html' title='Graphical view of table relationships'>Relationships Graph</a>&nbsp;&nbsp;");
        html.write("<a href='constraints.html' title='Useful for diagnosing error messages that just give constraint name or number'>Constraints</a>&nbsp;&nbsp;");
        html.writeln("<a href='anomalies.html' title=\"Things that aren't quite right\">Anomalies</a>");
        if (hasImpliedRelationships) {
            html.writeln("<p/><form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private DotRunner getDot() {
        DotRunner dot = DotRunner.getInstance();
        if (!dot.exists()) {
            if (!printedNoDotWarning) {
                printedNoDotWarning = true;
                System.err.println();
                System.err.println("Warning: Failed to run dot.");
                System.err.println("   Download " + dot.getSupportedVersions());
                System.err.println("   from www.graphviz.org and make sure dot is in your path.");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }

            return null;
        }

        if (!dot.isSupportedVersion()) {
            if (!printedInvalidVersionWarning) {
                printedInvalidVersionWarning = true;
                System.err.println();
                System.err.println("Warning: Invalid version of dot detected (" + dot.getVersion() + ").");
                System.err.println("   SchemaSpy requires " + dot.getSupportedVersions() + ".");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }
            return null;
        }

        return dot;
    }
}
