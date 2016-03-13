/*
 Copyright 2013 Microsoft Open Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.microsoftopentechnologies.windowsazurestorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.time.DurationFormatUtils;

import jenkins.model.Jenkins;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import com.microsoft.windowsazure.storage.CloudStorageAccount;
import com.microsoft.windowsazure.storage.RetryNoRetry;
import com.microsoft.windowsazure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.windowsazure.storage.StorageException;
import com.microsoft.windowsazure.storage.blob.BlobContainerPermissions;
import com.microsoft.windowsazure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.windowsazure.storage.blob.BlobRequestOptions;
import com.microsoft.windowsazure.storage.blob.CloudBlob;
import com.microsoft.windowsazure.storage.blob.CloudBlobClient;
import com.microsoft.windowsazure.storage.blob.CloudBlobContainer;
import com.microsoft.windowsazure.storage.blob.CloudBlobDirectory;
import com.microsoft.windowsazure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.storage.blob.ListBlobItem;
import com.microsoftopentechnologies.windowsazurestorage.beans.StorageAccountInfo;
import com.microsoftopentechnologies.windowsazurestorage.exceptions.WAStorageException;
import com.microsoftopentechnologies.windowsazurestorage.helper.Utils;

public class WAStorageClient {

	/*
	 * A random name for container name to test validity of storage account
	 * details
	 */
	private static final String TEST_CNT_NAME = "testcheckfromjenkins";
	// listing blobs on CloudBlobDirectory is giving empty file with name
	// "$$$.$$$". need
	// to filter this while downloading
	private static final String EMPTY_FILE_NAME = "$$$.$$$";
	private static final String BLOB = "blob";
	private static final String QUEUE = "queue";
	private static final String TABLE = "table";

	private static final String fpSeparator = ";";

	/**
	 * This method validates Storage Account credentials by checking for a dummy
	 * conatiner existence.
	 * 
	 * @param storageAccountName
	 * @param storageAccountKey
	 * @param blobEndPointURL
	 * @return true if valid
	 * @throws WAStorageException
	 */
	public static boolean validateStorageAccount(
			final String storageAccountName, final String storageAccountKey,
			final String blobEndPointURL) throws WAStorageException {
		try {
			// Get container reference
			CloudBlobContainer container = getBlobContainerReference(
					storageAccountName, storageAccountKey, blobEndPointURL,
					TEST_CNT_NAME, false, false, null);
			container.exists();

		} catch (Exception e) {
			e.printStackTrace();
			throw new WAStorageException(Messages.Client_SA_val_fail());
		}
		return true;
	}

	/**
	 * Returns reference of Windows Azure cloud blob container.
	 * 
	 * @param accName
	 *            storage account name
	 * @param key
	 *            storage account primary access key
	 * @param blobURL
	 *            blob service endpoint url
	 * @param containerName
	 *            name of the container
	 * @param createCnt
	 *            Indicates if container needs to be created
	 * @param allowRetry
	 *            sets retry policy
	 * @param cntPubAccess
	 *            Permissions for container
	 * @return reference of CloudBlobContainer
	 * @throws URISyntaxException
	 * @throws StorageException
	 */
	private static CloudBlobContainer getBlobContainerReference(String accName,
			String key, String blobURL, String containerName,
			boolean createCnt, boolean allowRetry, Boolean cntPubAccess)
			throws URISyntaxException, StorageException {

		CloudStorageAccount cloudStorageAccount;
		CloudBlobClient serviceClient;
		CloudBlobContainer container;
		StorageCredentialsAccountAndKey credentials;

		credentials = new StorageCredentialsAccountAndKey(accName, key);

		if (Utils.isNullOrEmpty(blobURL) || blobURL.equals(Utils.DEF_BLOB_URL)) {
			cloudStorageAccount = new CloudStorageAccount(credentials);
		} else {
			cloudStorageAccount = new CloudStorageAccount(credentials, new URI(
					blobURL), new URI(getCustomURI(accName, QUEUE, blobURL)),
					new URI(getCustomURI(accName, TABLE, blobURL)));
		}

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

	// Returns custom URL for queue and table.
	private static String getCustomURI(String storageAccountName, String type,
			String blobURL) {

		if (QUEUE.equalsIgnoreCase(type)) {
			return blobURL.replace(storageAccountName + "." + BLOB,
					storageAccountName + "." + type);
		} else if (TABLE.equalsIgnoreCase(type)) {
			return blobURL.replace(storageAccountName + "." + BLOB,
					storageAccountName + "." + type);
		} else {
			return null;
		}
	}

	public static List<String> getContainersList(
			StorageAccountInfo storageAccount, boolean allowRetry)
			throws URISyntaxException, StorageException {

		if (storageAccount == null) {
			return null;
		}

		List<String> containerList = null;

		CloudStorageAccount cloudStorageAccount;
		CloudBlobClient serviceClient;
		StorageCredentialsAccountAndKey credentials;
		String blobURL = storageAccount.getBlobEndPointURL();

		credentials = new StorageCredentialsAccountAndKey(
				storageAccount.getStorageAccName(),
				storageAccount.getStorageAccountKey());

		if (Utils.isNullOrEmpty(blobURL) || blobURL.equals(Utils.DEF_BLOB_URL)) {
			cloudStorageAccount = new CloudStorageAccount(credentials);
		} else {
			cloudStorageAccount = new CloudStorageAccount(credentials, new URI(
					blobURL), null, null);
		}

		serviceClient = cloudStorageAccount.createCloudBlobClient();
		if (!allowRetry && serviceClient != null) {
			// Setting no retry policy
			RetryNoRetry rnr = new RetryNoRetry();
			serviceClient.setRetryPolicyFactory(rnr);
		}

		if (serviceClient != null) {
			for (CloudBlobContainer blobContainer : serviceClient
					.listContainers()) {
				if (containerList == null) {
					containerList = new ArrayList<String>();
				}
				containerList.add(blobContainer.getName());
			}
		}
		return containerList;
	}

	public static List<String> getContainerBlobList(
			StorageAccountInfo storageAccountInfo, String containerName)
			throws URISyntaxException, StorageException {

		if (storageAccountInfo == null
				|| (containerName == null || containerName.trim().length() == 0)) {
			return null;
		}

		List<String> blobList = new ArrayList<String>();

		CloudBlobContainer cloudBlobContainer = getBlobContainerReference(
				storageAccountInfo.getStorageAccName(),
				storageAccountInfo.getStorageAccountKey(),
				storageAccountInfo.getBlobEndPointURL(), containerName, false,
				false, null);

		Iterable<ListBlobItem> blobItems = null;
		if (cloudBlobContainer != null && cloudBlobContainer.exists()) {
			blobItems = cloudBlobContainer.listBlobs();
		}

		if (blobItems != null) {
			for (ListBlobItem blobItem : blobItems) {
				// If the item is a blob, not a virtual directory
				if (blobItem instanceof CloudBlob) {
					// Download the item and save it to a file with the same
					// name
					CloudBlob blob = (CloudBlob) blobItem;

					// Filter blobs with name "$$$.$$$"
					if (blob.getName().endsWith(EMPTY_FILE_NAME)) {
						continue;
					}

					blobList.add(blob.getName());
				} else if (blobItem instanceof CloudBlobDirectory) {
					CloudBlobDirectory blobDir = (CloudBlobDirectory) blobItem;
					blobList.add(blobDir.getPrefix());
					// list blobs again
					getBlobDirectoryList(blobDir, blobList);
				}
			}
		}
		return blobList;
	}

	public static void getBlobDirectoryList(CloudBlobDirectory blobDirectory,
			List<String> blobList) throws URISyntaxException, StorageException {

		Iterable<ListBlobItem> blobItems = blobDirectory.listBlobs();
		if (blobItems != null) {
			for (ListBlobItem blobItem : blobItems) {
				// If the item is a blob, not a virtual directory
				if (blobItem instanceof CloudBlob) {
					// Download the item and save it to a file with the same
					// name
					CloudBlob blob = (CloudBlob) blobItem;

					// Filter blobs with name "$$$.$$$"
					if (blob.getName().endsWith(EMPTY_FILE_NAME)) {
						continue;
					}
					blobList.add(blob.getName());
				} else if (blobItem instanceof CloudBlobDirectory) {
					CloudBlobDirectory blobDir = (CloudBlobDirectory) blobItem;
					blobList.add(blobDir.getPrefix());
					// list blobs again
					getBlobDirectoryList(blobDir, blobList);
				}
			}
		}
	}

	/**
	 * Uploads files to Windows Azure Storage.
	 * 
	 * @param listener
	 * @param build
	 * @param StorageAccountInfo
	 *            storage account information.
	 * @param expContainerName
	 *            container name.
	 * @param cntPubAccess
	 *            denotes if container is publicly accessible.
	 * @param expFP
	 *            File Path in ant glob syntax relative to CI tool workspace.
	 * @param expVP
	 *            Virtual Path of blob container.
	 * @return filesUploaded number of files that are uploaded.
	 * @throws WAStorageException
	 * @throws Exception
	 */
	public static int upload(AbstractBuild<?, ?> build, BuildListener listener,
			StorageAccountInfo strAcc, String expContainerName,
			boolean cntPubAccess, boolean cleanUpContainer, String expFP,
			String expVP) throws WAStorageException {

		CloudBlockBlob blob = null;
		int filesUploaded = 0; // Counter to track no. of files that are
								// uploaded

		try {
			FilePath wsPath = build.getWorkspace();
			StringTokenizer strTokens = new StringTokenizer(expFP, fpSeparator);
			FilePath[] paths = null;

			listener.getLogger().println(
					Messages.WAStoragePublisher_uploading());

			CloudBlobContainer container = WAStorageClient
					.getBlobContainerReference(strAcc.getStorageAccName(),
							strAcc.getStorageAccountKey(),
							strAcc.getBlobEndPointURL(), expContainerName,
							true, true, cntPubAccess);

			// Delete previous contents if cleanup is needed
			if (cleanUpContainer) {
				deleteContents(container);
			}

			while (strTokens.hasMoreElements()) {
				String fileName = strTokens.nextToken();
			
				File fileToUpload = new File(fileName);
				FilePath fp = new FilePath(fileToUpload);
				
				if (fp.exists() && !fp.isDirectory()) {
					paths = new FilePath[1];
					paths[0] = fp;
				} else {
					paths = wsPath.list(fileName);
				}

				if (paths.length != 0) {
					for (FilePath src : paths) {
						if (Utils.isNullOrEmpty(expVP)) {
							blob = container.getBlockBlobReference(src
									.getName());
						} else {
							blob = container.getBlockBlobReference(expVP
									+ src.getName());
						}

						long startTime = System.currentTimeMillis();
						InputStream inputStream = src.read();
						try {
							blob.upload(inputStream, src.length(), null,
									getBlobRequestOptions(), null);
						} finally {
							try {
								inputStream.close();
							} catch (IOException e) {

							}
						}
						long endTime = System.currentTimeMillis();
						listener.getLogger().println(
								"Uploaded blob with uri " + blob.getUri()
										+ " in "
										+ getTime(endTime - startTime));
						filesUploaded++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WAStorageException(e.getMessage(), e.getCause());
		}
		return filesUploaded;
	}

	private static void deleteContents(CloudBlobContainer container)
			throws StorageException, URISyntaxException {

		for (ListBlobItem blobItem : container.listBlobs()) {
			if (blobItem instanceof CloudBlob) {
				((CloudBlob) blobItem).delete();
			}

			if (blobItem instanceof CloudBlobDirectory) {
				deleteContents((CloudBlobDirectory) blobItem);
			}
		}
	}

	private static void deleteContents(CloudBlobDirectory cloudBlobDirectory)
			throws StorageException, URISyntaxException {

		for (ListBlobItem blobItem : cloudBlobDirectory.listBlobs()) {
			if (blobItem instanceof CloudBlob) {
				((CloudBlob) blobItem).delete();
			}

			if (blobItem instanceof CloudBlobDirectory) {
				deleteContents((CloudBlobDirectory) blobItem);
			}
		}
	}

	public static int download(AbstractBuild<?, ?> build,
			BuildListener listener, StorageAccountInfo strAcc,
			String expContainerName, String blobName, String downloadDirLoc)
			throws WAStorageException {

		int filesDownloaded = 0;
		String downloadDir = downloadDirLoc;

		try {
			if (downloadDir == null || downloadDir.trim().length() == 0) {
				downloadDir = Jenkins.getInstance()
						.getBuildDirFor(build.getProject()).getPath();
			}

			// create directory if doesnot exist
			File downloadDirFile = new File(downloadDir);
			if (!downloadDirFile.exists()) {
				downloadDirFile.mkdirs();
			}

			listener.getLogger().println(
					Messages.AzureStorageBuilder_downloading());

			CloudBlobContainer container = WAStorageClient
					.getBlobContainerReference(strAcc.getStorageAccName(),
							strAcc.getStorageAccountKey(),
							strAcc.getBlobEndPointURL(), expContainerName,
							false, true, null);

			filesDownloaded = downloadBlobs(container, blobName, downloadDir,
					listener);

		} catch (Exception e) {
			e.printStackTrace();
			throw new WAStorageException(e.getMessage(), e.getCause());
		}
		return filesDownloaded;

	}

	private static int downloadBlobs(CloudBlobContainer container,
			String blobName, String downloadDir, BuildListener listener)
			throws URISyntaxException, StorageException, IOException {

		int filesDownloaded = 0;

		boolean exactBlobName = true;
		if (blobName.endsWith("*")) {
			exactBlobName = false;
			blobName = blobName.substring(0, blobName.length() - 1);
		}

		if (exactBlobName) {
			CloudBlob blobReference = container.getBlockBlobReference(blobName);

			// Check if it is page blob
			if (!blobReference.exists()) {
				blobReference = container.getPageBlobReference(blobName);
			}

			if (blobReference.exists()) {
				downloadBlob(blobReference, downloadDir, listener);
				filesDownloaded++;
			}
		} else {
			for (ListBlobItem blobItem : container.listBlobs(blobName)) {
				// If the item is a blob, not a virtual directory
				if (blobItem instanceof CloudBlob) {
					// Download the item and save it to a file with the same
					// name
					CloudBlob blob = (CloudBlob) blobItem;

					// Filter blobs with name "$$$.$$$"
					if (blob.getName().endsWith(EMPTY_FILE_NAME)) {
						continue;
					}

					downloadBlob(blob, downloadDir, listener);
					filesDownloaded++;

				} else if (blobItem instanceof CloudBlobDirectory) {
					CloudBlobDirectory blobDirectory = (CloudBlobDirectory) blobItem;
					filesDownloaded += downloadBlob(blobDirectory, downloadDir,
							listener);
				}
			}
		}

		return filesDownloaded;
	}

	private static int downloadBlob(CloudBlobDirectory blobDirectory,
			String downloadDir, BuildListener listener)
			throws StorageException, URISyntaxException, IOException {

		int filesDownloaded = 0;

		for (ListBlobItem blobItem : blobDirectory.listBlobs()) {
			// If the item is a blob, not a virtual directory
			if (blobItem instanceof CloudBlob) {
				// Download the item and save it to a file with the same
				// name
				CloudBlob blob = (CloudBlob) blobItem;

				// Filter blobs with name "$$$.$$$"
				if (blob.getName().endsWith(EMPTY_FILE_NAME)) {
					continue;
				}

				downloadBlob(blob, downloadDir, listener);
				filesDownloaded++;

			} else if (blobItem instanceof CloudBlobDirectory) {
				CloudBlobDirectory blobDir = (CloudBlobDirectory) blobItem;
				filesDownloaded += downloadBlob(blobDir, downloadDir, listener);
			}
		}

		return filesDownloaded;
	}

	private static void downloadBlob(CloudBlob blob, String downloadDir,
			BuildListener listener) throws URISyntaxException,
			StorageException, IOException {
		FileOutputStream fos = null;
		try {
			File downloadFile = new File(downloadDir + File.separator
					+ blob.getName());

			if (!downloadFile.getParentFile().exists()) {
				downloadFile.getParentFile().mkdirs();
			}

			fos = new FileOutputStream(downloadDir + File.separator
					+ blob.getName());

			long startTime = System.currentTimeMillis();

			blob.download(fos, null, getBlobRequestOptions(), null);

			long endTime = System.currentTimeMillis();

			listener.getLogger().println(
					"blob " + blob.getName() + " is downloaded to "
							+ downloadDir + " in "
							+ getTime(endTime - startTime));
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {

			}

		}

	}

	private static BlobRequestOptions getBlobRequestOptions() {
		BlobRequestOptions options = new BlobRequestOptions();

		int concurrentRequestCount = 1;

		try {
			concurrentRequestCount = Runtime.getRuntime().availableProcessors();
		} catch (Exception e) {
			e.printStackTrace();
		}

		options.setConcurrentRequestCount(concurrentRequestCount);

		return options;
	}

	public static String getTime(long timeInMills) {
		return DurationFormatUtils.formatDuration(timeInMills, "HH:mm:ss.S");
	}
}
