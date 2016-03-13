package com.microsoftopentechnologies.windowsazurestorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import hudson.FilePath;

import org.apache.commons.lang.time.DurationFormatUtils;

import com.microsoft.windowsazure.storage.CloudStorageAccount;
import com.microsoft.windowsazure.storage.RetryNoRetry;
import com.microsoft.windowsazure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.windowsazure.storage.StorageException;
import com.microsoft.windowsazure.storage.blob.BlobContainerPermissions;
import com.microsoft.windowsazure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.windowsazure.storage.blob.BlobRequestOptions;
import com.microsoft.windowsazure.storage.blob.CloudBlobClient;
import com.microsoft.windowsazure.storage.blob.CloudBlobContainer;
import com.microsoft.windowsazure.storage.blob.CloudBlockBlob;

public class Upload {
	public static String storageAccountName = "interopdemosteststore";
	public static String storageAccountKey = "wEVuYVsk7uDGwYTdxRvTmfpj7UzZy0ieOteIaMMIgyz9Yf2+HpZmsHHleVGzKRo43h/eTBklqDrDcu1utQaG2Q==";
	public static String containerName = "test";
	public static String fileToUpload = "C:/eclipseworld/eclipseworld.zip";

	
	public static void main(String[] args) throws Exception {
		CloudBlobContainer container = getBlobContainerReference(
				storageAccountName, storageAccountKey, containerName, false,
				false, null);
		
		FilePath fs = new FilePath(new File(fileToUpload));
		
		CloudBlockBlob cb = container.getBlockBlobReference("kepler.zip");
		
		long startTime = System.currentTimeMillis();
		
		InputStream inputStream = fs.read();
		try {
			cb.upload(inputStream, fs.length(), null, getBlobRequestOptions(), null);
		} finally {
			try {
				inputStream.close();
				
			} catch (IOException e) {

			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time taken to upload "
				+ getTime(endTime - startTime));
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
				new URI("http://interopdemosteststore.blob.core.windows.net/"),
				new URI("http://interopdemosteststore.queue.core.windows.net/"),
				new URI("http://interopdemosteststore.table.core.windows.net/"));
		

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
	
	private static BlobRequestOptions getBlobRequestOptions() {
		BlobRequestOptions options = new BlobRequestOptions();
		
		int concurrentRequestCount = 1;
		
		try {
			concurrentRequestCount = Runtime.getRuntime().availableProcessors() * 2 ;
		} catch (Exception e) {
			// Just ignore
			e.printStackTrace();
		}
		System.out.println("concurrentRequestCount"+concurrentRequestCount);
		concurrentRequestCount = 4;
		options.setConcurrentRequestCount(concurrentRequestCount);
		
		return options;
	}
	
	public static String getTime(long timeInMills) {
	    return DurationFormatUtils.formatDuration(timeInMills, "HH:mm:ss.S");
	}


}
