package com.microsoftopentechnologies.windowsazurestorage;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.microsoft.windowsazure.storage.CloudStorageAccount;
import com.microsoft.windowsazure.storage.RetryNoRetry;
import com.microsoft.windowsazure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.windowsazure.storage.StorageException;
import com.microsoft.windowsazure.storage.blob.BlobContainerPermissions;
import com.microsoft.windowsazure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.windowsazure.storage.blob.CloudBlob;
import com.microsoft.windowsazure.storage.blob.CloudBlobClient;
import com.microsoft.windowsazure.storage.blob.CloudBlobContainer;
import com.microsoft.windowsazure.storage.blob.CloudBlobDirectory;
import com.microsoft.windowsazure.storage.blob.ListBlobItem;

public class ListBlobs {
	public static String storageAccountName = "sureshtest";
	public static String storageAccountKey = "3lZBWozjIHg4w12WWKBGVhU3mtuXyqewI08fktpFnpZttvYj1BsKQH38WTWlkiz3XXY1H8Ee014P8vP16/dDMA==";
	public static String containerName = "test";

	public static void main(String[] args) throws Exception {

		// Get container reference;
		CloudBlobContainer container = getBlobContainerReference(
				storageAccountName, storageAccountKey, containerName, false,
				false, null);

		if (container != null) {
			for (ListBlobItem blobItem : container.listBlobs()) {
				if (blobItem instanceof CloudBlob) {
					System.out.println(((CloudBlob) blobItem).getName());
					;
				} else if (blobItem instanceof CloudBlobDirectory) {
					listBlobDirectory((CloudBlobDirectory) blobItem);
				}

			}

		}

	}

	private static void listBlobDirectory(CloudBlobDirectory cloudBlobDirectory)
			throws Exception {

		System.out.println("Blob Directory Prefix "
				+ cloudBlobDirectory.getPrefix());

		for (ListBlobItem blobItem : cloudBlobDirectory.listBlobs()) {
			if (blobItem instanceof CloudBlob) {
				System.out.println(((CloudBlob) blobItem).getName());
				;
			} else if (blobItem instanceof CloudBlobDirectory) {
				listBlobDirectory((CloudBlobDirectory) blobItem);
			}

		}
	}

	private static CloudBlobContainer getBlobContainerReference(String accName,
			String key, String containerName, boolean createCnt,
			boolean allowRetry, Boolean cntPubAccess)
			throws URISyntaxException, StorageException {

		CloudStorageAccount cloudStorageAccount;
		CloudBlobClient serviceClient;
		CloudBlobContainer container;
		StorageCredentialsAccountAndKey credentials;

		credentials = new StorageCredentialsAccountAndKey(accName, key);
		cloudStorageAccount = new CloudStorageAccount(credentials,
				new URI("http://sureshtest.blob.core.windows.net/"),
				new URI("http://sureshtest.queue.core.windows.net/"),
				new URI("http://sureshtest.table.core.windows.net/"));
		

		serviceClient = cloudStorageAccount.createCloudBlobClient();

		if (!allowRetry) {
			// Setting no retry policy
			RetryNoRetry rnr = new RetryNoRetry();
			serviceClient.setRetryPolicyFactory(rnr);
		}

		container = serviceClient.getContainerReference(containerName);

		boolean cntExists = container.exists();

		if (createCnt && !cntExists) {
			container.createIfNotExists();
		}

		// Apply permissions only if container is created newly
		if (!cntExists && cntPubAccess != null) {
			// Set access permissions on container.
			BlobContainerPermissions cntPerm;
			cntPerm = new BlobContainerPermissions();
			if (cntPubAccess) {
				cntPerm.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
			} else {
				cntPerm.setPublicAccess(BlobContainerPublicAccessType.OFF);
			}
			container.uploadPermissions(cntPerm);
		}

		return container;
	}

}
