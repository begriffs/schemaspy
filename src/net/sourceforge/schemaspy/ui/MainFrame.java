package net.sourceforge.schemaspy.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author John Currier
 */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JPanel dbConfigPanel = null;
    private JPanel buttonBar = null;
    private JButton launchButton = null;
    private JPanel header;

    /**
     * This is the default constructor
     */
    public MainFrame() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setTitle("SchemaSpy");
        this.setSize(new Dimension(500, 312));
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * This method initializes dbConfigPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getDbConfigPanel() {
        if (dbConfigPanel == null) {
            dbConfigPanel = new DbConfigPanel();
        }
        return dbConfigPanel;
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = GridBagConstraints.RELATIVE;
            constraints.weightx = 1.0;

            constraints.anchor = GridBagConstraints.CENTER;
            constraints.insets = new Insets(4, 0, 4, 0);
            jContentPane.add(getHeaderPanel(), constraints);
            constraints.insets = new Insets(0, 0, 0, 0);
            
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.weighty = 1.0;
            //JScrollPane scroller = new JScrollPane();
            //scroller.setBorder(null);
            //scroller.setViewportView(getDbConfigPanel());
            //scroller.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));
            //jContentPane.add(scroller, constraints);
            jContentPane.add(getDbConfigPanel(), constraints);

//            constraints.fill = GridBagConstraints.VERTICAL;
//            constraints.weighty = 0.0;
//            JLabel filler = new JLabel();
//            filler.setPreferredSize(new Dimension(0, 0));
//            filler.setMinimumSize(new Dimension(0, 0));
//            jContentPane.add(filler, constraints);
            
            constraints.anchor = GridBagConstraints.SOUTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.weighty = 0.0;
            jContentPane.add(getButtonBar(), constraints);
        }
        return jContentPane;
    }

    /**
     * This method initializes buttonBar	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getButtonBar() {
        if (buttonBar == null) {
            buttonBar = new JPanel();
            buttonBar.setLayout(new FlowLayout(FlowLayout.TRAILING));
            buttonBar.add(getLaunchButton(), null);
        }
        return buttonBar;
    }
    
    private JPanel getHeaderPanel() {
        if (header == null) {
            header = new JPanel();
            header.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            header.add(new JLabel("SchemaSpy - Graphical Database Metadata Browser"), constraints);
            constraints.gridx = 0;
            constraints.gridy++;
            header.add(new JLabel("Select a database type and fill in the required fields"), constraints);
        }
        return header;
    }

    /**
     * This method initializes launchButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getLaunchButton() {
        if (launchButton == null) {
            launchButton = new JButton();
            launchButton.setText("Launch");
        }
        return launchButton;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
