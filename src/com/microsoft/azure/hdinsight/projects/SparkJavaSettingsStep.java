package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;

import javax.swing.*;

public class SparkJavaSettingsStep extends ModuleWizardStep {
    private HDInsightModuleBuilder builder;
    private ModuleWizardStep javaStep;
    private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;
    private LibrariesContainer librariesContainer;

    public SparkJavaSettingsStep(HDInsightModuleBuilder builder, SettingsStep settingsStep, LibrariesContainer librariesContainer) {
        this.builder = builder;
        this.javaStep = StdModuleTypes.JAVA.modifyProjectTypeStep(settingsStep, builder);
        this.librariesContainer = librariesContainer;

        sparkLibraryOptionsPanel = new SparkLibraryOptionsPanel(settingsStep.getContext().getProject(), librariesContainer, new SparkLibraryDescription());
        settingsStep.addSettingsField("Spark S\u001BDK:", sparkLibraryOptionsPanel);
        settingsStep.addSettingsField("",ProjectUtil.createSparkSDKTipsPanel());
    }

    @Override
    public JComponent getComponent() {
        return javaStep.getComponent();
    }

    @Override
    public void updateDataModel() {
        javaStep.updateDataModel();
        this.builder.setSparkCompositionSettings(sparkLibraryOptionsPanel.apply());
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return super.validate() && (javaStep == null || javaStep.validate());
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        javaStep.disposeUIResources();
    }
}
