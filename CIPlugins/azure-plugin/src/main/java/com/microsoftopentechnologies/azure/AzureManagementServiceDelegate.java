package com.microsoftopentechnologies.azure;

import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.xml.sax.SAXException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.OperationStatusResponse;
import com.microsoft.windowsazure.core.utils.Base64;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.LocationOperations;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.ManagementService;
import com.microsoft.windowsazure.management.RoleSizeOperations;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementService;
import com.microsoft.windowsazure.management.compute.models.CertificateFormat;
import com.microsoft.windowsazure.management.compute.models.ConfigurationSet;
import com.microsoft.windowsazure.management.compute.models.ConfigurationSetTypes;
import com.microsoft.windowsazure.management.compute.models.DeploymentGetResponse;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;
import com.microsoft.windowsazure.management.compute.models.HostedServiceCheckNameAvailabilityResponse;
import com.microsoft.windowsazure.management.compute.models.HostedServiceCreateParameters;
import com.microsoft.windowsazure.management.compute.models.HostedServiceGetResponse;
import com.microsoft.windowsazure.management.compute.models.HostedServiceListResponse;
import com.microsoft.windowsazure.management.compute.models.InputEndpoint;
import com.microsoft.windowsazure.management.compute.models.InstanceEndpoint;
import com.microsoft.windowsazure.management.compute.models.OSVirtualHardDisk;
import com.microsoft.windowsazure.management.compute.models.PostShutdownAction;
import com.microsoft.windowsazure.management.compute.models.ResourceExtensionParameterValue;
import com.microsoft.windowsazure.management.compute.models.ResourceExtensionReference;
import com.microsoft.windowsazure.management.compute.models.Role;
import com.microsoft.windowsazure.management.compute.models.RoleInstance;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateCreateParameters;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateGetParameters;
import com.microsoft.windowsazure.management.compute.models.ServiceCertificateGetResponse;
import com.microsoft.windowsazure.management.compute.models.SshSettingKeyPair;
import com.microsoft.windowsazure.management.compute.models.SshSettingPublicKey;
import com.microsoft.windowsazure.management.compute.models.SshSettings;
import com.microsoft.windowsazure.management.compute.models.VirtualIPAddress;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineCreateDeploymentParameters;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineCreateParameters;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineGetResponse;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineRoleType;
import com.microsoft.windowsazure.management.compute.models.VirtualMachineShutdownParameters;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.configuration.PublishSettingsLoader;
import com.microsoft.windowsazure.management.models.LocationsListResponse;
import com.microsoft.windowsazure.management.models.LocationsListResponse.Location;
import com.microsoft.windowsazure.management.models.RoleSizeListResponse;
import com.microsoft.windowsazure.management.models.RoleSizeListResponse.RoleSize;
import com.microsoft.windowsazure.management.storage.StorageManagementClient;
import com.microsoft.windowsazure.management.storage.StorageManagementService;
import com.microsoft.windowsazure.management.storage.models.StorageAccountGetKeysResponse;

public class AzureManagementServiceDelegate {
	private static final String CI_SYSTEM = "jenkinsslaves";
	private static final int DEFAULT_SSH_PORT = 22;
	private static final Logger LOGGER = Logger
			.getLogger(AzureManagementServiceDelegate.class.getName());
	private static final String BLOB = "blob";
	private static final String TABLE = "table";
	private static final String QUEUE = "queue";
	private static final String CONFIG_CONTAINER_NAME = "jenkinsconfig";


	public static AzureSlave createVirtualMachine(AzureSlaveTemplate template,
			TaskListener listener) throws Exception {

		// TODO: Verify if storage and hosted service in same location

		AzureSlave slave = null;
		try {
			// TODO: Add validations

			LOGGER.info("Initializing create virtual machine request for slaveTemaple"
					+ template.getTemplateName());

			AzureCloud azureCloud = template.getAzureCloud();
			String subscriptionID = azureCloud.getSubscriptionId();

			// Load configuration
			Configuration config = loadConfiguration(subscriptionID,
					azureCloud.getServiceManagementCert(),
					azureCloud.getPassPhrase(),
					azureCloud.getServiceManagementURL());
			ComputeManagementClient client = getComputeManagementClient(config);

			// Create hosted service if not exists
			String cloudServiceName = template.getCloudServiceName() == null ? template
					.getCloudServiceName() : template.getTemplateName();
			String deploymentName = null;
			OperationStatusResponse response = null;
			if (createCloudServiceIfNotExists(config, cloudServiceName,
					template.getLocation())) {
				deploymentName = cloudServiceName + "prod";
				LOGGER.info("Creating deployment " + deploymentName
						+ " for cloud service " + cloudServiceName);

				VirtualMachineCreateDeploymentParameters params = createVirtualMachineDeploymentParams(config, 
						cloudServiceName, deploymentName, template);

				response = client.getVirtualMachinesOperations()
						.createDeployment(cloudServiceName, params);

				slave = parseDeploymentResponse(response, cloudServiceName,
						template, params);

			} else {
				deploymentName = getExistingDeploymentName(config,
						cloudServiceName);
				
				if (deploymentName != null ) {
					VirtualMachineCreateParameters params = createVirtualMachineParams(config, 
						cloudServiceName, deploymentName, template);
					response = client.getVirtualMachinesOperations().create(
						cloudServiceName, deploymentName, params);

					slave = parseResponse(response, cloudServiceName,
						deploymentName, template, params);
				} else {
					deploymentName = cloudServiceName + "prod";
					LOGGER.info("Creating deployment " + deploymentName
							+ " for cloud service " + cloudServiceName);

					VirtualMachineCreateDeploymentParameters params = createVirtualMachineDeploymentParams(config, 
							cloudServiceName, deploymentName, template);

					response = client.getVirtualMachinesOperations()
							.createDeployment(cloudServiceName, params);

					slave = parseDeploymentResponse(response, cloudServiceName,
							template, params);
				}
			}

			LOGGER.info("Sucessfully created virtual machine in in cloud service "
					+ cloudServiceName);
			LOGGER.info("Slave configuiration " + slave);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Error: Unable to create virtual machine due to "
					+ e.getMessage());
			throw new AzureCloudException(
					"Error: Unable to create virtual machine due to "
							+ e.getMessage());
		}
		return slave;
	}

	private static VirtualMachineCreateParameters createVirtualMachineParams(
			Configuration config, String cloudServiceName,
			String deploymentName, AzureSlaveTemplate template)
			throws Exception {
		
		ComputeManagementClient client = getComputeManagementClient(config);

		String sshPublicKeyPath = null;
		VirtualMachineCreateParameters params = new VirtualMachineCreateParameters();
		String virtualMachineName = cloudServiceName + "VM"
				+ getRandonInt(0, 9);
		// TODO: Get storage account blob uri by making rest call
		URI mediaLinkUriValue = new URI("http://"
				+ template.getStorageAccountName() + ".blob.core.windows.net/"
				+ CI_SYSTEM + "/" + cloudServiceName + "-" + virtualMachineName
				+ ".vhd");

		ArrayList<ConfigurationSet> configurationSets = new ArrayList<ConfigurationSet>();
		params.setConfigurationSets(configurationSets);
		params.setRoleName(virtualMachineName);
		params.setRoleSize(template.getVirtualMachineSize());
		params.setProvisionGuestAgent(true);

		OSVirtualHardDisk oSVirtualHardDisk = new OSVirtualHardDisk();
		params.setOSVirtualHardDisk(oSVirtualHardDisk);
		oSVirtualHardDisk.setMediaLink(mediaLinkUriValue);
		oSVirtualHardDisk.setSourceImageName(template.getImageId());

		ConfigurationSet osSpecificConf = new ConfigurationSet();
		configurationSets.add(osSpecificConf);

		String OSType = getVirtualMachineOSImage(client, template.getImageId());

		// TODO: remove this check later
		System.out.println(template.getAdminPassword()
				+ "template.getAdminPassword()");
		if ("Windows".equalsIgnoreCase(OSType)) {
			osSpecificConf
					.setConfigurationSetType(ConfigurationSetTypes.WINDOWSPROVISIONINGCONFIGURATION);
			osSpecificConf.setComputerName(virtualMachineName);
			osSpecificConf.setAdminUserName(template.getAdminUserName());
			osSpecificConf.setAdminPassword(template.getAdminPassword());
			osSpecificConf.setEnableAutomaticUpdates(false);
		
			params.setResourceExtensionReferences(handleCustomScriptExtension(config, virtualMachineName, cloudServiceName, template));;
		} else if ("Linux".equalsIgnoreCase(OSType)) {
			sshPublicKeyPath = "/home/user/.ssh/authorized_keys";

			osSpecificConf
					.setConfigurationSetType(ConfigurationSetTypes.LINUXPROVISIONINGCONFIGURATION);
			osSpecificConf.setHostName(virtualMachineName);
			osSpecificConf.setUserName(template.getAdminUserName());

			if (template.getAdminPassword() == null) {
				osSpecificConf.setDisableSshPasswordAuthentication(true);
			}

			if (template.getAdminPassword() == null
					|| template.getAdminPassword().trim().length() == 0) {
				osSpecificConf.setUserPassword("Abcd.1234");
			}
			osSpecificConf.setUserPassword(template.getAdminPassword());
			osSpecificConf.setAdminPassword("Abcd.1234");
			osSpecificConf.setDisableSshPasswordAuthentication(false);

			// Configure SSH
			// SshSettings sshSettings = new SshSettings();
			// osSpecificConf.setSshSettings(sshSettings);
			//
			// //Get certificate thumprint
			// Map<String, String> certMap =
			// getCertInfo(template.getSshPublicKey().getBytes("UTF-8"));
			//
			// ArrayList<SshSettingPublicKey> publicKeys= new
			// ArrayList<SshSettingPublicKey>();
			// sshSettings.setPublicKeys(publicKeys);
			// // Add public key
			// SshSettingPublicKey publicKey = new SshSettingPublicKey();
			// publicKeys.add(publicKey);
			// publicKey.setFingerprint(certMap.get("thumbPrint"));
			// publicKey.setPath(sshPublicKeyPath);
			//
			// ArrayList<SshSettingKeyPair> keyPairs= new
			// ArrayList<SshSettingKeyPair>();
			// sshSettings.setKeyPairs(keyPairs);
			// // Add key pair
			// SshSettingKeyPair keyPair = new SshSettingKeyPair();
			// keyPairs.add(keyPair);
			// keyPair.setFingerprint(sshPKFingerPrint);
			// keyPair.setPath(sshKeyPairPath);

		} else {
			throw new AzureCloudException("Unsupported OSType " + OSType);
		}

		// Network configuration set
		ConfigurationSet networkConfigset = new ConfigurationSet();
		configurationSets.add(networkConfigset);
		networkConfigset
				.setConfigurationSetType(ConfigurationSetTypes.NETWORKCONFIGURATION);
		// Define endpoints
		ArrayList<InputEndpoint> enpoints = new ArrayList<InputEndpoint>();
		networkConfigset.setInputEndpoints(enpoints);

		// Add RDP endpoint
		InputEndpoint rdpPort = new InputEndpoint();
		enpoints.add(rdpPort);

		if ("Windows".equalsIgnoreCase(OSType)) {
			rdpPort.setName("ssh");
			rdpPort.setProtocol("tcp");
			rdpPort.setLocalPort(DEFAULT_SSH_PORT);
			// rdpPort.setPort(endPointPort);
		} else if ("Linux".equalsIgnoreCase(OSType)) {
			rdpPort.setName("ssh");
			rdpPort.setProtocol("tcp");
			rdpPort.setLocalPort(DEFAULT_SSH_PORT);
			// purposefully not setting public port so that azure assigns a
			// random.
		}
		return params;
	}

	private static String getExistingDeploymentName(Configuration config,
			String cloudServiceName) throws Exception {
		ComputeManagementClient client = getComputeManagementClient(config);

		DeploymentGetResponse response = client.getDeploymentsOperations()
				.getBySlot(cloudServiceName, DeploymentSlot.Production);
		if (response.getRoleInstances().size() > 0) {
			return response.getName();
		} else {
			return null;
		}
	}

	public static String generateSASURL(String storageAccountName,
			String storageAccountKey, String containerName, String blobURL)
			throws Exception {
		LOGGER.info("Generating SAS URL for blob " + blobURL);

		String storageConnectionString = "DefaultEndpointsProtocol=http;"
				+ "AccountName=" + storageAccountName + ";" + "AccountKey="
				+ storageAccountKey;

		CloudStorageAccount storageAccount;

		// Use the connection string to create the storage account.
		// TODO: Need to externalize blob URL
		storageAccount = CloudStorageAccount.parse(storageConnectionString);

		// Create the blob client.
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

		CloudBlobContainer container = blobClient
				.getContainerReference(containerName);

		// At this point need to throw an error back since container itself did
		// not exist.
		if (!container.exists()) {
			throw new AzureCloudException("Container " + containerName
					+ " doesnot exist in storage account " + storageAccountName);
		}

		// TODO: Ask test team to test in different time zones
		SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
		GregorianCalendar calendar = new GregorianCalendar(
				TimeZone.getTimeZone("UTC"));
		calendar.setTime(new Date());
		policy.setSharedAccessStartTime(calendar.getTime());
		calendar.add(Calendar.HOUR, 1);
		policy.setSharedAccessExpiryTime(calendar.getTime());
		policy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ,
				SharedAccessBlobPermissions.WRITE));

		// TODO: Test if old sas is valid after permissions are updated
		BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
		// containerPermissions.setPublicAccess(BlobContainerPublicAccessType.OFF);
		containerPermissions.getSharedAccessPolicies().put(
				"jenkinssharedAccess", policy);
		container.uploadPermissions(containerPermissions);

		// Create a shared access signature for the container.
		String sas = container.generateSharedAccessSignature(policy, null);

		LOGGER.fine("SAS Url" + sas);
		LOGGER.info("Sucessfully generated SAS url " + blobURL);
		return blobURL + "?" + sas;
	}

	public static String getCustomScriptPublicConfigValue(String sasURL,
			String fileName, String jenkinsServerURL, String vmName)
			throws Exception {
		JsonFactory jFactory = new JsonFactory();
		StringWriter writer = new StringWriter();

		JsonGenerator json = jFactory.createJsonGenerator(writer);
		json.writeStartObject();

		json.writeArrayFieldStart("fileUris");
		json.writeString(sasURL);
		json.writeEndArray();

		json.writeStringField("commandToExecute",
				"powershell -ExecutionPolicy Unrestricted -file " + fileName
						+ " " + jenkinsServerURL + " " + vmName);

		json.writeEndObject();
		json.close();
		String jsonString = writer.toString();

		return jsonString;

	}

	public static ArrayList<ResourceExtensionReference> handleCustomScriptExtension(Configuration config, String roleName,
			String cloudServiceName, AzureSlaveTemplate template) throws Exception {
		
		StorageManagementClient client = getStorageManagementClient(config);
		String storageAccountKey = client.getStorageAccountsOperations().getKeys(template.getStorageAccountName()).getPrimaryKey();
		// upload init script
		LOGGER.info("uploading init script to storage account "+template.getInitScript());
		String fileName = roleName+"initscript.ps1";
		String blobURL = uploadConfigFileToStorage(template.getStorageAccountName(), storageAccountKey, client.getBaseUri().toString() , fileName, template.getInitScript());
		
		String jenkinsServerURL = Hudson.getInstance().getRootUrl();
		LOGGER.info("Jenkins server url "+jenkinsServerURL);
		// set custom script extension in role
		return addResourceExtenions(roleName, template.getStorageAccountName(), storageAccountKey, CONFIG_CONTAINER_NAME, blobURL, fileName, jenkinsServerURL);

	}
	

	public static String uploadConfigFileToStorage(String storageAccountName, String key, String baseURI, String fileName, String initScript)
			throws Exception {

		CloudBlockBlob blob = null;
		String storageURI = getStorageAccountEndPoint(storageAccountName, baseURI);

		CloudBlobContainer container = getBlobContainerReference(
				storageAccountName, key, storageURI, CONFIG_CONTAINER_NAME);

		
		blob = container.getBlockBlobReference(fileName);

		InputStream is = new ByteArrayInputStream(initScript.getBytes("UTF-8"));
		try {
			blob.upload(is, initScript.length());
		} finally {
			is.close();
		}
		
		return storageURI + CONFIG_CONTAINER_NAME + "/" +fileName;
		
	}
	
	

	private static CloudBlobContainer getBlobContainerReference(
			String storageAccountName, String key, String blobURL,
			String containerName) throws Exception {

		CloudStorageAccount cloudStorageAccount;
		CloudBlobClient serviceClient;
		CloudBlobContainer container;
		StorageCredentialsAccountAndKey credentials;

		credentials = new StorageCredentialsAccountAndKey(storageAccountName,
				key);

		cloudStorageAccount = new CloudStorageAccount(credentials, new URI(
				blobURL), new URI(getCustomURI(storageAccountName, QUEUE,
				blobURL)), new URI(getCustomURI(storageAccountName, TABLE,
				blobURL)));

		serviceClient = cloudStorageAccount.createCloudBlobClient();
		container = serviceClient.getContainerReference(containerName);

		container.createIfNotExists();

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

	public static String getStorageAccountEndPoint(String storageAccountName,
			String baseURI) {

		int index = baseURI.indexOf(".");
		String storageURI = "https://" + storageAccountName + ".blob."
				+ baseURI.substring(index + 1);

		if (!storageURI.endsWith("/"))
			storageURI += "/";

		return storageURI;

	}

	public static ArrayList<ResourceExtensionReference> addResourceExtenions(String roleName, String storageAccountName, String storageAccountKey,
			String containerName, String blobURL, String fileName,
			String jenkinsServerURL) throws Exception {

		ArrayList<ResourceExtensionReference> resourceExtensions = new ArrayList<ResourceExtensionReference>();
//		role.setResourceExtensionReferences(resourceExtensions);

		// BG info
		// TODO : Remove BG info extension and test if custom script extension
		// is working
		ResourceExtensionReference bgInfo = new ResourceExtensionReference();
		resourceExtensions.add(bgInfo);
		bgInfo.setReferenceName("BGInfo");
		bgInfo.setPublisher("Microsoft.Compute");
		bgInfo.setName("BGInfo");
		bgInfo.setVersion("1.*");
		bgInfo.setState("Enable");

		// custom script
		ResourceExtensionReference customExtension = new ResourceExtensionReference();
		resourceExtensions.add(customExtension);
		customExtension.setReferenceName("CustomScriptExtension");
		customExtension.setPublisher("Microsoft.Compute");
		customExtension.setName("CustomScriptExtension");
		customExtension.setVersion("1.*");
		customExtension.setState("Enable");

		ArrayList<ResourceExtensionParameterValue> resourceParams = new ArrayList<ResourceExtensionParameterValue>();
		customExtension.setResourceExtensionParameterValues(resourceParams);

		ResourceExtensionParameterValue pubicConfig = new ResourceExtensionParameterValue();
		resourceParams.add(pubicConfig);
		pubicConfig.setKey("CustomScriptExtensionPublicConfigParameter");

		// Get SAS URL
		String sasURL = generateSASURL(storageAccountName, storageAccountKey,
				containerName, blobURL);

		pubicConfig.setValue(getCustomScriptPublicConfigValue(sasURL, fileName,
				jenkinsServerURL, roleName));
		pubicConfig.setType("Public");

		ResourceExtensionParameterValue privateConfig = new ResourceExtensionParameterValue();
		resourceParams.add(privateConfig);
		privateConfig.setKey("CustomScriptExtensionPrivateConfigParameter");
		privateConfig.setValue(getCustomScriptPrivateConfigValue(
				storageAccountName, storageAccountKey));
		privateConfig.setType("Private");
		
		return resourceExtensions;
	}

	public static String getCustomScriptPrivateConfigValue(
			String storageAccountName, String storageAccountKey)
			throws Exception {
		JsonFactory jFactory = new JsonFactory();
		StringWriter writer = new StringWriter();

		JsonGenerator json = jFactory.createJsonGenerator(writer);
		json.writeStartObject();

		json.writeStringField("storageAccountName", storageAccountName);
		json.writeStringField("storageAccountKey", storageAccountKey);

		json.writeEndObject();
		json.close();
		String jsonString = writer.toString();

		return jsonString;
	}

	public static void setVirtualMachineDetails(AzureSlave azureSlave,
			AzureSlaveTemplate template) throws Exception {
		AzureCloud azureCloud = template.getAzureCloud();
		Configuration config = loadConfiguration(
				azureCloud.getSubscriptionId(),
				azureCloud.getServiceManagementCert(),
				azureCloud.getPassPhrase(),
				azureCloud.getServiceManagementURL());
		ComputeManagementClient client = getComputeManagementClient(config);

		DeploymentGetResponse response = client.getDeploymentsOperations()
				.getByName(azureSlave.getCloudServiceName(),
						azureSlave.getDeploymentName());
		// Getting the first virtual IP
		azureSlave.setPublicDNSName(response.getVirtualIPAddresses().get(0)
				.getAddress().getHostAddress());

		ArrayList<RoleInstance> instances = response.getRoleInstances();
		for (RoleInstance roleInstance : instances) {
			if (roleInstance.getRoleName().equals(azureSlave.getNodeName())) {
				ArrayList<InstanceEndpoint> endPoints = roleInstance
						.getInstanceEndpoints();

				for (InstanceEndpoint endPoint : endPoints) {
					if (endPoint.getLocalPort() == DEFAULT_SSH_PORT) {
						azureSlave.setSshPort(endPoint.getPort());
						break;
					}
				}
				break;
			}
		}
	}

	private static void uploadCertsIfNotExists(ComputeManagementClient client,
			String cloudServiceName, byte[] certData)
			throws AzureCloudException {
		try {
			if (checkIfCertExists(client, cloudServiceName, certData)) {
				LOGGER.info("Certificate alreday exists in hosted service "
						+ cloudServiceName);
				return;
			}

			// Try to upload to hosted service
			ServiceCertificateCreateParameters params = new ServiceCertificateCreateParameters();
			params.setCertificateFormat(CertificateFormat.Cer);
			params.setData(certData);
			client.getServiceCertificatesOperations().create(cloudServiceName,
					params);

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Error: Unable to upload certificates due to "
					+ e.getMessage());
			throw new AzureCloudException(
					"Error: Unable to upload certificates due to "
							+ e.getMessage());
		}

	}

	private static boolean checkIfCertExists(ComputeManagementClient client,
			String cloudServiceName, byte[] certData) {
		boolean exists = false;
		try {
			Map<String, String> certMap = getCertInfo(certData);

			if (certMap != null) {
				ServiceCertificateGetParameters certParams = new ServiceCertificateGetParameters();
				certParams.setServiceName(cloudServiceName);
				certParams.setThumbprint(certMap.get("thumbPrint"));
				certParams.setThumbprintAlgorithm(certMap.get("certAlg"));
				;
				ServiceCertificateGetResponse resp = client
						.getServiceCertificatesOperations().get(certParams);
				exists = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Error occured while getting cert data");
			return exists;
		}
		return exists;
	}

	private static Map<String, String> getCertInfo(byte[] certData)
			throws AzureCloudException {
		Map<String, String> certDataMap = new HashMap<String, String>();

		try {
			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");

			InputStream is = new ByteArrayInputStream(certData);
			X509Certificate cert = (X509Certificate) certificateFactory
					.generateCertificate(is);

			String certAlg = cert.getSigAlgName();
			certDataMap.put("certAlg", certAlg);
			// Calculate thumbPrint
			MessageDigest mdigest = MessageDigest.getInstance(certAlg);
			byte[] der = cert.getEncoded();
			mdigest.update(der);
			byte[] digest = mdigest.digest();
			String thumbPrint = hexify(digest).toUpperCase();

			certDataMap.put("thumbPrint", thumbPrint);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Error occured while parsing certificate data "
					+ e.getMessage());
			throw new AzureCloudException(
					"Error occured while parsing certificate data "
							+ e.getMessage());
		}
		return certDataMap.size() > 0 ? certDataMap : null;
	}

	private static AzureSlave parseDeploymentResponse(
			OperationStatusResponse response, String cloudServiceName,
			AzureSlaveTemplate template,
			VirtualMachineCreateDeploymentParameters params) {

		try {
			String osType = "Windows";
			for (ConfigurationSet configSet : params.getRoles().get(0).getConfigurationSets()) {
				if (configSet.getConfigurationSetType().equals(ConfigurationSetTypes.LINUXPROVISIONINGCONFIGURATION)) {
					osType = "Linux";
					break;
				}
			}
			
			LOGGER.info("AzureManagementServiceDelegate: found slave OS type as "+osType);
			
			AzureCloud azureCloud = template.getAzureCloud();

			
			return new AzureSlave(params.getRoles().get(0).getRoleName(),
					template.getTemplateDesc(),osType, template.getSlaveWorkSpace(),
					template.getNoOfExecutors(),
					template.getUseSlaveAlwaysIfAvail(), template.getLabels(),
					template.getAzureCloud().getDisplayName(),
					template.getAdminUserName(), template.getSshPrivateKey(),
					template.getSshPassPhrase(), template.getAdminPassword(),
					template.getJvmOptions(), template.isShutdownOnIdle(),
					cloudServiceName, params.getName(),
					template.getRetentionTimeInMin(), template.getInitScript(), azureCloud.getSubscriptionId(),
					azureCloud.getServiceManagementCert(), azureCloud.passPhrase, azureCloud.serviceManagementURL);
		} catch (FormException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static AzureSlave parseResponse(OperationStatusResponse response,
			String cloudServiceName, String deploymentName,
			AzureSlaveTemplate template, VirtualMachineCreateParameters params) {

		try {
			String osType = "Windows";
			for (ConfigurationSet configSet : params.getConfigurationSets()) {
				if (configSet.getConfigurationSetType().equals(ConfigurationSetTypes.LINUXPROVISIONINGCONFIGURATION)) {
					osType = "Linux";
					break;
				}
			}
			
			LOGGER.info("AzureManagementServiceDelegate: found slave OS type as "+osType);
			AzureCloud azureCloud = template.getAzureCloud();
			
			return new AzureSlave(params.getRoleName(),
					template.getTemplateDesc(), osType, template.getSlaveWorkSpace(),
					template.getNoOfExecutors(),
					template.getUseSlaveAlwaysIfAvail(), template.getLabels(),
					template.getAzureCloud().getDisplayName(),
					template.getAdminUserName(), template.getSshPrivateKey(),
					template.getSshPassPhrase(), template.getAdminPassword(),
					template.getJvmOptions(), template.isShutdownOnIdle(),
					cloudServiceName, deploymentName,
					template.getRetentionTimeInMin(), template.getInitScript(), azureCloud.getSubscriptionId(), azureCloud.getServiceManagementCert(),
					azureCloud.getPassPhrase(), azureCloud.getServiceManagementURL());
		} catch (FormException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean createCloudServiceIfNotExists(Configuration config,
			String cloudServiceName, String location) throws IOException,
			ServiceException, ParserConfigurationException, SAXException,
			AzureCloudException, URISyntaxException, InterruptedException,
			ExecutionException, TransformerException {

		ComputeManagementClient client = getComputeManagementClient(config);

		// check if already exists
		if (checkIfCloudServiceExists(client, cloudServiceName)) {
			LOGGER.info("Cloud Service already exists , no need to create one");
			return false;
		}

		// Check if name available
		if (!checkIfCloudServiceNameAvailable(client, cloudServiceName)) {
			LOGGER.info("Cloud Service alreday exists , no need to create one");
			throw new AzureCloudException(
					"Error: Cloud Service Name is not available , try configuring a different name in global configuration");
		}

		HostedServiceCreateParameters params = new HostedServiceCreateParameters();
		params.setServiceName(cloudServiceName);
		params.setLabel(cloudServiceName);
		params.setLocation(location);
		// Create new cloud service
		client.getHostedServicesOperations().create(params);

		LOGGER.info(" Created new cloud service with name " + cloudServiceName);
		return true;
	}

	private static boolean checkIfCloudServiceExists(
			ComputeManagementClient client, String cloudServiceName) {
		boolean exists = true;

		try {
			client.getHostedServicesOperations().get(cloudServiceName);
		} catch (Exception e) {
			LOGGER.info("Cloud service doesnot exists in subscription, need to create one");
			exists = false;
		}
		return exists;
	}

	private static boolean checkIfCloudServiceNameAvailable(
			ComputeManagementClient client, String cloudServiceName)
			throws IOException, ServiceException, ParserConfigurationException,
			SAXException {
		return client.getHostedServicesOperations()
				.checkNameAvailability(cloudServiceName).isAvailable();
	}

	private static VirtualMachineCreateDeploymentParameters createVirtualMachineDeploymentParams(Configuration config, 
			String cloudServiceName,
			String deploymentName, AzureSlaveTemplate template)
			throws Exception {
		
		ComputeManagementClient client = getComputeManagementClient(config);

		String sshPublicKeyPath = null;

		VirtualMachineCreateDeploymentParameters parameters = new VirtualMachineCreateDeploymentParameters();
		parameters.setLabel(deploymentName);
		parameters.setName(deploymentName);
		parameters.setDeploymentSlot(DeploymentSlot.Production);

		ArrayList<Role> roles = new ArrayList<Role>();
		parameters.setRoles(roles);

		Role role = new Role();
		roles.add(role);

		String virtualMachineName = cloudServiceName + "VM"
				+ getRandonInt(0, 9);

		// TODO: Get storage account blob uri by making rest call
		URI mediaLinkUriValue = new URI("http://"
				+ template.getStorageAccountName() + ".blob.core.windows.net/"
				+ CI_SYSTEM + "/" + cloudServiceName + "-" + virtualMachineName
				+ ".vhd");

		ArrayList<ConfigurationSet> configurationSets = new ArrayList<ConfigurationSet>();
		role.setConfigurationSets(configurationSets);
		role.setRoleName(virtualMachineName);
		role.setRoleType(VirtualMachineRoleType.PersistentVMRole.toString());
		role.setRoleSize(template.getVirtualMachineSize());
		role.setProvisionGuestAgent(true);
		
		OSVirtualHardDisk oSVirtualHardDisk = new OSVirtualHardDisk();
		role.setOSVirtualHardDisk(oSVirtualHardDisk);
		oSVirtualHardDisk.setMediaLink(mediaLinkUriValue);
		oSVirtualHardDisk.setSourceImageName(template.getImageId());

		ConfigurationSet osSpecificConf = new ConfigurationSet();
		configurationSets.add(osSpecificConf);

		String OSType = getVirtualMachineOSImage(client, template.getImageId());

		// TODO: remove this check later
		System.out.println(template.getAdminPassword()
				+ "template.getAdminPassword()");
		if ("Windows".equalsIgnoreCase(OSType)) {
			osSpecificConf
					.setConfigurationSetType(ConfigurationSetTypes.WINDOWSPROVISIONINGCONFIGURATION);
			osSpecificConf.setComputerName(virtualMachineName);
			osSpecificConf.setAdminUserName(template.getAdminUserName());
			osSpecificConf.setAdminPassword(template.getAdminPassword());
			osSpecificConf.setEnableAutomaticUpdates(false);
			
			//set custom script
			role.setResourceExtensionReferences(handleCustomScriptExtension(config, virtualMachineName, cloudServiceName, template));;
		} else if ("Linux".equalsIgnoreCase(OSType)) {
			sshPublicKeyPath = "/home/user/.ssh/authorized_keys";

			osSpecificConf
					.setConfigurationSetType(ConfigurationSetTypes.LINUXPROVISIONINGCONFIGURATION);
			osSpecificConf.setHostName(virtualMachineName);
			osSpecificConf.setUserName(template.getAdminUserName());

			if (template.getAdminPassword() == null) {
				osSpecificConf.setDisableSshPasswordAuthentication(true);
			}

			if (template.getAdminPassword() == null
					|| template.getAdminPassword().trim().length() == 0) {
				osSpecificConf.setUserPassword("Abcd.1234");
			}
			osSpecificConf.setUserPassword(template.getAdminPassword());
			osSpecificConf.setAdminPassword("Abcd.1234");
			osSpecificConf.setDisableSshPasswordAuthentication(false);

			// Configure SSH
			// SshSettings sshSettings = new SshSettings();
			// osSpecificConf.setSshSettings(sshSettings);
			//
			// //Get certificate thumprint
			// Map<String, String> certMap =
			// getCertInfo(template.getSshPublicKey().getBytes("UTF-8"));
			//
			// ArrayList<SshSettingPublicKey> publicKeys= new
			// ArrayList<SshSettingPublicKey>();
			// sshSettings.setPublicKeys(publicKeys);
			// // Add public key
			// SshSettingPublicKey publicKey = new SshSettingPublicKey();
			// publicKeys.add(publicKey);
			// publicKey.setFingerprint(certMap.get("thumbPrint"));
			// publicKey.setPath(sshPublicKeyPath);
			//
			// ArrayList<SshSettingKeyPair> keyPairs= new
			// ArrayList<SshSettingKeyPair>();
			// sshSettings.setKeyPairs(keyPairs);
			// // Add key pair
			// SshSettingKeyPair keyPair = new SshSettingKeyPair();
			// keyPairs.add(keyPair);
			// keyPair.setFingerprint(sshPKFingerPrint);
			// keyPair.setPath(sshKeyPairPath);

		} else {
			throw new AzureCloudException("Unsupported OSType " + OSType);
		}

		// Network configuration set
		ConfigurationSet networkConfigset = new ConfigurationSet();
		configurationSets.add(networkConfigset);
		networkConfigset
				.setConfigurationSetType(ConfigurationSetTypes.NETWORKCONFIGURATION);
		// Define endpoints
		ArrayList<InputEndpoint> enpoints = new ArrayList<InputEndpoint>();
		networkConfigset.setInputEndpoints(enpoints);

		// Add RDP endpoint
		InputEndpoint rdpPort = new InputEndpoint();
		enpoints.add(rdpPort);

		if ("Windows".equalsIgnoreCase(OSType)) {
			rdpPort.setName("ssh");
			rdpPort.setProtocol("tcp");
			rdpPort.setLocalPort(DEFAULT_SSH_PORT);
			// rdpPort.setName(endPointName);
			// rdpPort.setProtocol(endPointProtocol);
			// rdpPort.setLocalPort(endPointLocalPort);
			// rdpPort.setPort(endPointPort);
		} else if ("Linux".equalsIgnoreCase(OSType)) {
			rdpPort.setName("ssh");
			rdpPort.setProtocol("tcp");
			rdpPort.setLocalPort(DEFAULT_SSH_PORT);
			// purposefully not setting public port so that azure assigns a
			// random.
		}
		return parameters;
	}

	private static int getAvailablePort(ComputeManagementClient client,
			String cloudServiceName) {
		try {
			List<Integer> publicEndPoints = new ArrayList<Integer>();
			ArrayList<RoleInstance> roleInstances = client
					.getDeploymentsOperations()
					.getBySlot(cloudServiceName, DeploymentSlot.Production)
					.getRoleInstances();

			// Get all public ports
			for (RoleInstance instance : roleInstances) {
				for (InstanceEndpoint endpoint : instance
						.getInstanceEndpoints()) {
					publicEndPoints.add(endpoint.getPort());
				}
			}

			//
			int sshPort = DEFAULT_SSH_PORT;
			while (publicEndPoints.contains(sshPort)) {
				sshPort = getRandonInt(1024, 65000);
			}

			LOGGER.info("Assigning public ssh port value " + sshPort
					+ " for new instance in cloud service " + cloudServiceName);
			return sshPort;
		} catch (Exception e) {
			LOGGER.severe("Error occured while retrieving available port, returning default value ");
			return DEFAULT_SSH_PORT;
		}
	}

	private static String getCurrentDate() {
		// Format formatter = new SimpleDateFormat("YYYYMMddhhmmss");
		Format formatter = new SimpleDateFormat("ss");
		return formatter.format(new Date(System.currentTimeMillis()));
	}

	private static String getVirtualMachineOSImage(
			ComputeManagementClient client, String imageID)
			throws AzureCloudException {
		try {
			return client.getVirtualMachineOSImagesOperations().get(imageID)
					.getOperatingSystemType();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.severe("Error occured while retrieving image details for "
					+ imageID);
			LOGGER.severe("Verify that image name is correct in global configuration");

			throw new AzureCloudException(
					"Error occured while retrieving image details for "
							+ imageID);
		}

	}

	/*
	 * public static Configuration loadConfiguration(String subscriptionId,
	 * String publishSettingsPath) throws IOException {
	 * 
	 * Configuration config = PublishSettingsLoader
	 * .createManagementConfiguration(publishSettingsPath, subscriptionId);
	 * 
	 * return config; }
	 */

	public static Configuration loadConfiguration(String subscriptionId,
			String serviceManagementCert, String passPhrase,
			String serviceManagementURL) throws IOException {
		
		 ClassLoader thread = Thread.currentThread().getContextClassLoader();
		 Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
	
		try {
		 if (passPhrase == null || passPhrase.trim().length() == 0) {
			passPhrase = "";
		}

		URI managementURI = null;

		try {
			managementURI = new URI(serviceManagementURL);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(
					"The syntax of the Url in the publish settings file is incorrect.",
					e);
		}

		// Form outFile
		String outputKeyStore = System.getProperty("user.home")
				+ File.separator + ".azure" + File.separator + subscriptionId
				+ ".out";

		createKeyStoreFromCertifcate(serviceManagementCert, outputKeyStore,
				passPhrase);
		return ManagementConfiguration.configure(managementURI, subscriptionId,
				outputKeyStore, passPhrase, KeyStoreType.pkcs12);
		} finally {
			Thread.currentThread().setContextClassLoader(thread);
		}
	}

	public static List<String> getVirtualMachineLocations(
			String subscriptionId, String serviceManagementCert,
			String passPhrase, String serviceManagementURL) throws IOException,
			ServiceException, ParserConfigurationException, SAXException {

		List<String> locations = new ArrayList<String>();

		Configuration config = loadConfiguration(subscriptionId,
				serviceManagementCert, passPhrase, serviceManagementURL);

		ManagementClient managementClient = getManagementClient(config);
		LocationsListResponse listResponse = managementClient
				.getLocationsOperations().list();

		for (Iterator<Location> iterator = listResponse.iterator(); iterator
				.hasNext();) {
			Location location = iterator.next();
			for (String availServices : location.getAvailableServices()) {
				// check for PersistentVMRole
				if ("PersistentVMRole".equalsIgnoreCase(availServices)) {
					locations.add(location.getName());
					// break inner for loop
					break;
				}
			}

		}

		return locations;
	}

	public static List<String> getVMSizes(String subscriptionId,
			String serviceManagementCert, String passPhrase,
			String serviceManagementURL) throws IOException, ServiceException,
			ParserConfigurationException, SAXException {

		List<String> vmSizes = new ArrayList<String>();

		Configuration config = loadConfiguration(subscriptionId,
				serviceManagementCert, passPhrase, serviceManagementURL);

		ManagementClient managementClient = getManagementClient(config);
		RoleSizeListResponse roleSizeListResponse = managementClient
				.getRoleSizesOperations().list();

		for (RoleSize roleSize : roleSizeListResponse.getRoleSizes()) {
			if (roleSize.isSupportedByVirtualMachines()) {
				vmSizes.add(roleSize.getName());
			}
		}

		return vmSizes;
	}

	public static String verifyConfiguration(String subscriptionId,
			String serviceManagementCert, String passPhrase,
			String serviceManagementURL) {
		try {
			Configuration config = loadConfiguration(subscriptionId,
					serviceManagementCert, passPhrase, serviceManagementURL);
			ComputeManagementClient client = getComputeManagementClient(config);
			client.getHostedServicesOperations().checkNameAvailability(
					"CI_SYSTEM");

			return "Success";
		} catch (Exception e) {
			e.printStackTrace();
			return "Failure: Exception occured while validating subscription configuration"
					+ e;
		}
	}
	
	public static String getVirtualMachineStatus(Configuration config, String cloudServiceName, DeploymentSlot slot, String VMName) throws Exception {
		String status = "";
		ComputeManagementClient client = getComputeManagementClient(config);
		
		ArrayList<RoleInstance> roleInstances = client.getDeploymentsOperations().getBySlot(cloudServiceName, DeploymentSlot.Production).getRoleInstances();
		
		for (RoleInstance instance : roleInstances) {
			if (instance.getRoleName().equals(VMName)) {
				status = instance.getInstanceStatus();
				break;
			}
		}
		
		return status;
	}
	
	public static ComputeManagementClient getComputeManagementClient(Configuration config) {
		
		 ClassLoader thread = Thread.currentThread().getContextClassLoader();
		 Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
	
		 try {
			 return ComputeManagementService.create(config);
		 } finally {
			 Thread.currentThread().setContextClassLoader(thread);
		 }
	}
	
	public static ComputeManagementClient getComputeManagementClient(AzureSlave slave) throws Exception {
		Configuration config = loadConfiguration(slave.getSubscriptionID(), slave.getManagementCert(), 
				   slave.getPassPhrase(), slave.getManagementURL());
		return getComputeManagementClient(config);
	}
	
	public static StorageManagementClient getStorageManagementClient(Configuration config) {
		
		 ClassLoader thread = Thread.currentThread().getContextClassLoader();
		 Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
	
		 try {
			 return StorageManagementService.create(config);
		 } finally {
			 Thread.currentThread().setContextClassLoader(thread);
		 }
	}
	
	public static ManagementClient getManagementClient(Configuration config) {
		
		 ClassLoader thread = Thread.currentThread().getContextClassLoader();
		 Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
	
		 try {
			 return ManagementService.create(config);
		 } finally {
			 Thread.currentThread().setContextClassLoader(thread);
		 }
	}


	public static KeyStore createKeyStoreFromCertifcate(String certificate,
			String keyStoreFileName, String passPhrase) throws IOException {
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("PKCS12");

			keyStore.load(null, "".toCharArray());

			InputStream sslInputStream = new ByteArrayInputStream(
					Base64.decode(certificate));

			keyStore.load(sslInputStream, "".toCharArray());

			// create directories if does not exists
			File outStoreFile = new File(keyStoreFileName);
			if (!outStoreFile.getParentFile().exists()) {
				outStoreFile.getParentFile().mkdirs();
			}

			OutputStream outputStream;
			outputStream = new FileOutputStream(keyStoreFileName);
			keyStore.store(outputStream, passPhrase.toCharArray());
			outputStream.close();

		} catch (KeyStoreException e) {
			throw new IllegalArgumentException(
					"Cannot create keystore from the publish settings file", e);
		} catch (CertificateException e) {
			throw new IllegalArgumentException(
					"Cannot create keystore from the publish settings file", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(
					"Cannot create keystore from the publish settings file", e);
		}

		return keyStore;
	}

	private static String hexify(byte bytes[]) {
		char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		StringBuffer buf = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; ++i) {
			buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
			buf.append(hexDigits[bytes[i] & 0x0f]);
		}
		return buf.toString();
	}

	public static int getRandonInt(int minRange, int maxRange) {
		Random random = new Random();
		return random.nextInt((maxRange - minRange) + 1) + minRange;
	}
	
	public static void shutdownVirtualMachine(AzureSlave slave) throws Exception {
		ComputeManagementClient client = getComputeManagementClient(slave);
		
		VirtualMachineShutdownParameters params = new VirtualMachineShutdownParameters();
		params.setPostShutdownAction(PostShutdownAction.StoppedDeallocated);
		client.getVirtualMachinesOperations().shutdown(slave.getCloudServiceName(), slave.getDeploymentName(), slave.getNodeName(), params);
		
		
	}
	
	public static void terminateVirtualMachine(AzureSlave slave) throws Exception {
		ComputeManagementClient client = getComputeManagementClient(slave);
		client.getVirtualMachinesOperations().delete(slave.getCloudServiceName(), slave.getDeploymentName(), slave.getNodeName(), true);
	}
	
	public static void restartVirtualMachine(AzureSlave slave) throws Exception {
		ComputeManagementClient client = getComputeManagementClient(slave);
		client.getVirtualMachinesOperations().restart(slave.getCloudServiceName(), slave.getDeploymentName(), slave.getNodeName());
	}
	
	public static void startVirtualMachine(AzureSlave slave) throws Exception {
		ComputeManagementClient client = getComputeManagementClient(slave);
		client.getVirtualMachinesOperations().start(slave.getCloudServiceName(), slave.getDeploymentName(), slave.getNodeName());
	}
	
	


}
