package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class SubscriptionsDialog extends JDialog {
    private static final Logger LOGGER = Logger.getInstance(SubscriptionsDialog.class);

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;
    private JButton refreshButton;
    private List<SubscriptionDetail> sdl;
    private int result = JOptionPane.CANCEL_OPTION;


    private static class SubscriptionTableModel extends DefaultTableModel {
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

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    public int getResult() {
        return result;
    }

    public static SubscriptionsDialog go(List<SubscriptionDetail> sdl, Component parent) throws Exception {
        SubscriptionsDialog d = new SubscriptionsDialog();
        d.sdl = sdl;
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d;
    }

    private SubscriptionsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Select Subscriptions");

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


        DefaultTableModel model = new SubscriptionTableModel();
        model.addColumn("");
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");
        table.setModel(model);
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);



        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSubscriptions();
            }
        });
    }

    private void refreshSubscriptions() {
        try {
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }
            Project project = null;
            final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();
            subscriptionManager.cleanSubscriptions();

            DefaultTableModel dm = (DefaultTableModel)table.getModel();
            dm.getDataVector().removeAllElements();
            dm.fireTableDataChanged();

            SelectSubscriptionsAction.updateSubscriptionWithProgressDialog(subscriptionManager, project);
            sdl = subscriptionManager.getSubscriptionDetails();
            setSubscriptions();
            subscriptionManager.setSubscriptionDetails(sdl);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("onShowSubscriptions", ex);
            ErrorWindow.show(ex.getMessage(), "Select Subscriptions Action Error", SubscriptionsDialog.this);
        }

    }

    private void setSubscriptions() {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        for (SubscriptionDetail sd : sdl) {
            model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
            model.fireTableDataChanged();
        }
    }

    private void onOK() {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
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
        dispose();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        table = new JBTable();
    }
}
