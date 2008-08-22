package net.sourceforge.schemaspy.model.xml;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author John Currier
 */
public class TableColumnMeta {
    private final String name;
    private final String comments;
    private final boolean isPrimary;
    private final List<ForeignKeyMeta> foreignKeys = new ArrayList<ForeignKeyMeta>();
    private final boolean isExcluded;
    private final boolean isImpliedParentsDisabled;
    private final boolean isImpliedChildrenDisabled;
    
    TableColumnMeta(Node colNode) {
        NamedNodeMap attribs = colNode.getAttributes();
        String tmp;
        
        name = attribs.getNamedItem("name").getNodeValue();
        Node node = attribs.getNamedItem("comments");
        if (node != null) {
            tmp = node.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }
        
        node = attribs.getNamedItem("primaryKey");
        isPrimary = node != null && evalBoolean(node.getNodeValue());

        node = attribs.getNamedItem("disableImpliedKeys");
        if (node != null) {
            tmp = node.getNodeValue().trim().toLowerCase();
            if (tmp.equals("to")) {
                isImpliedChildrenDisabled = true;
                isImpliedParentsDisabled  = false;
            } else if (tmp.equals("from")) {
                isImpliedParentsDisabled  = true;
                isImpliedChildrenDisabled = false;
            } else if (tmp.equals("all")) {
                isImpliedChildrenDisabled = isImpliedParentsDisabled = true;
            } else {
                System.err.println("Unrecognized value for " + node.getNodeName() + ": '" + tmp + '\'');
                System.err.println("  Valid values include 'to', 'from' and 'all'");
                isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
            }
        } else {
            isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
        }

        node = attribs.getNamedItem("disableDiagramAssociations");
        isExcluded = node != null && evalBoolean(node.getNodeValue());
        
        NodeList fkNodes = ((Element)colNode.getChildNodes()).getElementsByTagName("foreignKey");
        
        for (int i = 0; i < fkNodes.getLength(); ++i) {
            Node fkNode = fkNodes.item(i);
            foreignKeys.add(new ForeignKeyMeta(fkNode));
        }
    }
    
    private boolean evalBoolean(String exp) {
        if (exp == null)
            return false;
        
        exp = exp.trim().toLowerCase();
        return exp.equals("true") || exp.equals("yes") || exp.equals("1");
    }
    
    public String getName() {
        return name;
    }
    
    public String getComments() {
        return comments;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public List<ForeignKeyMeta> getForeignKeys() {
        return foreignKeys;
    }
    
    public boolean isExcluded() {
        return isExcluded;
    }
    
    public boolean isImpliedParentsDisabled() {
        return isImpliedParentsDisabled;
    }
    
    public boolean isImpliedChildrenDisabled() {
        return isImpliedChildrenDisabled;
    }
}