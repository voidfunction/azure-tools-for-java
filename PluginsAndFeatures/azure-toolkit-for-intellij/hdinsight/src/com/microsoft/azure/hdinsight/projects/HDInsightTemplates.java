package com.microsoft.azure.hdinsight.projects;
import java.util.ArrayList;

public class HDInsightTemplates {
    private static final String[] TemplateNames = {
            "Spark on HDInsight (Scala)",
            "Spark on HDInsight (Java)",
            "Spark on HDInsight Local Run Sample (Scala)",
            "Spark on HDInsight Local Run Sample (Java)",
            "Spark on HDInsight Cluster Run Sample (Scala)",
    };

    private static ArrayList<HDInsightTemplateItem> templates = new ArrayList<HDInsightTemplateItem>();

    static {
        templates.add(new HDInsightTemplateItem(TemplateNames[0], HDInsightTemplatesType.Scala));
        templates.add(new HDInsightTemplateItem(TemplateNames[1], HDInsightTemplatesType.Java));
        templates.add(new HDInsightTemplateItem(TemplateNames[2], HDInsightTemplatesType.ScalaLocalSample));
        templates.add(new HDInsightTemplateItem(TemplateNames[3], HDInsightTemplatesType.JavaLocalSample));
        templates.add(new HDInsightTemplateItem(TemplateNames[4], HDInsightTemplatesType.ScalaClusterSample));
    }

    public static String[] getTemplateNames() { return TemplateNames; }

    public static ArrayList<HDInsightTemplateItem> getTemplates() {
        return templates;
    }
}

