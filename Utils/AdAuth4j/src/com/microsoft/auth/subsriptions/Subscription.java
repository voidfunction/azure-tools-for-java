package com.microsoft.auth.subsriptions;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Subscription {

    public  Subscription(){}
        
    @XmlElement(name="SubscriptionID")
    private String  subscriptionId;
    
    @XmlElement(name="SubscriptionName")
    private String subscriptionName;
    
    @XmlElement(name="SubscriptionStatus")
    private String subscriptionStatus;

    @XmlElement(name="AADTenantID")
    private String tenantId;

/*    
    @XmlElement(name="AccountAdminLiveEmailId")
    private String accountAdminLiveEmailId;
    
    @XmlElement(name="ServiceAdminLiveEmailId")
    private String serviceAdminLiveEmailId;
    
    @XmlElement(name="MaxCoreCount")
    private int maxCoreCount;
    
    @XmlElement(name="MaxStorageAccounts")
    private int maxStorageAccounts;
    
    @XmlElement(name="MaxHostedServices")
    private int maxHostedServices;
    
    @XmlElement(name="CurrentCoreCount")
    private int currentCoreCount;
    
    @XmlElement(name="CurrentHostedServices")
    private int currentHostedServices;
    
    @XmlElement(name="CurrentStorageAccounts")
    private int currentStorageAccounts;
    
    @XmlElement(name="MaxVirtualNetworkSites")
    private int maxVirtualNetworkSites;
    
    @XmlElement(name="CurrentVirtualNetworkSites")
    private int currentVirtualNetworkSites;
    
    @XmlElement(name="MaxLocalNetworkSites")
    private int maxLocalNetworkSites;
    
    @XmlElement(name="MaxDnsServers")
    private int  maxDnsServers;
    
    @XmlElement(name="OfferCategories")
    private String offerCategories;
    
    @XmlElement(name="CreatedTime")
    private String createdTime;
    
    @XmlElement(name="MaxReservedIPs")
    private int maxReservedIPs;
    
    @XmlElement(name="CurrentReservedIPs")
    private int currentReservedIPs;
    
    @XmlElement(name="MaxPublicIPCount")
    private int maxPublicIPCount;
 */
        
    Subscription(String subscriptionId, String subscriptionName, String subscriptionStatus, String tenantId) {
        this.subscriptionName = subscriptionName;
        this.subscriptionId = subscriptionId;
        this.subscriptionStatus = subscriptionStatus;
        this.tenantId = tenantId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }
    public String getAADTenantId() {
        return tenantId;
    }
    public String getSubscriptionName() {
        return subscriptionName;
    }
}


