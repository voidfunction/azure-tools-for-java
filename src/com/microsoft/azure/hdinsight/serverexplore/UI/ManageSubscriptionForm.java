package com.microsoft.azure.hdinsight.serverexplore.UI;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.hdinsight.serverexplore.HDExploreException;
import com.microsoft.azure.hdinsight.common.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.subscription.Subscription;
import com.microsoft.azure.hdinsight.serverexplore.AzureManager;
import com.microsoft.azure.hdinsight.serverexplore.AzureManagerImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

public class ManageSubscriptionForm extends DialogWrapper {
    private JPanel mainPanel;
    private JTable subscriptionTable;
    private JButton signInButton;
    private JButton removeButton;
    private JButton closeButton;
    private java.util.List<Subscription> subscriptionList;
    private Project project;

    public ManageSubscriptionForm(final Project project) {
        super(project, true);

        this.project = project;

        this.setTitle("Manage Subscriptions");
        this.setModal(true);

        final ManageSubscriptionForm form = this;

        final DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return (col == 0);
            }

            public Class<?> getColumnClass(int colIndex) {
                return getValueAt(0, colIndex).getClass();
            }
        };

        model.addColumn("");
        model.addColumn("Name");
        model.addColumn("Id");

        subscriptionTable.setModel(model);

        TableColumn column = subscriptionTable.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);

        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (AzureManagerImpl.getManager().authenticated()) {
                        clearSubscriptions(false);
                    } else {
                        form.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        AzureManager apiManager = AzureManagerImpl.getManager();
                        apiManager.authenticate();

                        refreshSignInCaption();
                        loadList();
                    }
                } catch (HDExploreException e1) {
                    DefaultLoader.getUIHelper().showException("An error occurred while attempting to sign in to " +
                                    "your account.", e1,
                            "HDInsight  Explorer - Error Signing In", false, true);
                } finally {
                    form.getWindow().setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                clearSubscriptions(true);
            }
        });

        removeButton.setEnabled(false);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancelAction();
            }
        });

        init();

        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                loadList();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    private void refreshSignInCaption() {
        boolean isNotSigned = !AzureManagerImpl.getManager().authenticated();
        signInButton.setText(isNotSigned ? "Sign In ..." : "Sign Out");
    }

    private void clearSubscriptions(boolean isSigningOut) {
        int res = JOptionPane.showConfirmDialog(mainPanel, (isSigningOut
                        ? "Are you sure you would like to clear all subscriptions?"
                        : "Are you sure you would like to sign out?"),
                (isSigningOut
                        ? "Clear Subscriptions"
                        : "Sign out"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (res == JOptionPane.YES_OPTION) {
            AzureManager apiManager = AzureManagerImpl.getManager();
            apiManager.clearAuthentication();

            DefaultTableModel model = (DefaultTableModel) subscriptionTable.getModel();

            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }

            ApplicationManager.getApplication().saveSettings();

            removeButton.setEnabled(false);

            refreshSignInCaption();
        }
    }

    private void loadList() {
        final ManageSubscriptionForm form = this;
        final DefaultTableModel model = (DefaultTableModel) subscriptionTable.getModel();

        refreshSignInCaption();

        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

        form.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Vector<Object> vector = new Vector<Object>();
        vector.add("");
        vector.add("(loading... )");
        vector.add("");
        model.addRow(vector);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                while (model.getRowCount() > 0) {
                    model.removeRow(0);
                }

                subscriptionList = AzureManagerImpl.getManager().getFullSubscriptionList();

                if (subscriptionList.size() > 0) {
                    for (Subscription subs : subscriptionList) {
                        Vector<Object> row = new Vector<Object>();
                        row.add(subs.isSelected());
                        row.add(subs.getDisplayName());
                        row.add(subs.getSubscriptionId());
                        model.addRow(row);
                    }

                    removeButton.setEnabled(true);
                } else {
                    removeButton.setEnabled(false);
                }

                form.getWindow().setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

    @Override
    public void doCancelAction() {
        try {
            java.util.List<String> selectedList = new ArrayList<String>();

            TableModel model = subscriptionTable.getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean selected = (Boolean) model.getValueAt(i, 0);

                if (selected) {
                    selectedList.add(model.getValueAt(i, 2).toString());
                }
            }

            AzureManagerImpl.getManager().setSelectedSubscriptions(selectedList);

            //Saving the project is necessary to save the changes on the PropertiesComponent
            if (project != null) {
                project.save();
            }

            close(DialogWrapper.CANCEL_EXIT_CODE, false);
        } catch (HDExploreException e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to set the selected " +
                            "subscriptions.", e,
                    "Azure Services Explorer - Error Setting Selected Subscriptions", false, true);
        }
    }


    private void createUIComponents() {
        subscriptionTable = new JBTable();
    }
}