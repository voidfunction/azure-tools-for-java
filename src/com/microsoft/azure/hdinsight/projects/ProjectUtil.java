package com.microsoft.azure.hdinsight.projects;


import com.microsoft.azure.hdinsight.common.DefaultLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class ProjectUtil {
    private static final String downloadSparkSDKUrl = "http://go.microsoft.com/fwlink/?LinkID=723585&clcid=0x409";

    public static JPanel createSparkSDKTipsPanel() {
        final JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();

        JLabel[] labels = new JLabel[]{
                new JLabel("You can either download Spark library from "),
                new JLabel("<HTML><FONT color=\"#000099\"><U>here</U></FONT>,</HTML>"),
                new JLabel("or add Apache Spark packages from Maven repository in the project manually.")
        };

        for (int i = 0; i < labels.length; ++i) {
            panel.add(labels[i]);
        }

        labels[1].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        labels[1].setToolTipText(downloadSparkSDKUrl);
        labels[1].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    StringSelection stringSelection = new StringSelection(downloadSparkSDKUrl);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                    JOptionPane.showMessageDialog(panel,"Already copy Download URL to Clipboard");
                } else if(SwingUtilities.isLeftMouseButton(e)){
                    try
                    {
                        URI uri = new URI(downloadSparkSDKUrl);
                        Desktop.getDesktop().browse(uri);
                    }catch (Exception exception) {
                        DefaultLoader.getUIHelper().showError(exception.getMessage(), exception.getClass().getName());
                    }
                }
            }
        });

        GridBagConstraints constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE,
                1, 1,
                1, 1,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0);

        layout.setConstraints(labels[0], constraints);
        layout.setConstraints(labels[1], constraints);
        layout.setConstraints(labels[2], constraints);

        JPanel mainPanel = new JPanel();
        GridBagLayout mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        mainPanel.add(panel, new GridBagConstraints(0,0,
                1,1,
                0,0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));

        //make sure label message on the head of left
        mainPanel.add(new JLabel(), new GridBagConstraints(1,0,
                1,1,
                1,1,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));

        return mainPanel;
    }
}
