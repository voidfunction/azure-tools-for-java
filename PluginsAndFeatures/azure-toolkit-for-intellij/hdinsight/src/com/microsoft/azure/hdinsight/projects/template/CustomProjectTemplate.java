package com.microsoft.azure.hdinsight.projects.template;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CustomProjectTemplate implements ProjectTemplate {

    private CustomHDInsightTemplateItem templateItem;

    public CustomProjectTemplate(CustomHDInsightTemplateItem parameterItem) {
        this.templateItem = parameterItem;
    }

    @NotNull
    @Override
    public String getName() {
        return this.templateItem.getDisplayText();
    }

    @Nullable
    @Override
    public String getDescription() {
        String description = this.templateItem.getTemplateInfo().getDescription();
        return StringHelper.isNullOrWhiteSpace(description) ? description : "Custom Template of HDInsight Tools";
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(this.templateItem.getTemplateInfo().getIconPath());
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        HDInsightModuleBuilder builder = new HDInsightModuleBuilder();
        builder.setSelectedTemplate(this.templateItem);
        return builder;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }
}
