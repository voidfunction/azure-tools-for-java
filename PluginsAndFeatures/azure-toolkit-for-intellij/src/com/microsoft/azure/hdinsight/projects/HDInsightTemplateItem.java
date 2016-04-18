package com.microsoft.azure.hdinsight.projects;

public class HDInsightTemplateItem {
    private String displayText;
    private HDInsightTemplatesType type;

    public HDInsightTemplateItem(String text, HDInsightTemplatesType type){
        this.displayText = text;
        this.type = type;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public HDInsightTemplatesType getType() {
        return type;
    }

    public void setType(HDInsightTemplatesType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.displayText;
    }
}
