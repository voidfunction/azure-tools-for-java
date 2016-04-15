package com.microsoft.azure.hdinsight.serverexplore;

import org.jetbrains.annotations.NotNull;

public class UserInfo {
    private final String tenantId;
    private final String uniqueName;

    public UserInfo(@NotNull String tenantId, @NotNull String uniqueName) {
        this.tenantId = tenantId;
        this.uniqueName = uniqueName;
    }

    @NotNull
    public String getTenantId() {
        return this.tenantId;
    }

    @NotNull
    public String getUniqueName() {
        return this.uniqueName;
    }

    @Override
    public boolean equals(Object otherObj) {
        if (otherObj != null && otherObj instanceof UserInfo) {
            UserInfo other = (UserInfo) otherObj;
            return tenantId.equals(other.tenantId) && uniqueName.equals(other.uniqueName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return tenantId.hashCode() * 13 + uniqueName.hashCode();
    }
}