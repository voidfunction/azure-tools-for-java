package com.microsoft.azure.hdinsight.sdk.subscription;
import java.util.List;

public class TenantList {

    private List<Tenant> value;

    public List<Tenant> getValue(){
        return value;
    }

    public TenantList(List<Tenant> tenantList){
        value = tenantList;
    }
}
