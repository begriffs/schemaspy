package net.sourceforge.schemaspy.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A {@link HashMap} implementation that uses {@link String}s as its keys
 * where the keys are treated without regard to case.  That is, <code>get("MyTableName")</code>
 * will return the same object as <code>get("MYTABLENAME")</code>.
 * 
 * @author John Currier
 */
public class CaseInsensitiveMap<V> extends HashMap<String, V>
{
    private static final long serialVersionUID = 1L;

    @Override
    public V get(Object key) {
        return super.get(((String)key).toUpperCase());
    }
    
    @Override
    public V put(String key, V value) {
        return super.put(key.toUpperCase(), value);
    }

    @Override
    @SuppressWarnings("unchecked") // until I get the iter declaration correct
    public void putAll(Map<? extends String, ? extends V> map) {
        Iterator iter = map.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry<String, V> entry = (Map.Entry<String, V>)iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public V remove(Object key) {
        return super.remove(((String)key).toUpperCase());
    }
    
    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(((String)key).toUpperCase());
    }
}