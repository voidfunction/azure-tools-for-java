package com.microsoft.azure.hdinsight.ToolWindow;

import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;

public class ServerExploreToolWindowProcessor implements IToolWindowProcessor {
    private HDInsightRootModule azureServiceModule;

    public ServerExploreToolWindowProcessor(HDInsightRootModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    public void Initialize() {

    }

    public HDInsightRootModule getAzureServiceModule() {
        return azureServiceModule;
    }
}
