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
public class TableMeta {
    private final String name;
    private final String comments;
    private final List<TableColumnMeta> columns = new ArrayList<TableColumnMeta>();
    private final String remoteSchema;
    
    TableMeta(Node tableNode) {
        NamedNodeMap attribs = tableNode.getAttributes();
        
        name = attribs.getNamedItem("name").getNodeValue();
        
        Node commentNode = attribs.getNamedItem("comments");
        if (commentNode != null) {
            String tmp = commentNode.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }
        
        Node remoteSchemaNode = attribs.getNamedItem("remoteSchema");
        if (remoteSchemaNode != null) {
            remoteSchema = remoteSchemaNode.getNodeValue().trim();
        } else {
            remoteSchema = null;
        }
        
        NodeList columnNodes = ((Element)tableNode.getChildNodes()).getElementsByTagName("column");
        
        for (int i = 0; i < columnNodes.getLength(); ++i) {
            Node colNode = columnNodes.item(i);
            columns.add(new TableColumnMeta(colNode));
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getComments() {
        return comments;
    }
    
    public List<TableColumnMeta> getColumns() {
        return columns;
    }
    
    public String getRemoteSchema() {
        return remoteSchema;
    }
}