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
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.tooling.msservices.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import org.apache.log4j.Logger;

import java.util.Date;

public class JobViewNode extends RefreshableNode {
    private static String NODE_ID = JobViewNode.class.getName();
    private static String NODE_NAME = "Jobs";
    private static String NODE_ICON_PATH = CommonConst.StorageAccountFoldIConPath;

    private final String uuid;
    private long lastClickTime = 0L;

    //private static Logger LOG = Logger.getInstance(JobViewNode.class.getName());

    public JobViewNode( Node parent, @NotNull final String uuid) {
        super(NODE_ID, NODE_NAME, parent, NODE_ICON_PATH, true);
        this.uuid = uuid;

        this.addClickActionListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                //HDInsightLoader.getHDInsightHelper().openFile(getProject(), uuid);
                HDInsightLoader.getHDInsightHelper().openJobViewEditor(getProject(), uuid);
            }
        });
        this.loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
    }
}
