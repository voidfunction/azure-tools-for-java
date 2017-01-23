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
package com.microsoftopentechnologies.azuremanagementutil.task;

import java.util.concurrent.Callable;

import com.microsoft.tooling.msservices.helpers.azure.AzureManager;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoft.windowsazure.Configuration;

public class LoadStorageServiceTask implements Callable<StorageAccount> {
	
	private Configuration configuration;
	private StorageAccount storageAccount;
	

	public StorageAccount call() throws Exception {
		return AzureManager.getManager().refreshStorageAccountInformation(storageAccount);
//		StorageService storageService = WindowsAzureServiceManagement.getStorageKeys(configuration, storageAccount.getName());
//        storageService.setServiceName(storageAccount.getName());
//        storageService.setStorageAccountProperties(storageAccount.getProperties());
//		return storageService;
	}
	
//	public void setConfiguration(Configuration configuration) {
//		this.configuration = configuration;
//	}

	public void setStorageAccount(StorageAccount storageAccount) {
		this.storageAccount= storageAccount;
	}
}
