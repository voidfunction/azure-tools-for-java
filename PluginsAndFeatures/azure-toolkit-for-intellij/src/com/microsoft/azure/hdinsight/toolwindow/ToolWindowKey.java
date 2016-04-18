package com.microsoft.azure.hdinsight.toolwindow;

import com.intellij.openapi.project.Project;

public class ToolWindowKey {
    private Project project;
    private String toolWindowFactoryId;

    public ToolWindowKey(Project project, String toolWindowFactoryId) {
        this.project = project;
        this.toolWindowFactoryId = toolWindowFactoryId;
    }

    @Override
    public boolean equals(Object obj) {
        ToolWindowKey otherToolWindowKey = (ToolWindowKey) obj;
        if (otherToolWindowKey == null) {
            return false;
        }

        return (this.project == otherToolWindowKey.project) &&
                (this.toolWindowFactoryId == otherToolWindowKey.toolWindowFactoryId);
    }

    @Override
    public int hashCode() {
        return this.project.hashCode() * 5 + this.toolWindowFactoryId.hashCode();
    }
}