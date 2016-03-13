/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoftopentechnologies.azurecommons.deploy.model;

import java.util.Date;

import com.interopbridges.tools.windowsazure.WindowsAzurePackageType;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.windowsazure.Configuration;
import com.microsoftopentechnologies.azuremanagementutil.model.KeyName;

public class DeployDescriptor {

	private final String deploymentId;
	private final Date startTime;
	private final String cspkgFile;
	private final String cscfgFile;
	private final String deployState;
	private final String subscriptionId;
	private final StorageAccount storageAcount;
	private final KeyName accessKey;
	private final CloudService hostedService;
	private final RemoteDesktopDescriptor remoteDesktopDescriptor;
	private final WindowsAzurePackageType deployMode;
    private final String mngUrl;
	private final String unpublish;
	private final CertificateUploadList certList;
	private final boolean displayHttpsLink;
    private final Configuration configuration;

	public DeployDescriptor(WindowsAzurePackageType deployMode,
							String subscriptionId, StorageAccount storageAcount,
							KeyName accessKey, CloudService hostedService, String cspkgFile,
							String cscfgFile, String deployState,
							RemoteDesktopDescriptor remoteDesktopDescriptor, String mngUrl,
							String unpublish,
							CertificateUploadList certList, boolean displayHttpsLink, Configuration configuration) {
		this.deployMode = deployMode;
		this.startTime = new Date();
		this.deploymentId = String.format("%s - %s", hostedService.getName(), deployState);
		this.subscriptionId = subscriptionId;
		this.storageAcount = storageAcount;
		this.accessKey = accessKey;
		this.hostedService = hostedService;
		this.remoteDesktopDescriptor = remoteDesktopDescriptor;
		this.cspkgFile = cspkgFile;
		this.cscfgFile = cscfgFile;
		this.deployState = deployState;
        this.mngUrl = mngUrl;
		this.unpublish = unpublish;
		this.certList = certList;
		this.displayHttpsLink=displayHttpsLink;
        this.configuration = configuration;
	}

	public WindowsAzurePackageType getDeployMode() {
		return deployMode;
	}

	/**
	 * @return the deploymentId
	 */
	public String getDeploymentId() {
		return deploymentId;
	}

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public StorageAccount getStorageAccount() {
		return storageAcount;
	}

	public String getStorageKey() {
		if (accessKey == KeyName.Primary) {
			return storageAcount.getPrimaryKey();
		}
		return storageAcount.getSecondaryKey();
	}

	public CloudService getHostedService() {
		return hostedService;
	}

	public String getCspkgFile() {
		return cspkgFile;
	}

	public String getCscfgFile() {
		return cscfgFile;
	}

	public String getDeployState() {
		return deployState;
	}

	/**
	 * @return the storageAcountDescriptor
	 */
	public StorageAccount getStorageAcountDescriptor() {
		return storageAcount;
	}

	/**
	 * @return the remoteDesktopDescriptor
	 */
	public RemoteDesktopDescriptor getRemoteDesktopDescriptor() {
		return remoteDesktopDescriptor;
	}

	/**
	 * @return the enableRemoteDesktop
	 */
	public boolean isEnableRemoteDesktop() {
		return remoteDesktopDescriptor.isEnabled();
	}

	public boolean isStartRdpOnDeploy() {
		return remoteDesktopDescriptor.isStartRemoteRDP();
	}

    public String getMngUrl() {
        return mngUrl;
    }

    public String getUnpublish() {
		return unpublish;
	}

	public CertificateUploadList getCertList() {
		return certList;
	}
	
	public boolean getDisplayHttpsLink() {
		return displayHttpsLink;
	}

    public Configuration getConfiguration() {
        return configuration;
    }
}