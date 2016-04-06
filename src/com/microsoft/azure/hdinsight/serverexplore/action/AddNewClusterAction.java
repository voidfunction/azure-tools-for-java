package com.microsoft.azure.hdinsight.serverexplore.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.serverexplore.UI.AddNewClusterFrom;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.node.Name;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionEvent;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionListener;

@Name("Add New Cluster")
public class AddNewClusterAction extends NodeActionListener {

    private HDInsightRootModule azureServiceModule;

    public AddNewClusterAction(HDInsightRootModule azureServiceModule) {
        this.azureServiceModule = azureServiceModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AddNewClusterFrom form = new AddNewClusterFrom((Project) azureServiceModule.getProject());
        form.show();
    }
}
