package com.microsoft.azure.hdinsight.common;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.node.Node;
import com.microsoft.azure.hdinsight.serverexplore.node.NodeActionListener;

import java.util.List;
import java.util.Map;

public class DefaultLoader {

    private static UIHelper uiHelper;
    private static IDEHelper ideHelper;
    private static Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions;

    public static void setUiHelper(UIHelper uiHelper) {
        DefaultLoader.uiHelper = uiHelper;
    }

    public static void setNode2Actions(Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions) {
        DefaultLoader.node2Actions = node2Actions;
    }

    public static void setIdeHelper(IDEHelper ideHelper) {
        DefaultLoader.ideHelper = ideHelper;
    }

    public static UIHelper getUIHelper() {
        return uiHelper;
    }

    public static List<Class<? extends NodeActionListener>> getActions(Class<? extends Node> nodeClass) {
        return node2Actions.get(nodeClass);
    }

    public static IDEHelper getIdeHelper() {
        return ideHelper;
    }
}