/**----------------------------------------------------------------------------------
* Microsoft Developer & Platform Evangelism
*
* Copyright (c) Microsoft Corporation. All rights reserved.
*
* THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, 
* EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES 
* OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.
*----------------------------------------------------------------------------------
* The example companies, organizations, products, domain names,	
* e-mail addresses, logos, people, places, and events depicted
* herein are fictitious.  No association with any real company,
* organization, product, domain name, email address, logo, person,
* places, or events is intended or should be inferred.
*----------------------------------------------------------------------------------
**/

package com.microsoft.photouploader;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.UUID;

public class ImageManager {
    /*
    **Shared Access Signatures info.
    https://docs.microsoft.com/en-us/rest/api/storageservices/delegating-access-with-a-shared-access-signature 
    and https://docs.microsoft.com/en-us/azure/storage/common/storage-dotnet-shared-access-signature-part-1 
    */

    public static final String storageConnectionString = BuildConfig.CONNECTION_STRING;;


    /**
     * Generate SAS that will give use access for 1 hour
     * @return SAS string
     * @throws Exception
     */
    private static String getContainerSAS() throws Exception {
        final int EXPIRATION_HOURS = 1;

        // Retrieve storage account from connection-string.
        CloudStorageAccount storageAccount = CloudStorageAccount
                .parse(storageConnectionString);

        // Create the blob client.
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

        // Create Share access policy
        SharedAccessBlobPolicy sasPolicy = new SharedAccessBlobPolicy();

        // Create a UTC Gregorian calendar value.
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // Use the start time delta one hour as the end time for the shared
        // access signature.
        calendar.add(Calendar.HOUR, EXPIRATION_HOURS);
        sasPolicy.setSharedAccessExpiryTime(calendar.getTime());

        // set permissions
        sasPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ, SharedAccessBlobPermissions.WRITE,
                SharedAccessBlobPermissions.LIST));

        // Get a reference to a container.
        // The container name must be lower case
        CloudBlobContainer container = blobClient.getContainerReference("foodimages"); //TODO: Set container name as participant number
        container.createIfNotExists();

        // apply permissions to container
        String sas = container.generateSharedAccessSignature(sasPolicy, null);

        // URL for SAS
        String sasurl = container.getUri()+"?"+sas;
        return sasurl;
    }

    /**
     * Uplaod file to the blob
     *
     * @param image
     * @param imageLength
     * @return
     * @throws Exception
     */
    public static String UploadImage(InputStream image, int imageLength) throws Exception {
        String imageName = uniqueFilename();

        // get container object with URI from SAS
        String ContainerSas = getContainerSAS();
        CloudBlobContainer container = new CloudBlobContainer(new URI(ContainerSas));
        CloudBlockBlob imageBlob = container.getBlockBlobReference(imageName);

        //Upload image
        imageBlob.upload(image, imageLength);

        return imageName;

    }

    /**
     *  List all of images in the blob container
     *
     * @return String[] - List of blob names
     * @throws Exception
     */
    public static String[] ListImages() throws Exception{
        String ContainerSas = getContainerSAS();
        CloudBlobContainer container = new CloudBlobContainer(new URI(ContainerSas));


        Iterable<ListBlobItem> blobs = container.listBlobs();

        LinkedList<String> blobNames = new LinkedList<>();
        for(ListBlobItem blob: blobs) {
            blobNames.add(((CloudBlockBlob) blob).getName());
        }

        return blobNames.toArray(new String[blobNames.size()]);
    }

    /**
     *  Get The data from the blob
     *
     * @param name
     * @param imageStream
     * @param imageLength
     * @throws Exception
     */
    public static void GetImage(String name, OutputStream imageStream, long imageLength) throws Exception {
        String ContainerSas = getContainerSAS();
        CloudBlobContainer container = new CloudBlobContainer(new URI(ContainerSas));

        CloudBlockBlob blob = container.getBlockBlobReference(name);

        if(blob.exists()){
            blob.downloadAttributes();

            imageLength = blob.getProperties().getLength();

            blob.download(imageStream);
        }
    }


    /**
     * Generate unique filename
     * @return String
     */
    static String uniqueFilename(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = dtf.format(now);
        UUID uniqueId = UUID.randomUUID();

        return  timestamp + "_" + uniqueId + ".jpg";
    }
}
