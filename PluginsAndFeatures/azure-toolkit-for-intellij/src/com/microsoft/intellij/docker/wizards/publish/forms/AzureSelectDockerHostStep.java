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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.docker.dialogs.AzureEditDockerLoginCredsDialog;
import com.microsoft.intellij.docker.dialogs.AzureViewDockerDialog;
import com.microsoft.intellij.docker.utils.AzureDockerRefreshResources;
import com.microsoft.intellij.docker.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardStep;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

public class AzureSelectDockerHostStep extends AzureSelectDockerWizardStep {
  private JPanel rootPanel;
  private JPanel dockerHostsPanel;
  private JTextField dockerImageNameTextField;
  private TextFieldWithBrowseButton dockerArtifactPath;
  private JLabel dockerImageNameLabel;
  private JLabel dockerArtifactPathLabel;
  private JBTable dockerHostsTable;

  private AzureSelectDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private AzureDockerImageInstance dockerImageDescription;
  private Artifact artifact;

  private DockerHostsTableSelection dockerHostsTableSelection;

  public AzureSelectDockerHostStep(String title, AzureSelectDockerWizardModel model, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageInstance) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Type an image name, select the artifact path and check a Docker to be used");
    this.model = model;
    this.dockerManager = dockerManager;
    this.dockerImageDescription = dockerImageInstance;

    dockerImageNameTextField.setText(dockerImageInstance.dockerImageName);
    dockerImageNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerImageNameTip());
    dockerImageNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerImageName(((JTextField) input).getText())) {
          dockerImageNameLabel.setVisible(false);
          setFinishButtonState(doValidate(false) == null);
          setNextButtonState(doValidate(false) == null);
          return true;
        } else {
          dockerImageNameLabel.setVisible(true);
          setFinishButtonState(false);
          setNextButtonState(false);
          return false;
        }
      }
    });
    dockerImageNameLabel.setVisible(false);

    dockerArtifactPath.addActionListener(UIUtils.createFileChooserListener(dockerArtifactPath, model.getProject(),
        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));

    artifact = null;

    for (Artifact item : ArtifactUtil.getArtifactWithOutputPaths(model.getProject())) {
      if (item.getArtifactType().getPresentableName().equals("Web Application: Archive")) {
        artifact = item;
        dockerImageInstance.artifactName = artifact.getName();
        break;
      }
    }

    dockerArtifactPath.setText(artifact != null ?
        artifact.getOutputFilePath() :
        ArtifactUtil.getDefaultArtifactOutputPath(dockerImageInstance.artifactName, model.getProject()));
    dockerArtifactPath.setToolTipText(AzureDockerValidationUtils.getDockerArtifactPathTip());
    dockerArtifactPath.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerArtifactPath(dockerArtifactPath.getText())) {
          dockerArtifactPathLabel.setVisible(false);
          setFinishButtonState(doValidate(false) == null);
          setNextButtonState(doValidate(false) == null);
          return true;
        } else {
          dockerArtifactPathLabel.setVisible(true);
          setFinishButtonState(false);
          setNextButtonState(false);
          return false;
        }
      }
    });
    dockerArtifactPathLabel.setVisible(false);

    refreshDockerHostsTable();

    if (dockerHostsTable.getRowCount() > 0) {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      tableModel.setValueAt(true, 0, 0);
      dockerHostsTableSelection = new DockerHostsTableSelection();
      dockerHostsTableSelection.column = 0;
      dockerHostsTableSelection.row = 0;
      dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(0, 4));
      dockerHostsTable.repaint();
    }

  }

  private void createUIComponents() {
    final DefaultTableModel dockerListTableModel = new DefaultTableModel() {
      @Override
      public boolean isCellEditable(int row, int col) {
        return (col == 0);
      }

      public Class<?> getColumnClass(int colIndex) {
        return getValueAt(0, colIndex).getClass();
      }
    };

    dockerListTableModel.addColumn("");
    dockerListTableModel.addColumn("Name");
    dockerListTableModel.addColumn("State");
    dockerListTableModel.addColumn("OS");
    dockerListTableModel.addColumn("API URL");
    dockerHostsTable = new JBTable(dockerListTableModel);
    dockerHostsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableColumn column = dockerHostsTable.getColumnModel().getColumn(0);
    column.setMinWidth(23);
    column.setMaxWidth(23);
    column = dockerHostsTable.getColumnModel().getColumn(1);
    column.setPreferredWidth(150);
    column = dockerHostsTable.getColumnModel().getColumn(2);
    column.setPreferredWidth(30);
    column = dockerHostsTable.getColumnModel().getColumn(3);
    column.setPreferredWidth(110);
    column = dockerHostsTable.getColumnModel().getColumn(4);
    column.setPreferredWidth(150);

    dockerListTableModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        DockerHostsTableSelection currentSelection = new DockerHostsTableSelection();
        currentSelection.column = dockerHostsTable.getSelectedColumn();
        currentSelection.row = dockerHostsTable.getSelectedRow();

        if (currentSelection.column == 0) {
          DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
          if ((Boolean) tableModel.getValueAt(currentSelection.row, currentSelection.column)) {
            if (dockerHostsTableSelection == null) {
              dockerHostsTableSelection = currentSelection;
              dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4));
            } else {
              int oldRow = dockerHostsTableSelection.row;
              dockerHostsTableSelection = null;
              if (currentSelection.row != oldRow) {
                // disable previous selection
                tableModel.setValueAt(false, oldRow, 0);
                dockerHostsTableSelection = currentSelection;
                dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4));
              }
            }
            setFinishButtonState(doValidate(false) == null);
            setNextButtonState(doValidate(false) == null);
          } else {
            dockerHostsTableSelection = null;
            setFinishButtonState(false);
            setNextButtonState(false);
          }
        }
      }
    });

    AnActionButton viewDockerHostsAction = new ToolbarDecorator.ElementActionButton("Details", AllIcons.Actions.Export) {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
        onViewDockerHostAction();
      }
    };

    AnActionButton refreshDockerHostsAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
        onRefreshDockerHostAction();
      }
    };

    ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(dockerHostsTable)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onAddNewDockerHostAction();
          }
        }).setEditAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton anActionButton) {
            onEditDockerHostAction();
          }
        }).setRemoveAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onRemoveDockerHostAction();
          }
        }).setEditActionUpdater(new AnActionButtonUpdater() {
          @Override
          public boolean isEnabled(AnActionEvent e) {
            return dockerHostsTable.getSelectedRow() != -1;
          }
        }).setRemoveActionUpdater(new AnActionButtonUpdater() {
          @Override
          public boolean isEnabled(AnActionEvent e) {
            return dockerHostsTable.getSelectedRow() != -1;
          }
        }).disableUpDownActions()
        .addExtraActions(viewDockerHostsAction, refreshDockerHostsAction);


    dockerHostsPanel = tableToolbarDecorator.createPanel();
  }

  private static final Logger LOGGER = Logger.getInstance(AzureSelectDockerHostStep.class);
  private void onRefreshDockerHostAction() {
    AzureDockerRefreshResources.updateAzureResourcesWithProgressDialog(model.getProject());

    refreshDockerHostsTable();
  }

  private void onViewDockerHostAction() {
    try {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      String apiURL = (String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4);
      DockerHost dockerHost = dockerManager.getDockerHostForURL(apiURL);

      if (dockerHost == null) {
        throw  new RuntimeException(String.format("Unexpected error: can't locate the Docker host for %s!", apiURL));
      }

      AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(model.getProject(), dockerHost, dockerManager);
      viewDockerDialog.show();

      if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
        onEditDockerHostAction();
      }
    } catch (Exception e) {
      String msg = "An error occurred while attempting to view the selected Docker host.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

  private void onAddNewDockerHostAction() {
    AzureNewDockerWizardModel newDockerHostModel = new AzureNewDockerWizardModel(model.getProject(), dockerManager);
    AzureNewDockerWizardDialog wizard = new AzureNewDockerWizardDialog(newDockerHostModel);
    wizard.setTitle("Create a Docker Host");
    wizard.show();

    if (wizard.getExitCode() == 0) {
      dockerHostsTable.setEnabled(false);

      DockerHost host = newDockerHostModel.getDockerHost();
      dockerImageDescription.host = host;
      dockerImageDescription.hasNewDockerHost = true;
      dockerImageDescription.sid = host.sid;

      final DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();

      if (dockerHostsTableSelection != null && (Boolean) tableModel.getValueAt(dockerHostsTableSelection.row, 0)) {
        tableModel.setValueAt(false, dockerHostsTableSelection.row, 0);
      }

      Vector<Object> row = new Vector<Object>();
      row.add(false);
      row.add(host.name);
      row.add("NEW-AZURE-VM");
      row.add(host.hostOSType.toString());
      row.add(host.apiUrl);
      tableModel.insertRow(0, row);
      tableModel.setValueAt(true, 0, 0);
      dockerHostsTable.setRowSelectionInterval(0, 0);

      setFinishButtonState(doValidate(false) == null);
      setNextButtonState(doValidate(false) == null);
    }
  }

  private void onEditDockerHostAction() {
    try {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      String apiURL = (String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4);

      EditableDockerHost editableDockerHost = new EditableDockerHost(dockerManager.getDockerHostForURL(apiURL));

      AzureEditDockerLoginCredsDialog editDockerDialog = new AzureEditDockerLoginCredsDialog(model.getProject(), editableDockerHost, dockerManager);
      editDockerDialog.show();

      if (editDockerDialog.getExitCode() == 0) {
        forceRefreshDockerHostsTable();
      }
    } catch (Exception e) {
      String msg = "An error occurred while attempting to edit the selected Docker host.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

  private void onRemoveDockerHostAction() {
  }

  /* Force a refresh of the docker hosts entries in the select host table
 *   This call will retrieve the latest list of VMs from Azure suitable to be a Docker Host
 */
  void forceRefreshDockerHostsTable() {
    dockerManager.forceRefreshDockerHosts();
    refreshDockerHostsTable();
  }

  /* Refresh the docker hosts entries in the select host table
   *
   */
  void refreshDockerHostsTable() {
    final DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();

    while (tableModel.getRowCount() > 0) {
      tableModel.removeRow(0);
    }

    try {
      List<DockerHost> dockerHosts = dockerManager.getDockerHostsList();
      if (dockerHosts != null) {
        for (DockerHost host : dockerHosts) {
          Vector<Object> row = new Vector<Object>();
          row.add(false);
          row.add(host.name);
          row.add(host.state.toString());
          row.add(host.hostOSType.toString());
          row.add(host.apiUrl);
          tableModel.addRow(row);
        }
      }
    } catch (Exception e) {
      String msg = "An error occurred while attempting to get the list of recognizable Docker hosts.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }


  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootPanel.revalidate();
    setFinishButtonState(true);
    setNextButtonState(true);

    return rootPanel;
  }

  private void setFinishButtonState(boolean finishButtonState) {
    model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
  }

  private void setNextButtonState(boolean nextButtonState) {
    model.getCurrentNavigationState().NEXT.setEnabled(nextButtonState);
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public ValidationInfo doValidate(boolean shakeOnError) {
    if (dockerImageNameTextField.getText() == null || dockerImageNameTextField.getText().equals("")){
      ValidationInfo info = new ValidationInfo("Missing Docker image name", dockerImageNameTextField);
      dockerImageNameLabel.setVisible(false);
      model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    dockerImageDescription.dockerImageName = dockerImageNameTextField.getText();

    if (dockerArtifactPath.getText() == null || dockerArtifactPath.getText().equals("") ||
        (artifact == null && !Files.isRegularFile(Paths.get(dockerArtifactPath.getText())))) {
      ValidationInfo info = new ValidationInfo("Missing the artifact to be published", dockerArtifactPath);
      dockerArtifactPathLabel.setVisible(true);
      model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    dockerImageDescription.artifactPath = dockerArtifactPath.getText();

    if (dockerHostsTableSelection == null && !dockerImageDescription.hasNewDockerHost){
      ValidationInfo info = new ValidationInfo("No Docker host has been selected", dockerHostsTable);
      model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    if (!dockerImageDescription.hasNewDockerHost) {
      dockerImageDescription.host = dockerHostsTableSelection.host;
      dockerImageDescription.sid = dockerImageDescription.host.sid;
    }

    return null;
  }

  public ValidationInfo doValidate() {
    return doValidate(true);
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
    return model.doValidate() == null && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    model.finishedOK = true;

    return super.onCancel();
  }

  private class DockerHostsTableSelection {
    int row;
    int column;
    DockerHost host;
  }

// CREATE CUSTOM ACTION FOR DIALOG WRAPPER!!!!!
//  @Nullable
//  @Override
//  protected Action[] createActions() {
//    myClickApplyAction = new ClickApplyAction();
//    myClickApplyAction.setEnabled(false);
//    return new Action[] {getCancelAction(), myClickApplyAction, getOKAction()};
//  }
//
//  protected class ClickApplyAction extends DialogWrapper.DialogWrapperAction {
//    protected ClickApplyAction() {
//      super("Apply");
//    }
//
//    protected void doAction(ActionEvent e) {
//      ValidationInfo info = doClickApplyValidate();
//      if(info != null) {
//        dialogShaker(info);
//      } else {
//        doClickApplyAction();
//      }
//    }
//  }

}
