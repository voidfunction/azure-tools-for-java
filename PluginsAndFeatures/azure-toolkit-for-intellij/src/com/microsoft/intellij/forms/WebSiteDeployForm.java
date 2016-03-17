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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.components.DefaultDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.WizardCacheManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ArtifactDescriptor;
import com.microsoft.tooling.msservices.helpers.IDEHelper.ProjectDescriptor;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask;
import com.microsoft.tooling.msservices.helpers.tasks.CancellableTask.CancellableTaskHandle;
import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebSiteDeployForm extends DialogWrapper {
    private JPanel mainPanel;
    private JList webSiteJList;
    private JButton buttonEditSubscriptions;
    private JButton buttonAddApp;
    private JCheckBox chkBoxDeployRoot;
    private JButton buttonDel;
    private Project project;
    private WebSite selectedWebSite;
    private CancellableTaskHandle fillListTaskHandle;
    private String nameToSelect = "";
    List<Subscription> subscriptionList = new ArrayList<Subscription>();
    List<WebSite> webSiteList = new ArrayList<WebSite>();
    Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();

    public WebSiteDeployForm(@org.jetbrains.annotations.Nullable Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.project = project;
        setTitle(message("webAppTtl"));
        buttonAddApp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createWebApp();
            }
        });
        buttonDel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteWebApp();
            }
        });
        buttonEditSubscriptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (subscriptionList.isEmpty()) {
                    editSubscriptions(true);
                } else {
                    editSubscriptions(false);
                }
            }
        });
        init();
        fillList();
    }

    void deleteWebApp() {
        if (selectedWebSite != null) {
            String name = selectedWebSite.getName();
            int choice = Messages.showOkCancelDialog(String.format(message("delMsg"), name), message("delTtl"), Messages.getQuestionIcon());
            if (choice == Messages.OK) {
                try {
                    AzureManagerImpl.getManager().deleteWebSite(selectedWebSite.getSubscriptionId(),
                            selectedWebSite.getWebSpaceName(), name);
                    webSiteList.remove(webSiteJList.getSelectedIndex());
                    webSiteConfigMap.remove(selectedWebSite);
                    selectedWebSite = null;
                    if (webSiteConfigMap.isEmpty()) {
                        setMessages("There are no Azure web apps in the imported subscriptions.");
                    } else {
                        setWebApps(webSiteConfigMap);
                    }
                } catch (AzureCmdException e) {
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), message("delWebErr"), e);
                }
            }
        } else {
            PluginUtil.displayErrorDialog(message("errTtl"), "Select a web app container to delete.");
        }
    }

    @Override
    protected boolean postponeValidation() {
        return true;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected ValidationInfo doValidate() {
        return (selectedWebSite != null && isDeployable(webSiteConfigMap, webSiteList, webSiteJList.getSelectedIndex())) ? null : new ValidationInfo("Select a valid web app container as the target for the deployment.", webSiteJList);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected void dispose() {
        if (fillListTaskHandle != null && !fillListTaskHandle.isFinished()) {
            fillListTaskHandle.cancel();
        }
        super.dispose();
    }

    public String deploy() throws AzureCmdException {
        String url = "";
        ProjectDescriptor projectDescriptor = new ProjectDescriptor(project.getName(),
                project.getBasePath() == null ? "" : project.getBasePath());
        WebSite webSite = this.selectedWebSite;

        AzureManager manager = AzureManagerImpl.getManager();
        ArtifactDescriptor artifactDescriptor = manager.getWebArchiveArtifact(projectDescriptor);
        if (artifactDescriptor != null && webSite != null) {
            manager.deployWebArchiveArtifact(projectDescriptor, artifactDescriptor, webSite, chkBoxDeployRoot.isSelected());
            WebSitePublishSettings webSitePublishSettings = manager.
                    getWebSitePublishSettings(webSite.getSubscriptionId(), webSite.getWebSpaceName(), webSite.getName());
            WebSitePublishSettings.PublishProfile profile = webSitePublishSettings.getPublishProfileList().get(0);
            if (profile != null) {
                url = profile.getDestinationAppUrl();
                if (!chkBoxDeployRoot.isSelected()) {
                    url = url + "/" + artifactDescriptor.getName();
                }
            }
        }
        return url;
    }

    private void fillList() {
        if (fillListTaskHandle != null && !fillListTaskHandle.isFinished()) {
            fillListTaskHandle.cancel();
        }

        webSiteJList.setModel(getMessageListModel("(Loading Web Apps...)"));

        for (ListSelectionListener selectionListener : webSiteJList.getListSelectionListeners()) {
            webSiteJList.removeListSelectionListener(selectionListener);
        }

        ProjectDescriptor projectDescriptor = new ProjectDescriptor(project.getName(),
                project.getBasePath() == null ? "" : project.getBasePath());

        try {
            fillListTaskHandle = DefaultLoader.getIdeHelper().runInBackground(projectDescriptor, "Retrieving web apps info...", null, new CancellableTask() {
                final AzureManager manager = AzureManagerImpl.getManager();
                final Object lock = new Object();

                CancellationHandle cancellationHandle;

                @Override
                public synchronized void run(final CancellationHandle cancellationHandle) throws Throwable {
                    this.cancellationHandle = cancellationHandle;
                    subscriptionList = manager.getSubscriptionList();
                    webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
                    if (subscriptionList.size() > 0) {
                        if (manager.authenticated()) {
                            // authenticated using AD. Proceed for Web Apps retrieval
                            List<ListenableFuture<Void>> subscriptionFutures = new ArrayList<ListenableFuture<Void>>();

                            for (final Subscription subscription : subscriptionList) {
                                if (cancellationHandle.isCancelled()) {
                                    return;
                                }

                                final SettableFuture<Void> subscriptionFuture = SettableFuture.create();

                                DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadWebSiteConfigurations(subscription, subscriptionFuture);
                                    }
                                });

                                subscriptionFutures.add(subscriptionFuture);
                            }

                            try {
                                Futures.allAsList(subscriptionFutures).get();
                            } catch (InterruptedException e) {
                                throw new AzureCmdException(e.getMessage(), e);
                            } catch (ExecutionException e) {
                                throw new AzureCmdException(e.getCause().getMessage(), e.getCause());
                            }
                        } else {
                            // imported publish settings file. Clear subscription
                            manager.clearImportedPublishSettingsFiles();
                            WizardCacheManager.clearSubscriptions();
                            subscriptionList = manager.getSubscriptionList();
                        }
                    }
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onSuccess() {
                    DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (subscriptionList.isEmpty()) {
                                setMessages("Please sign in to import your Azure subscriptions. The credentials in a publish settings file are not sufficient for the web app functionality.");
                                selectedWebSite = null;
                                editSubscriptions(true);
                            } else if (webSiteConfigMap.isEmpty()) {
                                setMessages("There are no Azure web apps in the imported subscriptions.");
                                selectedWebSite = null;
                            } else {
                                setWebApps(webSiteConfigMap);
                            }
                        }
                    });
                }

                @Override
                public void onError(@NotNull Throwable throwable) {
                    DefaultLoader.getUIHelper().showException("An error occurred while trying to load the web apps info",
                            throwable, "Azure Services Explorer - Error Loading Web Apps Info", false, true);
                }

                private void loadWebSiteConfigurations(final Subscription subscription,
                                                       final SettableFuture<Void> subscriptionFuture) {
                    try {
                        List<ListenableFuture<Void>> webSpaceFutures = new ArrayList<ListenableFuture<Void>>();

                        for (final String webSpace : manager.getResourceGroupNames(subscription.getId())) {
                            if (cancellationHandle.isCancelled()) {
                                subscriptionFuture.set(null);
                                return;
                            }

                            final SettableFuture<Void> webSpaceFuture = SettableFuture.create();

                            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadWebSiteConfigurations(subscription, webSpace, webSpaceFuture);
                                }
                            });

                            webSpaceFutures.add(webSpaceFuture);
                        }

                        Futures.addCallback(Futures.allAsList(webSpaceFutures), new FutureCallback<List<Void>>() {
                            @Override
                            public void onSuccess(List<Void> voids) {
                                subscriptionFuture.set(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                subscriptionFuture.setException(throwable);
                            }
                        });
                    } catch (AzureCmdException ex) {
                        subscriptionFuture.setException(ex);
                    }
                }

                private void loadWebSiteConfigurations(final Subscription subscription, final String webSpace,
                                                       final SettableFuture<Void> webSpaceFuture) {
                    try {
                        List<ListenableFuture<Void>> webSiteFutures = new ArrayList<ListenableFuture<Void>>();

                        for (final WebSite webSite : manager.getWebSites(subscription.getId(), webSpace)) {
                            if (cancellationHandle.isCancelled()) {
                                webSpaceFuture.set(null);
                                return;
                            }

                            final SettableFuture<Void> webSiteFuture = SettableFuture.create();

                            DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadWebSiteConfigurations(subscription, webSpace, webSite, webSiteFuture);
                                }
                            });

                            webSiteFutures.add(webSiteFuture);
                        }

                        Futures.addCallback(Futures.allAsList(webSiteFutures), new FutureCallback<List<Void>>() {
                            @Override
                            public void onSuccess(List<Void> voids) {
                                webSpaceFuture.set(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                webSpaceFuture.setException(throwable);
                            }
                        });
                    } catch (AzureCmdException ex) {
                        webSpaceFuture.setException(ex);
                    }
                }

                private void loadWebSiteConfigurations(final Subscription subscription, final String webSpace,
                                                       final WebSite webSite,
                                                       final SettableFuture<Void> webSiteFuture) {
                    WebSiteConfiguration webSiteConfiguration;

                    try {
                        webSiteConfiguration = AzureManagerImpl.getManager().
                                getWebSiteConfiguration(webSite.getSubscriptionId(),
                                        webSite.getWebSpaceName(), webSite.getName());
                    } catch (Throwable ignore) {
                        webSiteConfiguration = new WebSiteConfiguration(webSpace, webSite.getName(),
                                subscription.getId());
                    }

                    synchronized (lock) {
                        webSiteConfigMap.put(webSite, webSiteConfiguration);
                    }

                    webSiteFuture.set(null);
                }
            });
        } catch (AzureCmdException e) {
            selectedWebSite = null;
            DefaultLoader.getUIHelper().showException("An error occurred while trying to load the web apps info",
                    e, "Azure Services Explorer - Error Loading Web Apps Info", false, true);
        }
    }

    private void setWebApps(@NotNull final Map<WebSite, WebSiteConfiguration> webSiteConfigMap) {
        webSiteList = new ArrayList<WebSite>(webSiteConfigMap.keySet());
        Collections.sort(webSiteList, new Comparator<WebSite>() {
            @Override
            public int compare(WebSite ws1, WebSite ws2) {
                return ws1.getName().compareTo(ws2.getName());
            }
        });

        selectedWebSite = null;

        webSiteJList.setModel(new AbstractListModel() {
            @Override
            public int getSize() {
                return webSiteList.size();
            }

            @Override
            public Object getElementAt(int i) {
                WebSite webSite = webSiteList.get(i);
                WebSiteConfiguration webSiteConfiguration = webSiteConfigMap.get(webSite);

                StringBuilder builder = new StringBuilder(webSite.getName());

                if (!webSiteConfiguration.getJavaVersion().isEmpty()) {
                    builder.append(" (JRE ");
                    builder.append(webSiteConfiguration.getJavaVersion());

                    if (!webSiteConfiguration.getJavaContainer().isEmpty()) {
                        builder.append("; ");
                        builder.append(webSiteConfiguration.getJavaContainer());
                        builder.append(" ");
                        builder.append(webSiteConfiguration.getJavaContainerVersion());
                    }
                    builder.append(")");
                } else {
                    builder.append(" (.NET ");
                    builder.append(webSiteConfiguration.getNetFrameworkVersion());

                    if (!webSiteConfiguration.getPhpVersion().isEmpty()) {
                        builder.append("; PHP ");
                        builder.append(webSiteConfiguration.getPhpVersion());
                    }
                    builder.append(")");
                }
                return builder.toString();
            }
        });

        webSiteJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                int index = webSiteJList.getSelectedIndex();
                if (index >= 0 && webSiteList.size() > index) {
                    selectedWebSite = webSiteList.get(index);
                } else {
                    selectedWebSite = null;
                }
            }
        });

        webSiteJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel jLabel = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Border border = jLabel.getBorder();
                Border margin = new EmptyBorder(0, 2, 0, 2);
                jLabel.setBorder(new CompoundBorder(border, margin));

                if (!isDeployable(webSiteConfigMap, webSiteList, index)) {
                    jLabel.setBackground(list.getBackground());
                    jLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                }

                return jLabel;
            }
        });

        // select newly created azure web app
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!nameToSelect.isEmpty()) {
                    for (int i = 0; i < webSiteList.size(); i++) {
                        WebSite website = webSiteList.get(i);
                        if (website.getName().equalsIgnoreCase(nameToSelect)) {
                            webSiteJList.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        }, ModalityState.defaultModalityState());
    }

    private static boolean isDeployable(@NotNull Map<WebSite, WebSiteConfiguration> webSiteConfigMap,
                                        @NotNull List<WebSite> webSiteList,
                                        int index) {
        return index >= 0 && webSiteList.size() > index && !webSiteConfigMap.get(webSiteList.get(index)).getJavaContainer().isEmpty();
    }

    private void setMessages(String... messages) {
        webSiteJList.setModel(getMessageListModel(messages));
    }

    @NotNull
    private static AbstractListModel getMessageListModel(final String... messages) {
        return new AbstractListModel() {
            @Override
            public int getSize() {
                return messages.length;
            }

            @Override
            public Object getElementAt(int index) {
                return messages[index];
            }
        };
    }

    private void createWebApp() {
        CreateWebSiteForm form = new CreateWebSiteForm(project, webSiteList);
        form.show();

        if (form.isOK()) {
            nameToSelect = form.getWebAppCreated();
            fillList();
        }
    }

    private void editSubscriptions(boolean invokeSignIn) {
        try {
            final ManageSubscriptionPanel manageSubscriptionPanel = new ManageSubscriptionPanel(project, false);
            final DefaultDialogWrapper subscriptionsDialog = new DefaultDialogWrapper(project, manageSubscriptionPanel) {
                @Nullable
                @Override
                protected JComponent createSouthPanel() {
                    return null;
                }
                @Override
                protected JComponent createTitlePane() {
                    return null;
                }
            };
            manageSubscriptionPanel.setDialog(subscriptionsDialog);
            JButton signInBtn = manageSubscriptionPanel.getSignInButton();
            if (invokeSignIn && signInBtn != null && signInBtn.getText().equalsIgnoreCase("Sign In...")) {
                signInBtn.doClick();
            }
            subscriptionsDialog.show();

            subscriptionList = AzureManagerImpl.getManager().getSubscriptionList();
            if (subscriptionList.size() == 0) {
                setMessages("Please sign in to import your Azure subscriptions. The credentials in a publish settings file are not sufficient for the web app functionality.");
                selectedWebSite = null;
            } else {
                fillList();
            }
        } catch (AzureCmdException e) {
            setMessages("There has been an error while retrieving the configured Azure subscriptions.",
                    "Please retry signing in/importing your Azure subscriptions.");
        }
    }

    public WebSite getSelectedWebSite() {
        return selectedWebSite;
    }
}