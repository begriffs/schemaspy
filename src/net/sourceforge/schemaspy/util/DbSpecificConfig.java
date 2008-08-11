package net.sourceforge.schemaspy.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import net.sourceforge.schemaspy.Config;

/**
 * @author John Currier
 */
public class DbSpecificConfig {
    private final String type;
    private       String description;
    private final List<DbSpecificOption> options = new ArrayList<DbSpecificOption>();
    private final Config config = new Config();
    
    public DbSpecificConfig(final String dbType) {
        type = dbType;
        /*
        class DbPropLoader {
            Properties load(String dbType) {
                ResourceBundle bundle = ResourceBundle.getBundle(dbType);
                Properties properties;
                try {
                    String baseDbType = bundle.getString("extends");
                    int lastSlash = dbType.lastIndexOf('/');
                    if (lastSlash != -1)
                        baseDbType = dbType.substring(0, dbType.lastIndexOf("/") + 1) + baseDbType;
                    properties = load(baseDbType);  // recurse
                } catch (MissingResourceException doesntExtend) {
                    properties = new Properties();
                }

                return Config.add(properties, bundle);
            }
        }
        Properties props = new DbPropLoader().load(dbType);
        */

        Properties props;
        try {
            props = config.getDbProperties(dbType);
            description = props.getProperty("description");
            loadOptions(props);
        } catch (IOException exc) {
            description = exc.toString();
        }
    }

    private void loadOptions(Properties properties) {
        boolean inParam = false;

        StringTokenizer tokenizer = new StringTokenizer(properties.getProperty("connectionSpec"), "<>", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("<")) {
                inParam = true;
            } else if (token.equals(">")) {
                inParam = false;
            } else {
                if (inParam) {
                    String desc = properties.getProperty(token);
                    options.add(new DbSpecificOption(token, desc));
                }
            }
        }
    }

    /**
     * Returns a {@link List} of {@link DbSpecificOption}s that are applicable to the
     * specified database type.
     * 
     * @return
     */
    public List<DbSpecificOption> getOptions() {
        return options;
    }
    
    /**
     * Return the generic configuration associated with this DbSpecificCofig
     * 
     * @return
     */
    public Config getConfig() {
        return config;
    }

    public void dumpUsage() {
        System.out.println(" " + new File(type).getName() + ":");
        System.out.println("  " + description);
        
        for (DbSpecificOption option : getOptions()) {
            System.out.println("   -" + option.getName() + " " + (option.getDescription() != null ? "  \t" + option.getDescription() : ""));
        }
    }
    
    @Override
    public String toString() {
        return description;
    }
}
