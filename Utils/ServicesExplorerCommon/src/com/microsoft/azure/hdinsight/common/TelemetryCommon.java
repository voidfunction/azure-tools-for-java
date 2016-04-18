package com.microsoft.azure.hdinsight.common;

public class TelemetryCommon {

    public static final String SparkProjectSystemCategory = "SparkProjectSystem";
    public static final String SparkProjectSystemJavaCreation = SparkProjectSystemCategory + ".JavaCreation";
    public static final String SparkProjectSystemJavaSampleCreation = SparkProjectSystemCategory + ".JavaSampleCreation";
    public static final String SparkProjectSystemScalaCreation = SparkProjectSystemCategory + ".ScalaCreation";
    public static final String SparkProjectSystemScalaSampleCreation = SparkProjectSystemCategory + ".ScalaSampleCreation";
    public static final String SparkProjectSystemStreamingSampleCreation = SparkProjectSystemCategory  + ".ScalaStreamingSampleCreation";
    public static final String SparkProjectSystemSparkSqlampleCreation = SparkProjectSystemCategory  + ".ScalaSparkSqlSampleCreation";

    public static final String SparkSubmissionCategory = "SparkSubmission";
    public static final String SparkSubmissionRightClickProject = SparkSubmissionCategory + ".RightClickProject";
    public static final String SparkSubmissionButtonClickEvent = SparkSubmissionCategory + ".SubmitEvent";
    public static final String SparkSubmissionHelpClickEvent = SparkSubmissionCategory + ".HelpEvent";
    public static final String SparkSubmissionStopButtionClickEvent = SparkSubmissionCategory + ".StopJobEvent";

    public static final String HDInsightExplorerCategory = "HDInsightExplorer";
    public static final String HDInsightExplorerHDInsightNodeExpand = HDInsightExplorerCategory + ".HDInsightNodeExpand";
    public static final String HDInsightExplorerSparkNodeExpand = HDInsightExplorerCategory + ".SparkNodeExpand";
    public static final String HDInsightExplorerStorageAccountExpand = HDInsightExplorerCategory + ".StorageAccountExpand";
    public static final String HDInsightExplorerContainerOpen = HDInsightExplorerCategory + ".OpenContainer";


}
