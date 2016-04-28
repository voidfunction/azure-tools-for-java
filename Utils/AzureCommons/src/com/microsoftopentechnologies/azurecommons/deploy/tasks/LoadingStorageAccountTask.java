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

package com.microsoftopentechnologies.azurecommons.deploy.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.storage.StorageAccount;
import com.microsoftopentechnologies.azurecommons.deploy.util.PublishData;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageService;
import com.microsoftopentechnologies.azuremanagementutil.model.StorageServices;
import com.microsoftopentechnologies.azuremanagementutil.model.Subscription;


public class LoadingStorageAccountTask extends LoadingTask<Map<String, List<StorageAccount>>> {

	public LoadingStorageAccountTask(PublishData data, Object projectObject) {
		super(data);
		this.projectObject = projectObject;
	}

	private static final int OPERATION_TIMEOUT = 120;

//	private final WindowsAzureServiceManagement service = getServiceInstance();
	private final Map<String, List<StorageAccount>> storageServicesMap = new ConcurrentHashMap<String, List<StorageAccount>>();
	private List<Future<?>> futures = new ArrayList<Future<?>>();
	private ScheduledExecutorService threadPool;

	private Object projectObject;

	@Override
	public Map<String, List<StorageAccount>> call() throws Exception {
		List<Subscription> subscriptions = data.getPublishProfile().getSubscriptions();
		if (!subscriptions.isEmpty()) {
			int numberOfSubscriptions = data.getPublishProfile().getSubscriptions().size();
			threadPool = Executors.newScheduledThreadPool(numberOfSubscriptions);
			for (Subscription sub : data.getPublishProfile().getSubscriptions()) {
				LoadStorageAccountsPerSubscription task = new LoadStorageAccountsPerSubscription();
				task.setSubscriptionId(sub.getId());
				Future<?> submit = threadPool.submit(task);
				futures.add(submit);
			}
			try {
				for (Future<?> future : futures) {
					future.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
				}
			}
			catch (TimeoutException e) {
				AccountCachingExceptionEvent event = new AccountCachingExceptionEvent(this);
				event.setException(e);
				event.setMessage("Timed out while waiting for storage services, please try again");
				threadPool.shutdownNow();
				fireRestAPIErrorEvent(event);
				return new ConcurrentHashMap<String, List<StorageAccount>>();
			}
			catch (InterruptedException e) {
				return new ConcurrentHashMap<String, List<StorageAccount>>();
			}
		}

		return storageServicesMap;
	}

	@Override
	protected void setDataResult(Map<String, List<StorageAccount>> data) {
		this.data.setStoragesPerSubscription(data);
		if (!data.keySet().isEmpty()) {
			fireOnLoadedStorageServicesEvent();
		}
	}

	class LoadStorageAccountsPerSubscription implements Runnable {

		private String subscriptionId;

		public void setSubscriptionId(String id) {
			this.subscriptionId = id;
		}

		@Override
		public void run() {
			List<StorageAccount> storageAccountsForSubscription;
			try {
				storageAccountsForSubscription = AzureManagerImpl.getManager(projectObject).getStorageAccounts(subscriptionId, true);
				List<StorageService> storageServicesForSubscription = new ArrayList<StorageService>();
				for (StorageAccount storageAccount : storageAccountsForSubscription) {
					StorageService storageService = new StorageService(storageAccount);
					storageServicesForSubscription.add(storageService);
				}
				StorageServices services = new StorageServices();
				services.setStorageServices(storageServicesForSubscription);
				storageServicesMap.put(subscriptionId, storageAccountsForSubscription);
			} catch (Exception e) {
				e.printStackTrace();
			} 
        }
	}

	private void fireOnLoadedStorageServicesEvent() {
		Object[] list = listeners.getListenerList();
		for (int i = 0; i < list.length; i += 2) {
			if (list[i] == LoadingAccoutListener.class) {
				((LoadingAccoutListener) list[i + 1]).onLoadedStorageServices();
			}
		}	
	}

}
