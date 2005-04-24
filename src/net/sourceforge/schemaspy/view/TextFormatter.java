package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.LineWriter;
import net.sourceforge.schemaspy.model.Table;

public class TextFormatter {
    public void write(Database database, Collection tables, boolean includeViews, LineWriter out) throws IOException {
        for (Iterator iter = tables.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            if (!table.isView() || includeViews)
                out.writeln(table.getName());
        }
    }
}
