/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.utils;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.*;

import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Created by vlashch on 1/19/17.
 */
public class StorageAccoutUtils {
    private static CloudStorageAccount getCloudStorageAccount(String blobLink, String saKey) throws Exception {
        if (blobLink == null || blobLink.isEmpty()) {
            throw new IllegalArgumentException("Invalid blob link, it's null or empty: " + blobLink);
        }
        if (saKey == null || saKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid storage account key, it's null or empty: " + saKey);
        }
        // check the link is valic
        URI blobUri = new URL(blobLink).toURI();
        String host =  blobUri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid blobLink, can't find host: " + blobLink);
        }
        String storageAccountName = host.substring(0, host.indexOf("."));
        String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageAccountName, saKey);
        CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);
        return cloudStorageAccount;
    }

    public static String  getBlobSasUri(String blobLink, String saKey) throws Exception {
        CloudStorageAccount cloudStorageAccount = getCloudStorageAccount(blobLink, saKey);
        // Create the blob client.
        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        // Get container and blob name from the link
        String path = new URI(blobLink).getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid blobLink: " + blobLink);
        }
        int containerNameEndIndex = path.indexOf("/", 1);
        String containerName = path.substring(1, containerNameEndIndex);
        if (containerName == null || containerName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find container name: " + blobLink);
        }
        String blobName = path.substring(path.indexOf("/", containerNameEndIndex)+1);
        if (blobName == null || blobName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find blob name: " + blobLink);
        }
        // Retrieve reference to a previously created container.
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        //CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        SharedAccessBlobPolicy sharedAccessBlobPolicy = new SharedAccessBlobPolicy();
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(new Date());
        sharedAccessBlobPolicy.setSharedAccessStartTime(calendar.getTime());
        calendar.add(Calendar.HOUR, 23);
        sharedAccessBlobPolicy.setSharedAccessExpiryTime(calendar.getTime());
        sharedAccessBlobPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        container.uploadPermissions(containerPermissions);
        String signature = container.generateSharedAccessSignature(sharedAccessBlobPolicy, null);
        return blobLink + "?" + signature;
    }

}
