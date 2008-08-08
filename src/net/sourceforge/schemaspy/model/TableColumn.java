package net.sourceforge.schemaspy.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.model.xml.TableColumnMeta;

public class TableColumn {
    private final Table table;
    private final String name;
    private final Object id;
    private final String type;
    private final int length;
    private final int decimalDigits;
    private final String detailedSize;
    private final boolean isNullable;
    private       boolean isAutoUpdated;
    private final Object defaultValue;
    private       String comments;
    private final Map<TableColumn, ForeignKeyConstraint> parents = new HashMap<TableColumn, ForeignKeyConstraint>();
    private final Map<TableColumn, ForeignKeyConstraint> children = new TreeMap<TableColumn, ForeignKeyConstraint>(new ColumnComparator());

    /**
     * Create a column associated with a table.
     *
     * @param table Table the table that this column belongs to
     * @param rs ResultSet returned from <code>java.sql.DatabaseMetaData.getColumns()</code>.
     * @throws SQLException
     */
    TableColumn(Table table, ResultSet rs) throws SQLException {
        this.table = table;
        
        // names and types are typically reused *many* times in a database,
        // so keep a single instance of each distint one
        // (thanks to Mike Barnes for the suggestion)
        String tmp = rs.getString("COLUMN_NAME");
        name = tmp == null ? null : tmp.intern();
        tmp = rs.getString("TYPE_NAME");
        type = tmp == null ? null : tmp.intern();
        
        decimalDigits = rs.getInt("DECIMAL_DIGITS");
        Number bufLength = (Number)rs.getObject("BUFFER_LENGTH");
        if (bufLength != null && bufLength.shortValue() > 0)
            length = bufLength.shortValue();
        else
            length = rs.getInt("COLUMN_SIZE");

        StringBuffer buf = new StringBuffer();
        buf.append(length);
        if (decimalDigits > 0) {
            buf.append(',');
            buf.append(decimalDigits);
        }
        detailedSize = buf.toString();

        isNullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        defaultValue = rs.getString("COLUMN_DEF");
        setComments(rs.getString("REMARKS"));
        id = new Integer(rs.getInt("ORDINAL_POSITION") - 1);
    }

    public Table getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    public Object getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getLength() {
        return length;
    }
    
    public int getDecimalDigits() {
        return decimalDigits;
    }

    public String getDetailedSize() {
        return detailedSize;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public boolean isAutoUpdated() {
        return isAutoUpdated;
    }

    public boolean isUnique() {
        for (TableIndex index : table.getIndexes()) {
            if (index.isUnique()) {
                List<TableColumn> indexColumns = index.getColumns();
                if (indexColumns.size() == 1 && indexColumns.contains(this)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isPrimary() {
        return table.getPrimaryColumns().contains(this);
    }
    
    /**
     * Returns <code>true</code> if this column points to another table's primary key.
     * @return
     */
    public boolean isForeignKey() {
        return !parents.isEmpty();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * @return Comments associated with this column, or <code>null</code> if none.
     */
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = (comments == null || comments.trim().length() == 0) ? null : comments.trim();
    }

    public void addParent(TableColumn parent, ForeignKeyConstraint constraint) {
        parents.put(parent, constraint);
        table.addedParent();
    }

    public void removeParent(TableColumn parent) {
        parents.remove(parent);
    }

    public void unlinkParents() {
        for (TableColumn parent : parents.keySet()) {
            parent.removeChild(this);
        }
        parents.clear();
    }

    public Set<TableColumn> getParents() {
        return parents.keySet();
    }

    /**
     * returns the constraint that connects this column to the specified column (this 'child' column to specified 'parent' column)
     */
    public ForeignKeyConstraint getParentConstraint(TableColumn parent) {
        return parents.get(parent);
    }

    /**
     * removes a parent constraint and returns it, or null if there are no parent constraints
     * @return
     */
    public ForeignKeyConstraint removeAParentFKConstraint() {
        for (TableColumn relatedColumn : parents.keySet()) {
            ForeignKeyConstraint constraint = parents.remove(relatedColumn);
            relatedColumn.removeChild(this);
            return constraint;
        }

        return null;
    }

    public ForeignKeyConstraint removeAChildFKConstraint() {
        for (TableColumn relatedColumn : children.keySet()) {
            ForeignKeyConstraint constraint = children.remove(relatedColumn);
            relatedColumn.removeParent(this);
            return constraint;
        }

        return null;
    }

    public void addChild(TableColumn child, ForeignKeyConstraint constraint) {
        children.put(child, constraint);
        table.addedChild();
    }

    public void removeChild(TableColumn child) {
        children.remove(child);
    }

    public void unlinkChildren() {
        for (TableColumn child : children.keySet())
            child.removeParent(this);
        children.clear();
    }

    /**
     * Returns <code>Set</code> of <code>TableColumn</code>s that have a real (or implied) foreign key that
     * references this <code>TableColumn</code>.
     * @return Set
     */
    public Set<TableColumn> getChildren() {
        return children.keySet();
    }

    /**
     * returns the constraint that connects the specified column to this column
     * (specified 'child' to this 'parent' column)
     */
    public ForeignKeyConstraint getChildConstraint(TableColumn child) {
        return children.get(child);
    }

    /**
     * setIsAutoUpdated
     *
     * @param isAutoUpdated boolean
     */
    public void setIsAutoUpdated(boolean isAutoUpdated) {
        this.isAutoUpdated = isAutoUpdated;
    }

    public boolean matches(Pattern regex) {
        return regex.matcher(getTable().getName() + '.' + getName()).matches();
    }

    /**
     * @param colMeta
     */
    public void update(TableColumnMeta colMeta) {
        String newComments = colMeta.getComments();
        if (newComments != null)
            setComments(newComments);
        
        if (colMeta.isPrimary() && !isPrimary())
            table.getPrimaryColumns().add(this);
    }

    @Override
    public String toString() {
        return getName();
    }

    private class ColumnComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getTable().getName().compareTo(column2.getTable().getName());
            if (rc == 0)
                rc = column1.getName().compareTo(column2.getName());
            return rc;
        }
    }
}
