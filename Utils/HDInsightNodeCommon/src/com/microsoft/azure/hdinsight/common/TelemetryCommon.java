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
