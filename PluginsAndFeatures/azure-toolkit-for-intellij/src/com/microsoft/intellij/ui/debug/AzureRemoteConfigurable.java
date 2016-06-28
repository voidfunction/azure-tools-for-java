/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Class RemoteConfigurable
 * @author Jeka
 */
package com.microsoft.intellij.ui.debug;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.forms.WebSiteDeployForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tasks.WebSiteDeployTask;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSiteConfiguration;
import com.microsoftopentechnologies.azurecommons.util.WAEclipseHelperMethods;
import com.microsoftopentechnologies.azurecommons.wacommonutil.Utils;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureRemoteConfigurable extends SettingsEditor<AzureRemoteConfiguration> {
    JPanel myPanel;
    private JRadioButton myRbSocket;
    private JRadioButton myRbShmem;
    private JRadioButton myRbListen;
    private JRadioButton myRbAttach;
    private JTextField myAddressField;
    private JTextField myHostField;
    private JTextField myPortField;
    private JPanel myShmemPanel;
    private JPanel mySocketPanel;
    private LabeledComponent<ModulesComboBox> myModule;
    private String myHostName = "localhost";
    @NonNls
    protected static final String LOCALHOST = "localhost";
    private final ConfigurationModuleSelector myModuleSelector;
    private JComboBox webAppCombo;
    private JPanel webAppPanel;
    private JXHyperlink link;
    Map<WebSite, WebSiteConfiguration> webSiteConfigMap = new HashMap<WebSite, WebSiteConfiguration>();
    List<WebSite> webSiteList = new ArrayList<WebSite>();
    Project project;
    Collection<Module> modules;

    public AzureRemoteConfigurable(final Project project, final Collection<Module> modules) {
        this.project = project;
        this.modules = modules;

        final ButtonGroup transportGroup = new ButtonGroup();
        transportGroup.add(myRbSocket);
        transportGroup.add(myRbShmem);

        final ButtonGroup connectionGroup = new ButtonGroup();
        connectionGroup.add(myRbListen);
        connectionGroup.add(myRbAttach);

        final DocumentListener helpTextUpdater = new DocumentAdapter() {
            public void textChanged(DocumentEvent event) {
                updateHelpText();
            }
        };
        myAddressField.getDocument().addDocumentListener(helpTextUpdater);
        myHostField.getDocument().addDocumentListener(helpTextUpdater);
        myPortField.getDocument().addDocumentListener(helpTextUpdater);
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Object source = e.getSource();
                if (source.equals(myRbSocket)) {
                    myShmemPanel.setVisible(false);
                    mySocketPanel.setVisible(true);
                }
                else if (source.equals(myRbShmem)) {
                    myShmemPanel.setVisible(true);
                    mySocketPanel.setVisible(false);
                }
                myPanel.repaint();
                updateHelpText();
            }
        };
        myRbShmem.addActionListener(listener);
        myRbSocket.addActionListener(listener);

        final ItemListener updateListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean isAttach = myRbAttach.isSelected();
                myHostField.setEnabled(isAttach);
                myHostField.setText(LOCALHOST);
                updateHelpText();
            }
        };
        myRbAttach.addItemListener(updateListener);
        myRbListen.addItemListener(updateListener);

        final FocusListener fieldFocusListener = new FocusAdapter() {
            public void focusLost(final FocusEvent e) {
                updateHelpText();
            }
        };
        myAddressField.addFocusListener(fieldFocusListener);
        myPortField.addFocusListener(fieldFocusListener);

        if (myModule.getComponent() == null) {
            myModuleSelector = new ConfigurationModuleSelector(project, new ModulesComboBox(), "<whole project>");
        } else {
            myModuleSelector = new ConfigurationModuleSelector(project, myModule.getComponent(), "<whole project>");
        }

        // default
        myRbSocket.doClick();
        myRbAttach.doClick();
    }

    public void applyEditorTo(@NotNull final AzureRemoteConfiguration configuration) throws ConfigurationException {
        configuration.HOST = (myHostField.getText()).trim();
        if (configuration.HOST != null && configuration.HOST.isEmpty()) {
            configuration.HOST = null;
        }
        configuration.PORT = myPortField.getText().trim();
        if (configuration.PORT != null && configuration.PORT.isEmpty()) {
            configuration.PORT = null;
        }
        configuration.SHMEM_ADDRESS = myAddressField.getText().trim();
        if (configuration.SHMEM_ADDRESS != null && configuration.SHMEM_ADDRESS.isEmpty()) {
            configuration.SHMEM_ADDRESS = null;
        }
        configuration.USE_SOCKET_TRANSPORT = myRbSocket.isSelected();
        configuration.SERVER_MODE = myRbListen.isSelected();
        myModuleSelector.applyTo(configuration);
        configuration.WEBAPP = (String) webAppCombo.getSelectedItem();
    }

    public void resetEditorFrom(final AzureRemoteConfiguration configuration) {
        if (!SystemInfo.isWindows) {
            configuration.USE_SOCKET_TRANSPORT = true;
            myRbShmem.setEnabled(false);
            myAddressField.setEditable(false);
        }
        myAddressField.setText(configuration.SHMEM_ADDRESS);
        myHostName = LOCALHOST;
        myHostField.setText(LOCALHOST);
        if (configuration.PORT == null) {
            // new run/debug configuration
            int portToUse = getAvailablePort();
            if (portToUse == 0) {
                myPortField.setText(configuration.PORT);
            } else {
                myPortField.setText(Integer.toString(portToUse));
            }
        } else {
            myPortField.setText(configuration.PORT);
        }
        if (configuration.USE_SOCKET_TRANSPORT) {
            myRbSocket.doClick();
        }
        else {
            myRbShmem.doClick();
        }
        if (configuration.SERVER_MODE) {
            myRbListen.doClick();
        }
        else {
            myRbAttach.doClick();
        }

        myModuleSelector.reset(configuration);
        List<String> listToDisplay = loadWebApps();
        String website = configuration.WEBAPP;
        if (!listToDisplay.isEmpty()) {
            if (website != null && !website.isEmpty() && listToDisplay.contains(website)) {
                webAppCombo.setSelectedItem(website);
            } else {
                webAppCombo.setSelectedItem(listToDisplay.get(0));
            }
        }

    }

    @NotNull
    public JComponent createEditor() {
        link.setAction(createLinkAction());
        link.setVisible(true);
        return myPanel;
    }

    private void updateHelpText() {
        boolean useSockets = !myRbShmem.isSelected();
        final RemoteConnection connection = new RemoteConnection(
                useSockets,
                myHostName,
                useSockets ? myPortField.getText().trim() : myAddressField.getText().trim(),
                myRbListen.isSelected()
        );
    }

    private List<String> loadWebApps() {
        List<String> listToDisplay = new ArrayList<>();
        try {
            webSiteConfigMap = AzureSettings.getSafeInstance(project).loadWebApps();
            if (webSiteConfigMap != null) {
                // filter out Java Web Apps
                for (Iterator<Map.Entry<WebSite, WebSiteConfiguration>> it = webSiteConfigMap.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<WebSite, WebSiteConfiguration> entry = it.next();
                    if (entry.getValue().getJavaContainer().isEmpty()) {
                        it.remove();
                    }
                }
                webSiteList = new ArrayList<WebSite>(webSiteConfigMap.keySet());
                Collections.sort(webSiteList, new Comparator<WebSite>() {
                    @Override
                    public int compare(WebSite ws1, WebSite ws2) {
                        return ws1.getName().compareTo(ws2.getName());
                    }
                });

                listToDisplay = WAEclipseHelperMethods.prepareListToDisplay(webSiteConfigMap, webSiteList);
                webAppCombo.setModel(new DefaultComboBoxModel(listToDisplay.toArray(new String[listToDisplay.size()])));
                Map<String, Boolean> mp = AzureSettings.getSafeInstance(project).getWebsiteDebugPrep();
                for (WebSite webSite : webSiteList) {
                    String name = webSite.getName();
                    if (!mp.containsKey(name)) {
                        mp.put(name, false);
                    }
                }
                AzureSettings.getSafeInstance(project).setWebsiteDebugPrep(mp);
            }
        } catch (Exception e) {
            AzurePlugin.log(e.getMessage(), e);
        }
        return listToDisplay;
    }

    private Action createLinkAction() {
        return new AbstractAction("Azure Web App") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String prevSelection = (String) webAppCombo.getSelectedItem();
                Module module = PluginUtil.getSelectedModule();
                if (module == null && !modules.isEmpty()) {
                    module = modules.iterator().next();
                    WebSiteDeployForm form = new WebSiteDeployForm(module);
                    form.show();
                    if (form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                        try {
                            String url = form.deploy();
                            WebSiteDeployTask task = new WebSiteDeployTask(project, form.getSelectedWebSite(), url);
                            task.queue();
                        } catch (AzureCmdException ex) {
                            PluginUtil.displayErrorDialogAndLog(message("webAppDplyErr"), ex.getMessage(), ex);
                        }
                    }
                    List<String> listToDisplay = loadWebApps();
                    if (!listToDisplay.isEmpty()) {
                        if (!prevSelection.isEmpty() && listToDisplay.contains(prevSelection)) {
                            webAppCombo.setSelectedItem(prevSelection);
                        } else {
                            webAppCombo.setSelectedItem(listToDisplay.get(0));
                        }
                    }
                } else {
                    Messages.showErrorDialog(message("noModule"), message("error"));
                }
            }
        };
    }

    private int getAvailablePort() {
        int defaultPort = 49876;
        try {
            while (!Utils.isPortAvailable(defaultPort) && defaultPort < 65535) {
                defaultPort = defaultPort + 1;
            }
        } catch(Exception ex) {
            defaultPort = 0;
        }
        return defaultPort;
    }
}