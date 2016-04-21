/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.forms;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.table.JBTable;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.ui.AppInsightsMngmtPanel;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishProfile;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ManageSubscriptionPanel implements AzureAbstractPanel {
    private static final String DISPLAY_NAME = "Manage Subscriptions";
    private JPanel mainPanel;
    private JTable subscriptionTable;
    private JButton signInButton;
    private JButton removeButton;
    private JButton importSubscriptionButton;
    private JButton closeButton;
    private java.util.List<Subscription> subscriptionList;
    private Project project;
    private DialogWrapper myDialog;
    boolean needBtnImpFrmPubSetFile;

    public ManageSubscriptionPanel(final Project project, boolean needBtnImpFrmPubSetFile) {
        this.project = project;
        this.needBtnImpFrmPubSetFile = needBtnImpFrmPubSetFile;
        if (!needBtnImpFrmPubSetFile) {
            importSubscriptionButton.setEnabled(false);
            removeButton.setEnabled(false);
        }

        final ManageSubscriptionPanel form = this;

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
                        myDialog.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        AzureManager apiManager = AzureManagerImpl.getManager();
                        apiManager.clearImportedPublishSettingsFiles();
                        apiManager.authenticate();

                        refreshSignInCaption();
                        loadList();

                        WizardCacheManager.getPublishDatas().clear();
                        List<Subscription> subscriptions = apiManager.getSubscriptionList();
                        PublishData pd  = new PublishData();
                        PublishProfile publishProfile = new PublishProfile();
                        pd.setPublishProfile(publishProfile);
                        Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
                        for (Subscription subscription : subscriptions) {
                            com.microsoftopentechnologies.azuremanagementutil.model.Subscription profileSubscription =
                                    new com.microsoftopentechnologies.azuremanagementutil.model.Subscription();
                            profileSubscription.setSubscriptionID(subscription.getId());
                            profileSubscription.setSubscriptionName(subscription.getName());
                            publishProfile.getSubscriptions().add(profileSubscription);
                            List<String> resList = apiManager.getResourceGroupNames(subscription.getId());
                            for (String res : resList) {
                                List<WebSite> webList = apiManager.getWebSites(subscription.getId(), res);
                                for (WebSite webSite : webList) {
                                    WebSiteConfiguration webSiteConfiguration = apiManager.
                                            getWebSiteConfiguration(webSite.getSubscriptionId(),
                                                    webSite.getWebSpaceName(), webSite.getName());
                                    webSiteConfigMap.put(webSite, webSiteConfiguration);
                                }
                            }
                        }
                        AzureSettings.getSafeInstance(project).saveWebApps(webSiteConfigMap);
                        AzureSettings.getSafeInstance(project).setwebAppLoaded(true);
                        AppInsightsMngmtPanel.updateApplicationInsightsResourceRegistry(subscriptions);
                        pd.setCurrentSubscription(publishProfile.getSubscriptions().get(0));
                        try {
                            WizardCacheManager.cachePublishData(null, pd, null);
                            AzureSettings.getSafeInstance(project).savePublishDatas();
                        } catch (RestAPIException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } catch (Exception e1) {
                    PluginUtil.displayErrorDialogAndLog(message("signInErr"), e1.getMessage(), e1);
                } finally {
                    myDialog.getWindow().setCursor(Cursor.getDefaultCursor());
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

        importSubscriptionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportSubscriptionForm isf = new ImportSubscriptionForm(project);
                isf.setOnSubscriptionLoaded(new Runnable() {
                    @Override
                    public void run() {
                        AzureSettings.getSafeInstance(project).saveWebApps(new HashMap<WebSite, WebSiteConfiguration>());
                        AzureSettings.getSafeInstance(project).setwebAppLoaded(false);
                        AppInsightsMngmtPanel.keeepManuallyAddedList();
                        loadList();
                    }
                });
                isf.show();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancelAction();
            }
        });

        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                loadList();
            }
        });
    }

    public void setDialog(DialogWrapper myDialog) {
        this.myDialog = myDialog;
    }

    @Nullable
//    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    private void refreshSignInCaption() {
        boolean isNotSigned = !AzureManagerImpl.getManager().authenticated();

        signInButton.setText(isNotSigned ? "Sign In..." : "Sign Out");
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
            apiManager.clearImportedPublishSettingsFiles();
            WizardCacheManager.getPublishDatas().clear();
            AzureSettings.getSafeInstance(project).savePublishDatas();
            AzureSettings.getSafeInstance(project).saveWebApps(new HashMap<WebSite, WebSiteConfiguration>());
            AzureSettings.getSafeInstance(project).setwebAppLoaded(false);
            AppInsightsMngmtPanel.keeepManuallyAddedList();
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
        final DefaultTableModel model = (DefaultTableModel) subscriptionTable.getModel();

        refreshSignInCaption();

        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }

//        myDialog.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Vector<Object> vector = new Vector<Object>();
        vector.add("");
        vector.add("(loading... )");
        vector.add("");
        model.addRow(vector);

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    while (model.getRowCount() > 0) {
                        model.removeRow(0);
                    }

                    subscriptionList = AzureManagerImpl.getManager().getFullSubscriptionList();

                    if (subscriptionList.size() > 0) {
                        for (Subscription subs : subscriptionList) {
                            Vector<Object> row = new Vector<Object>();
                            row.add(subs.isSelected());
                            row.add(subs.getName());
                            row.add(subs.getId());
                            model.addRow(row);
                        }
                        if (needBtnImpFrmPubSetFile) {
                            removeButton.setEnabled(true);
                        }
                    } else {
                        removeButton.setEnabled(false);
                    }

//                    myDialog.getWindow().setCursor(Cursor.getDefaultCursor());
                } catch (AzureCmdException e) {
//                    myDialog.getWindow().setCursor(Cursor.getDefaultCursor());
                    String msg = "An error occurred while attempting to get the subscription list." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }
            }
        });
    }

//    @Override
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
//            if (project != null) {
//                project.save();
//            }
            if (myDialog != null) {
                myDialog.close(DialogWrapper.CANCEL_EXIT_CODE, false);
            }
        } catch (AzureCmdException e) {
            String msg = "An error occurred while attempting to set the selected subscriptions." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
        }
    }


    private void createUIComponents() {
        subscriptionTable = new JBTable();
    }

    @Override
    public JComponent getPanel() {
        return mainPanel;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        return false;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public JButton getSignInButton() {
        return signInButton;
    }
}
