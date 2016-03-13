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
package com.microsoftopentechnologies.windowsazure.tools.build;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.tooling.msservices.model.vm.CloudService;

public class Utils {
	private static String eclipseDeployContainer = "eclipsedeploy";
	private final static String FWD_SLASH = "/";
	public final static String STORAGE_ACCOUNT_DEFAULT_ACCOUNT_TYPE = "Standard_GRS";

//	public static OperationStatus waitForStatus(Configuration configuration,
//			WindowsAzureServiceManagement service, String requestId) throws Exception {
//		OperationStatusResponse op;
//		OperationStatus status = null;
//		do {
//			op = service.getOperationStatus(configuration, requestId);
//			status = op.getStatus();
//			if (op.getError() != null) {
//				System.out.println("Error Message: " + op.getError().getMessage());
//				throw new Exception(op.getError().getMessage());
//			}
//			Thread.sleep(10000);
//		} while (status == OperationStatus.InProgress);
//		return status;
//	}
	
	public static StorageAccount createStorageAccountIfNotExists(String subscriptionId,
																 String storageAccountName,
																 String region,
																 String managementUrl) throws Exception {
		System.out.println("Creating storage account : '" + storageAccountName + "' if does not exists");
		ProgressBar progressBar = new ProgressBar(10000, "Creating storage account");
		Thread progressBarThread = new Thread(progressBar);
		progressBarThread.start();
		StorageAccount storageAccount = new StorageAccount(storageAccountName, subscriptionId);
		storageAccount.setLocation(region);
		boolean isStorageAccountExist = false;
		List<StorageAccount> storageAccountList = AzureManagerImpl.getManager().getStorageAccounts(subscriptionId, false);
		for (StorageAccount storageService : storageAccountList) {
			if (storageService.getName().equalsIgnoreCase(storageAccountName)) {
				isStorageAccountExist = true;
				System.out.println("Storage account " + storageAccountName + " already exists");
				break;
			}
		}
		if (!isStorageAccountExist) {
			storageAccount.setLabel(storageAccountName);
			storageAccount.setType(STORAGE_ACCOUNT_DEFAULT_ACCOUNT_TYPE);
			AzureManagerImpl.getManager().createStorageAccount(storageAccount);
		}
		progressBarThread.interrupt();
		try {
			progressBarThread.join();
		} catch (InterruptedException e) {
			;
		}
		// Get storage account object
		storageAccount = AzureManagerImpl.getManager().refreshStorageAccountInformation(storageAccount);
		if (managementUrl.equals("https://management.core.chinacloudapi.cn")) {
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

	public static void createCloudServiceIfNotExists(String subscriptionId, String cloudServiceName, String region) throws Exception {
		boolean isCloudServiceExist = false;
		List<CloudService> list = AzureManagerImpl.getManager().getCloudServices(subscriptionId);
		for (CloudService hostedService : list) {
			if (hostedService.getName().equalsIgnoreCase(cloudServiceName)) {
				isCloudServiceExist = true;
				break;
			}
		}
		if (!isCloudServiceExist) {
			AzureManagerImpl.getManager().createCloudService(new CloudService(cloudServiceName, region, null, subscriptionId, cloudServiceName));
		}
	}

	public static void uploadCertificate(String subscriptionId,
								  String cloudServiceName,
								  String pfxPath, String pfxPwd) throws Exception {
		File pfxFile = new File(pfxPath);
		byte[] buff = new byte[(int) pfxFile.length()];
		FileInputStream fileInputStram = null;
		DataInputStream dis = null;
		try {
			fileInputStram = new FileInputStream(pfxFile);
			dis = new DataInputStream(fileInputStram);
			dis.readFully(buff);
		}
		finally {
			if (fileInputStram != null) {
				fileInputStram.close();
			}
			if (dis != null) {
				dis.close();
			}
		}
		AzureManagerImpl.getManager().createServiceCertificate(subscriptionId, cloudServiceName, buff, pfxPwd, false);
	}
	
	public static String prepareCloudBlobURL(String filePath,
			String newUrl) {
		if ((filePath == null || filePath.length() == 0)
				|| (newUrl == null || newUrl.length() == 0)) {
			return "";
		}

		File jdkPath = new File(filePath);
		return new StringBuilder(newUrl).append(eclipseDeployContainer)
				.append(FWD_SLASH).append(jdkPath.getName().trim().replaceAll("\\s+", "-"))
				.append(".zip").toString();
	}

	public static String prepareUrlForApp(String asName,
			String url) {
		if ((asName == null || asName.length() == 0)
				|| (url == null || url.length() == 0)) {
			return "";
		}

		return new StringBuilder(url).append(eclipseDeployContainer)
				.append(FWD_SLASH).append(asName).toString();
	}

	public static String prepareUrlForThirdPartyJdk(String cloudValue, String url) {
		String finalUrl = "";
		String dirName = cloudValue.substring(cloudValue.lastIndexOf("\\") + 1,
				cloudValue.length());
		finalUrl = new StringBuilder(url)
		.append(eclipseDeployContainer).append(FWD_SLASH)
		.append(dirName).append(".zip").toString();
		return finalUrl;
	}
	
	/*
	 * Checks if given parameter is null or empty
	 */
	public static boolean isNullOrEmpty(String value) {
		return (value == null) || (value.trim().length() == 0);
		
	}
	
	/*
	 * Checks if given parameter is not null and not empty
	 */
	public static boolean isNotNullOrEmpty(String value) {
		return (value != null) && (value.trim().length() > 0);		
	}
	
    /**
     * Checks is file path is valid
     * @param filePath
     * @return
     */
	public static boolean isValidFilePath(String filePath) {
		if (isNullOrEmpty(filePath)) {
			return false;
		}
		
		String path = filePath.trim();
		
		// Validate publish settings path
		File file = new File(path);
		if ((!file.exists()) || file.isDirectory()) {
			return false; 				
		} else {
			return true;
		}
	}
}
