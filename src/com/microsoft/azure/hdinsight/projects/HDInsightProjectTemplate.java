package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;

public class HDInsightProjectTemplate implements ProjectTemplate {
    private HDInsightTemplateItem templateItem;

    private static HashMap<HDInsightTemplatesType, ImageIcon> imageMap = new HashMap<HDInsightTemplatesType, ImageIcon>() {
        {
            put(HDInsightTemplatesType.Java, StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath));
            put(HDInsightTemplatesType.Scala, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
            put(HDInsightTemplatesType.JavaLocalSample, StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath));
            put(HDInsightTemplatesType.ScalaClusterSample, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
            put(HDInsightTemplatesType.ScalaLocalSample, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
        }
    };

    public HDInsightProjectTemplate(HDInsightTemplateItem parameterItem) {
        this.templateItem = parameterItem;
    }

    @NotNull
    @Override
    public String getName() { return templateItem.getDisplayText(); }

    @Nullable
    @Override
    public String getDescription() { return "HDInsight Tools"; }

    @Override
    public Icon getIcon() {
        if(this.templateItem instanceof CustomHDInsightTemplateItem) {
            return new ImageIcon(((CustomHDInsightTemplateItem) this.templateItem).getTemplateInfo().getIconPath());
        } else {
            return imageMap.get(templateItem.getType());
        }
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        HDInsightModuleBuilder builder = new HDInsightModuleBuilder();
        builder.setSelectedTemplate(templateItem);
        return builder;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() { return null; }
}
