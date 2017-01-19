package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ProjectManager;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SignInWindow extends JDialog {
    private JPanel contentPane;

    private JRadioButton interactiveRadioButton;
    private JTextPane interactiveCommentTextPane;

    private JPanel buttonsPanel;
    private JRadioButton automatedRadioButton;
    private JTextPane automatedCommentTextPane;
    private JLabel authFileLabel;
    private JTextField authFileTextField;
    private JButton browseButton;
    private JButton createNewAuthenticationFileButton;

    private JButton buttonOK;
    private JButton buttonCancel;

    private AuthMethodDetails authMethodDetails;
    private AuthMethodDetails authMethodDetailsResult;

    private String accountEmail;
    private int result = JOptionPane.CANCEL_OPTION;

    final JFileChooser fileChooser;

    public int getResult() {
        return result;
    }

    public AuthMethodDetails getAuthMethodDetails() {
        return authMethodDetailsResult;
    }

    public static SignInWindow go(AuthMethodDetails authMethodDetails, Component parent) {
        SignInWindow d = new SignInWindow(authMethodDetails);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d;
    }

    public SignInWindow(AuthMethodDetails authMethodDetails) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Azure Sign In");

        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        FileFilter filter = new FileNameExtensionFilter("*.azureauth", "azureauth");
        fileChooser.setFileFilter(filter);
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setDialogTitle("Select Authenticated File");

        this.authMethodDetails = authMethodDetails;
        authFileTextField.setText(authMethodDetails.getCredFilePath());


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

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        interactiveRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onInteractiveRadioButton();
            }
        });

        automatedRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAutomatedRadioButton();
            }
        });

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSelectCredFilepath();
            }
        });

        createNewAuthenticationFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCreateServicePrincipal();
            }
        });

        interactiveRadioButton.setSelected(true);
        onInteractiveRadioButton();
    }

    private void onOK() {
        authMethodDetailsResult = new AuthMethodDetails();
        if (interactiveRadioButton.isSelected()) {
            doSignIn();
            if (StringUtils.isNullOrEmpty(accountEmail)) {
                System.out.println("Canceled by the user.");
                return;
            }
            authMethodDetailsResult.setAuthMethod(AuthMethod.AD);
            authMethodDetailsResult.setAccountEmail(accountEmail);
        } else { // automated
            String authPath = authFileTextField.getText();
            if (StringUtils.isNullOrWhiteSpace(authPath)) {
                JOptionPane.showMessageDialog(this,
                        "Please select authentication file",
                        "Sing in dialog info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            authMethodDetailsResult.setAuthMethod(AuthMethod.SP);
            // TODO: check field is empty, check file is valid
            authMethodDetailsResult.setCredFilePath(authPath);

        }
        result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        // use initial
        authMethodDetailsResult = authMethodDetails;
        dispose();
    }

    private void onInteractiveRadioButton() {
        enableAutomatedAuthControls(false);
    }

    private void onAutomatedRadioButton() {
        enableAutomatedAuthControls(true);
    }

    private void enableAutomatedAuthControls(boolean enabled) {
        interactiveCommentTextPane.setEnabled(!enabled);
        automatedCommentTextPane.setEnabled(enabled);
        authFileLabel.setEnabled(enabled);
        authFileTextField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        createNewAuthenticationFileButton.setEnabled(enabled);
    }

    private void doSelectCredFilepath() {
        int returnVal = fileChooser.showOpenDialog(SignInWindow.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String filepath = file.getCanonicalPath();
                //setCredFilepath(filepath.toString());
                authFileTextField.setText(filepath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void doSignIn() {
        try {
            AdAuthManager adAuthManager = AdAuthManager.getInstance();
            if (adAuthManager.isSignedIn()) {
                doSingOut();
            }
            signInAsync();
            accountEmail = adAuthManager.getAccountEmail();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void signInAsync() {
        ProgressManager.getInstance().run(
            new Task.Modal(ProjectManager.getInstance().getDefaultProject(), "Signing in...", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    try {
                        AdAuthManager.getInstance().signIn();
                    } catch (Exception ex) {
                        System.out.println("signInAsync ex: " + ex.getMessage());
                    }
                }
            }
        );
    }

    private void doSingOut() {
        try {
            accountEmail = null;
            AdAuthManager.getInstance().signOut();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void doCreateServicePrincipal() {
        AdAuthManager adAuthManager = null;
        try {
            adAuthManager = AdAuthManager.getInstance();
            if (adAuthManager.isSignedIn()) {
                adAuthManager.signOut();
            }

            signInAsync();

            if (!adAuthManager.isSignedIn()) {
                // canceled by the user
                System.out.println(">> Canceled by the user");
                return;
            }

            AccessTokenAzureManager accessTokenAzureManager = new AccessTokenAzureManager();
            SubscriptionManager subscriptionManager = accessTokenAzureManager.getSubscriptionManager();

            ProgressManager.getInstance().run(new Task.Modal(ProjectManager.getInstance().getDefaultProject(), "Loading Subscriptions...", true) {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    try {
                        progressIndicator.setIndeterminate(true);
                        subscriptionManager.getSubscriptionDetails();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            SrvPriSettingsDialog d = SrvPriSettingsDialog.go(subscriptionManager.getSubscriptionDetails(), this);
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            String destinationFolder;
            if (d.getResult() == JOptionPane.OK_OPTION) {
                subscriptionDetailsUpdated = d.getSubscriptionDetails();
                destinationFolder = d.getDestinationFolder();
            } else {
                System.out.println(">> Canceled by the user");
                return;
            }

            Map<String, List<String>> tidSidsMap = new HashMap<>();
            for (SubscriptionDetail sd : subscriptionDetailsUpdated) {
                if (sd.isSelected()) {
                    System.out.format(">> %s\n", sd.getSubscriptionName());
                    String tid = sd.getTenantId();
                    List<String> sidList;
                    if (!tidSidsMap.containsKey(tid)) {
                        sidList = new LinkedList<>();
                    } else {
                        sidList = tidSidsMap.get(tid);
                    }
                    sidList.add(sd.getSubscriptionId());
                    tidSidsMap.put(tid, sidList);
                }
            }

            SrvPriCreationStatusDialog  d1 = SrvPriCreationStatusDialog.go(tidSidsMap, destinationFolder, this);
            if (d1.getResult() != JOptionPane.OK_OPTION) {
                System.out.println(">> Canceled by the user");
                return;
            }

            String path = d1.getSelectedAuthFilePath();
            if (path == null) {
                System.out.println(">> No file was created");
                return;
            }

            authFileTextField.setText(path);
            fileChooser.setCurrentDirectory(new File(destinationFolder));


        } catch (Exception ex) {
            System.out.println("doCreateServicePrincipal ex:");
            ex.printStackTrace();
        } finally {
            if (adAuthManager != null) {
                try {
                    System.out.println(">> Signing out...");
                    adAuthManager.signOut();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}