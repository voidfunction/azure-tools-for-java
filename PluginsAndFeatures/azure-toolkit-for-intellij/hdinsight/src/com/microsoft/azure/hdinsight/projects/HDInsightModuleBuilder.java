package com.microsoft.azure.hdinsight.projects;

import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.TelemetryCommon;
import com.microsoft.azure.hdinsight.common.TelemetryManager;
import com.microsoft.azure.hdinsight.projects.samples.ProjectSampleUtil;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import com.microsoft.azure.hdinsight.projects.template.CustomModuleWizardSetup;
import com.microsoft.azure.hdinsight.projects.template.CustomTemplateInfo;
import com.microsoft.azure.hdinsight.projects.template.TemplatesUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;

public class HDInsightModuleBuilder extends JavaModuleBuilder implements ModuleBuilderListener {
    private HDInsightTemplateItem selectedTemplate;
    private LibrariesContainer librariesContainer;
    private LibraryCompositionSettings scalaLibraryCompositionSettings;
    private LibraryCompositionSettings sparkLibraryCompositionSettings;

    private boolean isSparkSdkIncluded = false;
    private boolean isScalaSdkIncluded = false;

    public static final String UniqueKeyName = "UniqueKey";
    public static final String UniqueKeyValue = "HDInsightTool";

    public HDInsightModuleBuilder() {
        this.addListener(this);
        this.addModuleConfigurationUpdater(new ModuleConfigurationUpdater() {
            @Override
            public void update(Module module, ModifiableRootModel modifiableRootModel) {
                int librarySize = getOrderEntriesLength(modifiableRootModel);

                if(sparkLibraryCompositionSettings != null){
                    sparkLibraryCompositionSettings.addLibraries(modifiableRootModel, new ArrayList<Library>(), librariesContainer);
                    if (getOrderEntriesLength(modifiableRootModel) != librarySize) {
                        isSparkSdkIncluded = true;
                        librarySize = getOrderEntriesLength(modifiableRootModel);
                    }
                } else {
                    isSparkSdkIncluded = true;
                }

                if (scalaLibraryCompositionSettings != null) {
                    scalaLibraryCompositionSettings.addLibraries(modifiableRootModel, new ArrayList<Library>(), librariesContainer);
                    if (getOrderEntriesLength(modifiableRootModel) != librarySize) {
                        isScalaSdkIncluded = true;
                    }
                } else {
                    isScalaSdkIncluded = true;
                }
            }
        });
    }

    public void setSelectedTemplate(HDInsightTemplateItem selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void setSparkCompositionSettings(LibraryCompositionSettings sparkCompositionSettings) {
        this.sparkLibraryCompositionSettings = sparkCompositionSettings;
    }

    public void setScalaLibraryCompositionSettings(LibraryCompositionSettings scalaLibraryCompositionSettings) {
        this.scalaLibraryCompositionSettings = scalaLibraryCompositionSettings;
    }

    public void setLibraryCompositionSettings(LibraryCompositionSettings scalalibraryCompositionSettings, LibraryCompositionSettings sparkLibraryCompositionSettings) {
        this.scalaLibraryCompositionSettings= scalalibraryCompositionSettings;
        this.sparkLibraryCompositionSettings = sparkLibraryCompositionSettings;
    }

    @Override
    public String getBuilderId() {
        return "HDInsight";
    }

    @Override
    public Icon getBigIcon() {
        return null;
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon(CommonConst.ProductIConPath);
    }

    @Override
    public String getPresentableName() {
        return "HDInsight";
    }

    @Override
    public String getGroupName() {
        return "HDInsight Tools";
    }

    @Override
    public ModuleType getModuleType() {
        return HDInsightModuleType.getInstance();
    }

    @Override
    public ModuleWizardStep modifySettingsStep(SettingsStep settingsStep) {

        this.librariesContainer = LibrariesContainerFactory.createContainer(settingsStep.getContext().getProject());

        if (this.selectedTemplate.getType() == HDInsightTemplatesType.CustomTemplate) {
            return new CustomModuleWizardSetup(this, settingsStep, librariesContainer, ((CustomHDInsightTemplateItem) this.selectedTemplate).getTemplateInfo());
        } else if (this.selectedTemplate.getType() == HDInsightTemplatesType.Scala ||
                this.selectedTemplate.getType() == HDInsightTemplatesType.ScalaClusterSample ||
                this.selectedTemplate.getType() == HDInsightTemplatesType.ScalaLocalSample) {
            return new SparkScalaSettingsStep(this, settingsStep, this.librariesContainer);
        } else {
            return new SparkJavaSettingsStep(this, settingsStep, this.librariesContainer);
        }
    }

    @Override
    public void moduleCreated(@NotNull Module module) {
        HDInsightTemplatesType templatesType = this.selectedTemplate.getType();

        if(templatesType == HDInsightTemplatesType.CustomTemplate) {
            customTemplateModuleCreated(module, ((CustomHDInsightTemplateItem)this.selectedTemplate).getTemplateInfo());
        } else {
            module.setOption(UniqueKeyName, UniqueKeyValue);
            createDefaultArtifact(module);
            if (templatesType == HDInsightTemplatesType.JavaLocalSample ||
                    templatesType == HDInsightTemplatesType.ScalaClusterSample ||
                    templatesType == HDInsightTemplatesType.ScalaLocalSample) {
                ProjectSampleUtil.copyFileToPath(module, templatesType);
            }
        }

        CheckSDK(templatesType);
        addTelemetry(templatesType);
    }

    private void customTemplateModuleCreated(Module module, CustomTemplateInfo info) {
        if(info.isSparkProject()) {
            module.setOption(UniqueKeyName, UniqueKeyValue);
        }
        TemplatesUtil.createTemplateSampleFiles(module, info);
    }

    private void CheckSDK(HDInsightTemplatesType templatesType) {
        if (templatesType == HDInsightTemplatesType.Java || templatesType == HDInsightTemplatesType.JavaLocalSample) {
            if (!isSparkSdkIncluded) {
                showErrorMessageWithLink(withoutSparkSDKErrorMessage, "Project SDK Check", sparkDownloadLink);
            }
        } else if (templatesType == HDInsightTemplatesType.Scala ||
                templatesType == HDInsightTemplatesType.ScalaClusterSample ||
                templatesType == HDInsightTemplatesType.ScalaLocalSample) {
            if(!isScalaSdkIncluded || !isSparkSdkIncluded) {
                showErrorMessageWithLink(withoutSParkOrScalaSDKErrorMessage, "Project SDK Check", sparkDownloadLink);
            }
        }
    }

    private int getOrderEntriesLength(ModifiableRootModel modifiableRootModel) {
        return modifiableRootModel.getOrderEntries().length;
    }

    private void addTelemetry(HDInsightTemplatesType templatesType){
        if(templatesType == HDInsightTemplatesType.Java){
            TelemetryManager.postEvent(TelemetryCommon.SparkProjectSystemJavaCreation, null, null);
        }else if(templatesType == HDInsightTemplatesType.JavaLocalSample){
            TelemetryManager.postEvent(TelemetryCommon.SparkProjectSystemJavaSampleCreation, null, null);
        }else if(templatesType == HDInsightTemplatesType.Scala) {
            TelemetryManager.postEvent(TelemetryCommon.SparkProjectSystemScalaCreation, null, null);
        }else if(templatesType == HDInsightTemplatesType.ScalaClusterSample || templatesType == HDInsightTemplatesType.ScalaLocalSample){
            TelemetryManager.postEvent(TelemetryCommon.SparkProjectSystemScalaSampleCreation, null, null);
        }
    }

    private void createDefaultArtifact(final Module module) {
        final Project project = module.getProject();
        final JarArtifactType type = new JarArtifactType();
        final PackagingElementFactory factory = PackagingElementFactory.getInstance();
        CompositePackagingElement root = factory.createArchive("default_artifact.jar");
        root.addOrFindChild(factory.createModuleOutput(module));
        ArtifactManager.getInstance(project).addArtifact(module.getName() + "_DefaultArtifact", type, root);
    }

    private static String sparkDownloadLink = "http://go.microsoft.com/fwlink/?LinkId=723585";
    private static String withoutSparkSDKErrorMessage = String.format("<HTML>Failed to load Spark SDK and you need to add Spark SDK manually. Please download the Spark assembly from <FONT color=\\\"#000099\\\"><U>%s</U></FONT> and then add it manually.</HTML>", sparkDownloadLink);
    private static String withoutSParkOrScalaSDKErrorMessage = String.format("<HTML>Failed to load Spark SDK and you need to add Scala/Spark SDK manually. Please download the Spark assembly from <FONT color=\\\"#000099\\\"><U>%s</U></FONT> and then add it manually.</HTML>", sparkDownloadLink);

    private void showErrorMessageWithLink(String message, String title, final String link){
        JLabel label = new JLabel(message);
        label.setOpaque(false);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    URI uri = new URI(link);
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(uri);
                    } else {
                        DefaultLoader.getUIHelper().showError("Couldn't open browser in current OS system", "Open URL in Browser");
                    }
                } catch (Exception exception) {
                    DefaultLoader.getUIHelper().showException("An error occurred while attempting to open Browser.", exception, "Error Open Browser", false, true);
                }
            }
        });

        JOptionPane.showMessageDialog(null, label, title, JOptionPane.ERROR_MESSAGE);
    }
}
