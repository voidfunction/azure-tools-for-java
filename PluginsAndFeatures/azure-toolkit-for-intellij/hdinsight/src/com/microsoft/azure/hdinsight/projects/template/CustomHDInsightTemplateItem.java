package com.microsoft.azure.hdinsight.projects.template;

import com.microsoft.azure.hdinsight.projects.HDInsightTemplateItem;
import com.microsoft.azure.hdinsight.projects.HDInsightTemplatesType;

public class CustomHDInsightTemplateItem extends HDInsightTemplateItem {
    private CustomTemplateInfo info;

    public CustomHDInsightTemplateItem(CustomTemplateInfo info) {
        super(info.getName(), HDInsightTemplatesType.CustomTemplate);
        this.info = info;
    }

    @Override
    public String getDisplayText() {
        return info.getName();
    }

    @Override
    public void setDisplayText(String displayText) {
        super.setDisplayText(displayText);
    }

    @Override
    public HDInsightTemplatesType getType() {
        return HDInsightTemplatesType.CustomTemplate;
    }

    @Override
    public void setType(HDInsightTemplatesType type) {
        super.setType(type);
    }

    public CustomTemplateInfo getTemplateInfo() {
        return info;
    }
}
