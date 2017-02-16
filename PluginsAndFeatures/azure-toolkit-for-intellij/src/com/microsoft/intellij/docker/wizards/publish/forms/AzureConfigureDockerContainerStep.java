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
package com.microsoft.intellij.docker.wizards.publish.forms;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.KnownDockerImages;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardStep;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class AzureConfigureDockerContainerStep extends AzureSelectDockerWizardStep {
  private static final Logger LOGGER = Logger.getInstance(AzureConfigureDockerContainerStep.class);

  private AzureSelectDockerWizardModel model;
  private JPanel rootConfigureContainerPanel;
  private JTextField dockerContainerNameTextField;
  private JTextField dockerContainerPortSettings;
  private JRadioButton predefinedDockerfileRadioButton;
  private JRadioButton customDockerfileRadioButton;
  private ButtonGroup selectContainerButtonGroup;
  private JComboBox<KnownDockerImages> dockerfileComboBox;
  private TextFieldWithBrowseButton customDockerfileBrowseButton;
  private JLabel dockerContainerNameLabel;
  private JLabel customDockerfileBrowseLabel;
  private JLabel dockerContainerPortSettingsLabel;

  private AzureDockerHostsManager dockerManager;
  private AzureDockerImageInstance dockerImageDescription;

  public AzureConfigureDockerContainerStep(String title, AzureSelectDockerWizardModel model, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageInstance) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure the Docker container to be created");
    this.model = model;
    this.dockerManager = dockerManager;
    this.dockerImageDescription = dockerImageInstance;

    initUI();
  }

  private void initUI() {
    dockerContainerNameTextField.setText(dockerImageDescription.dockerContainerName);
    dockerContainerNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerContainerNameTip());
    dockerContainerNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerImageName(((JTextField) input).getText())) {
          dockerContainerNameLabel.setVisible(false);
          return true;
        } else {
          dockerContainerNameLabel.setVisible(true);
          return false;
        }
      }
    });
    dockerContainerNameLabel.setVisible(false);

    selectContainerButtonGroup = new ButtonGroup();
    selectContainerButtonGroup.add(predefinedDockerfileRadioButton);
    selectContainerButtonGroup.add(customDockerfileRadioButton);

    predefinedDockerfileRadioButton.setSelected(true);
    dockerfileComboBox.setEnabled(true);

    for (KnownDockerImages image : dockerManager.getDefaultDockerImages()) {
      dockerfileComboBox.addItem(image);
    }

    customDockerfileBrowseButton.setEnabled(false);
    customDockerfileBrowseButton.addActionListener(UIUtils.createFileChooserListener(customDockerfileBrowseButton, model.getProject(),
        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    customDockerfileBrowseButton.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerfilePath(customDockerfileBrowseButton.getText())) {
          customDockerfileBrowseLabel.setVisible(false);
          setFinishButtonState(doValidate(false) == null);
          return true;
        } else {
          customDockerfileBrowseLabel.setVisible(true);
          setFinishButtonState(false);
          return false;
        }
      }
    });
    customDockerfileBrowseLabel.setVisible(false);
    customDockerfileBrowseButton.setText("");

    predefinedDockerfileRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerfileComboBox.setEnabled(true);
        customDockerfileBrowseLabel.setVisible(false);
        customDockerfileBrowseButton.setEnabled(false);
        setFinishButtonState(doValidate(false) == null);
      }
    });

    customDockerfileRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerfileComboBox.setEnabled(false);
        customDockerfileBrowseButton.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerfilePath(customDockerfileBrowseButton.getText())) {
          customDockerfileBrowseLabel.setVisible(false);
          setFinishButtonState(doValidate(false) == null);
        } else {
          customDockerfileBrowseLabel.setVisible(true);
          setFinishButtonState(false);
        }
      }
    });

    dockerContainerPortSettings.setText(String.format("2%4d:", new Random().nextInt(10000)) + // default to host port 2xxxx
        (dockerManager.getDefaultDockerImages().isEmpty() ?
            "8080" :
            ((KnownDockerImages) dockerfileComboBox.getSelectedItem()).getPortSettings()));
    dockerContainerPortSettings.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerPortSettings(dockerContainerPortSettings.getText())) {
          dockerContainerPortSettingsLabel.setVisible(false);
          setFinishButtonState(doValidate(false) == null);
          return true;
        } else {
          dockerContainerPortSettingsLabel.setVisible(true);
          setFinishButtonState(false);
          return false;
        }
      }
    });
    dockerContainerPortSettingsLabel.setVisible(false);

  }

//  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }

  private ValidationInfo doValidate(Boolean shakeOnError) {
    if (dockerContainerNameTextField.getText() == null || dockerContainerNameTextField.getText().equals("")){
      ValidationInfo info = new ValidationInfo("Please name your Docker container", dockerContainerNameTextField);
      dockerContainerNameLabel.setVisible(true);
      setFinishButtonState(false);
      if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    dockerImageDescription.dockerContainerName = dockerContainerNameTextField.getText();
    dockerContainerNameLabel.setVisible(false);

    if (predefinedDockerfileRadioButton.isSelected()) {
      KnownDockerImages dockerfileImage = (KnownDockerImages) dockerfileComboBox.getSelectedItem();
      if (dockerfileImage == null) {
        ValidationInfo info = new ValidationInfo("Please select a Docker image type form the list", dockerfileComboBox);
        setFinishButtonState(true);
        if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
        return info;
      }
      if (dockerImageDescription.artifactPath != null) {
        dockerImageDescription.dockerfileContent = dockerfileImage.getDockerfileContent()
            .replace(KnownDockerImages.DOCKER_ARTIFACT_FILENAME, new File(dockerImageDescription.artifactPath).getName());
      }
    }

    String dockerfileName = customDockerfileBrowseButton.getText();
    if (customDockerfileRadioButton.isSelected()) {
      if (dockerfileName == null || dockerfileName.equals("") || Files.notExists(Paths.get(dockerfileName))) {
        ValidationInfo info = new ValidationInfo("Dockerfile not found", customDockerfileBrowseButton);
        customDockerfileBrowseLabel.setVisible(true);
        setFinishButtonState(true);
        if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
        return info;
      }
      try {
        model.getDockerImageDescription().dockerfileContent = new String(Files.readAllBytes(Paths.get(customDockerfileBrowseButton.getText())));
      } catch (Exception e) {
        customDockerfileBrowseLabel.setVisible(true);
        setFinishButtonState(false);

        String errTitle = "Error reading Dockerfile content";
        String msg = "An error occurred while attempting to get the content of " + customDockerfileBrowseButton.getText() + "\n" + e.getMessage();
        if (AzureDockerUtils.DEBUG) e.printStackTrace();
        LOGGER.error("doValidate", e);
        PluginUtil.displayErrorDialog(errTitle, msg);

        return new ValidationInfo(errTitle, customDockerfileBrowseButton);
      }
    }

    if (dockerContainerPortSettings.getText() == null || dockerContainerPortSettings.getText().equals("") ||
        !AzureDockerValidationUtils.validateDockerPortSettings(dockerContainerPortSettings.getText())){
      ValidationInfo info = new ValidationInfo("Invalid port settings", dockerContainerPortSettings);
      if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
      dockerContainerPortSettingsLabel.setVisible(true);
      setFinishButtonState(false);
      return info;
    }
    dockerImageDescription.dockerPortSettings = dockerContainerPortSettings.getText();

    return null;
  }

  private void setFinishButtonState(boolean finishButtonState) {
    model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootConfigureContainerPanel.revalidate();
    setFinishButtonState(true);

    return rootConfigureContainerPanel;
  }

  @Override
  public WizardStep onNext(final AzureSelectDockerWizardModel model) {
    if (doValidate() == null) {
      return super.onNext(model);
    } else {
      return this;
    }
  }

  @Override
  public boolean onFinish() {
    return doValidate(false) == null  && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    model.finishedOK = true;

    return super.onCancel();
  }
}
