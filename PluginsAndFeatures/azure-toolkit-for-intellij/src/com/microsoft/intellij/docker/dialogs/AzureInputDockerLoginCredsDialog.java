package com.microsoft.intellij.docker.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.forms.AzureDockerHostUpdateLoginPanel;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureInputDockerLoginCredsDialog extends DialogWrapper {
  private JPanel mainPanel;

  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;
  private AzureDockerHostUpdateLoginPanel loginPanel;

  public AzureInputDockerLoginCredsDialog(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerManager) {
    super(project, true);

    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerManager;

    loginPanel = new AzureDockerHostUpdateLoginPanel(project, editableHost, dockerManager, this);

    init();
    setTitle("Update Login Credentials");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return loginPanel.getMainPanel();
    //return mainPanel;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return null;
  }

  @Nullable
  @Override
  protected Action[] createActions() {
    Action updateAction = getOKAction();
    updateAction.putValue(Action.NAME, "Update");
    return new Action[] {getCancelAction(), updateAction};
  }

  @Nullable
  @Override
  protected void doOKAction() {
    try {
      if (loginPanel.doValidate(true) == null) {
        super.doOKAction();
      }
    }
    catch (Exception e){
      String msg = "An error occurred while attempting to use the updated log in credentials.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

}
