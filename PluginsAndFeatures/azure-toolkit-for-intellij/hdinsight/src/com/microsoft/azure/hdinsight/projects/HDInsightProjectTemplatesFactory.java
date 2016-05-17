package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.util.IconLoader;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import com.microsoft.azure.hdinsight.projects.template.CustomProjectTemplate;
import com.microsoft.azure.hdinsight.projects.template.TemplatesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class HDInsightProjectTemplatesFactory extends ProjectTemplatesFactory{
    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] { "HDInsight" };
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String var1, WizardContext var2) {
        ArrayList<HDInsightTemplateItem> templateItems = HDInsightTemplates.getTemplates();
        int templateCount = templateItems.size();
        List<CustomHDInsightTemplateItem> customHDInsightTemplateItems = TemplatesUtil.getCustomTemplate();

        ProjectTemplate[] projectTemplates = new ProjectTemplate[templateCount + customHDInsightTemplateItems.size()];
        for (int i = 0; i < templateCount ; i++) {
            projectTemplates[i] = new HDInsightProjectTemplate(templateItems.get(i));
        }
        for(int i = templateCount; i < templateCount + customHDInsightTemplateItems.size(); ++i) {
            projectTemplates[i] = new CustomProjectTemplate(customHDInsightTemplateItems.get(i - templateCount));
        }

        return projectTemplates;
    }

    @Override
    public Icon getGroupIcon(String group) {
        return IconLoader.getIcon(CommonConst.ProductIConPath);
    }
}
