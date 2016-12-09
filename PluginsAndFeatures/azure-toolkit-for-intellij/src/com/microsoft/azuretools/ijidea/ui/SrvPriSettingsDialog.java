package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class SrvPriSettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;
    private JTextPane selectSubscriptionCommentTextPane;
    private TextFieldWithBrowseButton destinationFolderTextField;
    private List<SubscriptionDetail> sdl;
    private int result = JOptionPane.CANCEL_OPTION;

    public String getDestinationFolder() {
        return destinationFolderTextField.getText();
    }

    public void setDestinationFolderTextField(TextFieldWithBrowseButton destinationFolderTextField) {
        this.destinationFolderTextField = destinationFolderTextField;
    }

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    public int getResult() {
        return result;
    }

    DefaultTableModel model = new DefaultTableModel() {
        final Class[] columnClass = new Class[] {
                Boolean.class, String.class, String.class
        };
        @Override
        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return columnClass[columnIndex];
        }
    };

    public static SrvPriSettingsDialog go(List<SubscriptionDetail> sdl, Component parent) throws Exception {
        SrvPriSettingsDialog d = new SrvPriSettingsDialog();
        d.sdl = sdl;
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d;
    }

    private SrvPriSettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Create authentication files");

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }

            @Override
            public void windowOpened (WindowEvent e) {
                setSubscriptions();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        model.addColumn("");
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");

        table.setModel(model);

        TableColumn column = table.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);

        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        destinationFolderTextField.setText(System.getProperty("user.home"));
        destinationFolderTextField.addBrowseFolderListener("Choose Destination Folder", "", null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor());
    }

    private void setSubscriptions() {
        for (SubscriptionDetail sd : sdl) {
            model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
            model.fireTableDataChanged();
        }
    }

    private void onOK() {
        int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            if (!selected) unselectedCount++;
        }

        if (unselectedCount == rc) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one subscription",
                    "Subscription dialog info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            this.sdl.get(ri).setSelected(selected);
        }
        result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        table = new JBTable();
    }

}
