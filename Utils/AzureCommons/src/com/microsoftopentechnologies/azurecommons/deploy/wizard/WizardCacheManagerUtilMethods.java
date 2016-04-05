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

package com.microsoftopentechnologies.azurecommons.deploy.wizard;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;
import com.microsoft.tooling.msservices.model.vm.Location;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateListResponse.Certificate;
import com.microsoftopentechnologies.azurecommons.deploy.util.MethodUtils;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azurecommons.exception.RestAPIException;
import com.microsoftopentechnologies.azurecommons.wacommonutil.PreferenceSetUtil;
import com.microsoftopentechnologies.azuremanagementutil.exception.InvalidThumbprintException;
import com.microsoftopentechnologies.azuremanagementutil.model.KeyName;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;
import com.microsoftopentechnologies.azuremanagementutil.rest.WindowsAzureStorageServices;

public final class WizardCacheManagerUtilMethods {
	public final static String STORAGE_ACCOUNT_DEFAULT_ACCOUNT_TYPE = "Standard_GRS";

	public static WindowsAzureStorageServices createStorageServiceHelper(
			PublishData currentPublishData,
			String currentStorageService,
			KeyName currentAccessKey) {
		if (currentPublishData != null) {
			StorageAccount storageService =
					getCurrentStorageAcount(currentPublishData, currentStorageService);
			try {
				String key = ""; //$NON-NLS-1$
				if (currentAccessKey == KeyName.Primary)
					key = storageService.getPrimaryKey();
				else
					key = storageService.getSecondaryKey();

				return new WindowsAzureStorageServices(
						storageService, key);
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static List<Location> getLocation(PublishData currentPublishData) {

		if (currentPublishData != null)
			return currentPublishData.getLocationsPerSubscription().get(
					currentPublishData.getCurrentSubscription().getId());

		return null;
	}

	public static Subscription findSubscriptionByName(String subscriptionName,
			List<PublishData> PUBLISHS) {

		for (PublishData pd : PUBLISHS) {
			List<Subscription> subscriptions = pd.getPublishProfile()
					.getSubscriptions();
			for (Subscription sub : subscriptions) {
				if (sub.getName().equals(subscriptionName))
					return sub;
			}
		}

		return null;
	}

	public static PublishData findPublishDataBySubscriptionId(String subscriptionId,
			List<PublishData> PUBLISHS) {
		for (PublishData pd : PUBLISHS) {
			if (pd.getSubscriptionIds().contains(subscriptionId)) {
				return pd;
			}
		}
		return null;
	}

	public static String findSubscriptionNameBySubscriptionId(String subscriptionId,
			List<PublishData> PUBLISHS) {
		for (PublishData pd : PUBLISHS) {
			if (pd.getSubscriptionIds().contains(subscriptionId)) {
				for (Subscription sub : pd.getPublishProfile().getSubscriptions()) {
					if (sub.getId().equals(subscriptionId)) {
						return sub.getName();
					}
				}
			}
		}
		return null;
	}

	public static void changeCurrentSubscription(PublishData publishData, String subscriptionId) {
		if (publishData == null || subscriptionId == null) {
			return;
		}
		List<Subscription> subs = publishData.getPublishProfile().getSubscriptions();
		for (int i = 0; i < subs.size(); i++) {
			Subscription s = subs.get(i);

			if (s.getSubscriptionID().equals(subscriptionId)) {
				publishData.setCurrentSubscription(s);
				break;
			}
		}
	}

	public static StorageAccount getCurrentStorageAcount(PublishData currentPublishData,
			String currentStorageService) {

		if (currentPublishData != null && (currentStorageService != null && !currentStorageService.isEmpty())) {

			for (StorageAccount storageService : currentPublishData
					.getStoragesPerSubscription()
					.get(currentPublishData.getCurrentSubscription().getId())) {
				if (storageService.getName().equalsIgnoreCase(
						currentStorageService))
					return storageService;
			}
		}
		return null;
	}

	public static CloudService getCurentHostedService(PublishData currentPublishData,
			String currentHostedService) {
		if (currentPublishData != null && (currentHostedService != null && !currentHostedService.isEmpty())) {
			String subsId = currentPublishData.getCurrentSubscription().getId();

			for (CloudService cloudService : currentPublishData.getServicesPerSubscription().get(subsId)) {
				if (cloudService.getName().equalsIgnoreCase(currentHostedService))
					return cloudService;
			}
		}
		return null;
	}

	public static CloudService getHostedServiceFromCurrentPublishData(final String hostedServiceName,
			PublishData currentPublishData) {
		if (currentPublishData != null) {
			String subsId = currentPublishData.getCurrentSubscription().getId();

			for (CloudService cloudService : currentPublishData.getServicesPerSubscription().get(subsId)) {
				if (cloudService.getName().equalsIgnoreCase(hostedServiceName))
					return cloudService;
			}
		}
		return null;
	}

	/**
	 * Method uses REST API and returns already uploaded certificates
	 * from currently selected cloud service on wizard.
	 * @return
	 */
	public static List<Certificate> fetchUploadedCertificates(PublishData currentPublishData,
			String currentHostedService) throws AzureCmdException {
		List<Certificate> certsInService = null;
		AzureManagerImpl.getManager().getCertificates(currentPublishData.getCurrentSubscription().getId(),
				getCurentHostedService(currentPublishData, currentHostedService).getName());
		return certsInService;
	}

	public static CloudService createHostedService(CloudService cloudService, PublishData currentPublishData)
					throws AzureCmdException {
		Subscription subscription = currentPublishData.getCurrentSubscription();

		String subscriptionId = subscription.getId();
		AzureManagerImpl.getManager().createCloudService(cloudService);
		// todo?
		// do we really need this call?
		cloudService = AzureManagerImpl.getManager().getCloudServiceDetailed(cloudService);
		// remove previos mock if existed
		for (CloudService cs : currentPublishData.getServicesPerSubscription().get(subscriptionId)) {
			if (cloudService.getName().equals(cs.getName())) {
				currentPublishData.getServicesPerSubscription().get(subscriptionId).remove(cs);
				break; // important to avoid exception
			}
		}
		return cloudService;
	}

	public static StorageAccount createStorageAccount(String name, String label, String location, String description,
													  PublishData currentPublishData, String prefFilePath)
			throws Exception {
		StorageAccount storageAccount = new StorageAccount(name, currentPublishData.getCurrentSubscription().getId());
		storageAccount.setLabel(label);
		storageAccount.setLocation(location);
		storageAccount.setDescription(description);
		storageAccount.setType(STORAGE_ACCOUNT_DEFAULT_ACCOUNT_TYPE);

		OperationStatusResponse response = AzureManagerImpl.getManager().createStorageAccount(storageAccount);

		AzureManagerImpl.getManager().waitForStatus(currentPublishData.getCurrentSubscription().getId(), response);

		storageAccount = AzureManagerImpl.getManager().refreshStorageAccountInformation(storageAccount);

		String mngmntUrl = MethodUtils.getManagementUrlAsPerPubFileVersion(currentPublishData,
				currentPublishData.getCurrentSubscription(), prefFilePath);
		String chinaMngmntUrl = PreferenceSetUtil.getManagementURL("windowsazure.cn (China)", prefFilePath);
		chinaMngmntUrl = chinaMngmntUrl.substring(0, chinaMngmntUrl.lastIndexOf("/"));
		if (mngmntUrl.equals(chinaMngmntUrl)) {
			if (storageAccount.getBlobsUri().startsWith("https://")) {
				storageAccount.setBlobsUri(storageAccount.getBlobsUri().replaceFirst("https://", "http://"));
			}
			if (storageAccount.getQueuesUri().startsWith("https://")) {
				storageAccount.setQueuesUri(storageAccount.getQueuesUri().replaceFirst("https://", "http://"));
			}
			if (storageAccount.getTablesUri().startsWith("https://")) {
				storageAccount.setTablesUri(storageAccount.getTablesUri().replaceFirst("https://", "http://"));
			}
		}
		return storageAccount;
	}

//	private static OperationStatus waitForStatus(Configuration configuration,
//			WindowsAzureServiceManagement service, String requestId)
//					throws Exception {
//		OperationStatusResponse op;
//		OperationStatus status = null;
//		do {
//			op = service.getOperationStatus(configuration, requestId);
//			status = op.getStatus();
//
//			if (op.getError() != null) {
//				throw new RestAPIException(op.getError().getMessage());
//			}
//			Thread.sleep(5000);
//
//		} while (status == OperationStatus.InProgress);
//		return status;
//	}

	public static boolean isHostedServiceNameAvailable(final String hostedServiceName,
			PublishData currentPublishData)
					throws Exception, RestAPIException {
			return AzureManagerImpl.getManager()
					.checkHostedServiceNameAvailability(currentPublishData.getCurrentSubscription().getId(), hostedServiceName);
	}

	public static boolean isStorageAccountNameAvailable(final String storageAccountName, PublishData currentPublishData)
					throws AzureCmdException {
		return AzureManagerImpl.getManager()
					.checkStorageNameAvailability(currentPublishData.getCurrentSubscription().getId(),storageAccountName);
	}

	public static StorageAccount createStorageServiceMock(String storageAccountNameToCreate,
			String storageAccountLocation,
			String description) {
		StorageAccount storageAccount = new StorageAccount(storageAccountNameToCreate, null);
		storageAccount.setLocation(storageAccountLocation);
		storageAccount.setDescription(description);

		return storageAccount;
	}

	public static CloudService createHostedServiceMock(String hostedServiceNameToCreate,
			String hostedServiceLocation,
			String description) {
//		HostedServiceProperties props = new HostedServiceProperties();
//		props.setDescription(description);
//		props.setLocation(hostedServiceLocation);
//
//		HostedService hostedService = new HostedService();
//		hostedService.setProperties(props);
//		hostedService.setServiceName(hostedServiceNameToCreate);
		CloudService cloudService = new CloudService(hostedServiceNameToCreate, hostedServiceLocation, "", null);
		return cloudService;
	}

	public static List<CloudService> getHostedServices(PublishData currentPublishData) {
		if (currentPublishData == null)
			return null;
		String subbscriptionId = currentPublishData.getCurrentSubscription().getId();
		return currentPublishData.getServicesPerSubscription().get(subbscriptionId);
	}

	public static CloudService getHostedServiceWithDeployments(CloudService cloudService, PublishData currentPublishData)
					throws Exception, InvalidThumbprintException {
		return AzureManagerImpl.getManager().getCloudServiceDetailed(cloudService);
	}

	public static boolean empty(PublishData data) {

		Map<String, List<CloudService>> hostedServices = data.getServicesPerSubscription();
		if (hostedServices == null || hostedServices.keySet().isEmpty()) {
			return true;
		}
		Map<String, List<StorageAccount>> storageServices = data.getStoragesPerSubscription();
		if (storageServices == null || storageServices.keySet().isEmpty()) {
			return true;
		}
		Map<String , List<Location>> locations = data.getLocationsPerSubscription();
		return locations == null || locations.keySet().isEmpty();
	}

	public static StorageAccount getStorageAccountFromCurrentPublishData(String storageAccountName,
			PublishData currentPublishData) {
		if (currentPublishData != null) {
			for (StorageAccount storageService : currentPublishData
					.getStoragesPerSubscription()
					.get(currentPublishData.getCurrentSubscription().getId())) {
				if (storageService.getName().equalsIgnoreCase(
						storageAccountName))
					return storageService;
			}
		}
		return null;
	}

	public static String checkSchemaVersionAndReturnUrl(PublishData currentPublishData) {
		String url = null;
		String schemaVer = currentPublishData.getPublishProfile().getSchemaVersion();
		if (schemaVer != null && !schemaVer.isEmpty() && schemaVer.equalsIgnoreCase("2.0")) {
			// publishsetting file is of schema version 2.0
			url = currentPublishData.getCurrentSubscription().getServiceManagementUrl();
		} else {
			url = currentPublishData.getPublishProfile().getUrl();
		}
		return url;
	}
	
	public static int getIndexOfPublishData(String subscriptionId, List<PublishData> PUBLISHS) {
		int index = 0;
		for (int i = 0; i < PUBLISHS.size(); i++) {
			if (PUBLISHS.get(i).getSubscriptionIds().contains(subscriptionId)) {
				index = i;
			}
		}
		return index;
	}
}
