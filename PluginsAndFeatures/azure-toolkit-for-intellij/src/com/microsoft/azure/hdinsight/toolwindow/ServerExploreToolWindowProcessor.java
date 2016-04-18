package com.microsoft.azure.hdinsight.toolwindow;

import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;

public class ServerExploreToolWindowProcessor implements IToolWindowProcessor {
    private HDInsightRootModule hdInsightModule;

    public ServerExploreToolWindowProcessor(HDInsightRootModule azureServiceModule) {
        this.hdInsightModule = azureServiceModule;
    }

    public void initialize() {
    }

    public HDInsightRootModule getHDInsightModule() {
        return hdInsightModule;
    }
}
