package com.microsoftopentechnologies.azure;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.management.compute.models.DeploymentSlot;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Describable;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import hudson.util.ListBoxModel;

/**
 * This class defines the configuration of Azure instance templates
 * 
 * @author v-sunal
 * 
 */
public class AzureSlaveTemplate implements Describable<AzureSlaveTemplate> {
	// General Configuration
	public final String templateName;
	public final String templateDesc;
	public final String labels;
	private final String location;
	public final String virtualMachineSize;
	public final String storageAccountName;
	public final int noOfExecutors;
	public final Node.Mode useSlaveAlwaysIfAvail;
	public final boolean shutdownOnIdle;

	// Image Configuration
	public final String imageId;
	public final String initScript;
	public final String adminUserName;
	private final String sshPublicKey;
	private final String sshPrivateKey;
	private final String sshPassPhrase;

	public final String adminPassword;
	public final String endPoints;
	public final String slaveWorkSpace;
	public final int retentionTimeInMin;
	public final String jvmOptions;
	public final String cloudServiceName;
	public transient AzureCloud azureCloud;

	private transient Set<LabelAtom> labelDataSet;

	private static final Logger LOGGER = Logger
			.getLogger(AzureSlaveTemplate.class.getName());

	@DataBoundConstructor
	public AzureSlaveTemplate(String templateName, String templateDesc,
			String labels, String location, String virtualMachineSize,
			String storageAccountName, String noOfExecutors,
			Node.Mode useSlaveAlwaysIfAvail, boolean shutdownOnIdle,
			String imageId, String initScript, String adminUserName,
			String sshPublicKey, String sshPrivateKey, String sshPassPhrase,
			String adminPassword, String endPoints, String slaveWorkSpace,
			String jvmOptions, String retentionTimeInMin,
			String cloudServiceName) {

		this.templateName = templateName;
		this.templateDesc = templateDesc;
		this.labels = labels;
		this.location = location;
		this.virtualMachineSize = virtualMachineSize;
		this.storageAccountName = storageAccountName;

		if (null == noOfExecutors || noOfExecutors.trim().equals("")) {
			this.noOfExecutors = 1;
		} else {
			this.noOfExecutors = Integer.parseInt(noOfExecutors);
		}

		this.useSlaveAlwaysIfAvail = useSlaveAlwaysIfAvail;
		this.shutdownOnIdle = shutdownOnIdle;
		this.imageId = imageId;
		this.initScript = initScript;
		this.adminUserName = adminUserName;
		this.sshPublicKey = sshPublicKey;
		this.sshPrivateKey = sshPrivateKey;
		this.sshPassPhrase = sshPassPhrase;
		this.adminPassword = adminPassword;
		this.endPoints = endPoints;
		this.slaveWorkSpace = slaveWorkSpace;
		this.jvmOptions = jvmOptions;
		if (null == retentionTimeInMin || retentionTimeInMin.trim().equals("")) {
			this.retentionTimeInMin = 30;
		} else {
			this.retentionTimeInMin = Integer.parseInt(retentionTimeInMin);
		}

		this.cloudServiceName = cloudServiceName;

		// Forms data which is not persisted
		readResolve();
	}

	private Object readResolve() {
		labelDataSet = Label.parse(labels);
		return this;
	}

	public String getLabels() {
		return labels;
	}

	public String getLocation() {
		return location;
	}

	public String getVirtualMachineSize() {
		return virtualMachineSize;
	}

	public String getStorageAccountName() {
		return storageAccountName;
	}

	public Node.Mode getUseSlaveAlwaysIfAvail() {
		return useSlaveAlwaysIfAvail;
	}

	public boolean isShutdownOnIdle() {
		return shutdownOnIdle;
	}

	public String getImageId() {
		return imageId;
	}

	public String getInitScript() {
		return initScript;
	}

	public String getAdminUserName() {
		return adminUserName;
	}

	public String getSshPublicKey() {
		return sshPublicKey;
	}

	public String getSshPrivateKey() {
		return sshPrivateKey;
	}

	public String getSshPassPhrase() {
		return sshPassPhrase;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public String getEndPoints() {
		return endPoints;
	}

	public String getSlaveWorkSpace() {
		return slaveWorkSpace;
	}

	public int getRetentionTimeInMin() {
		return retentionTimeInMin;
	}

	public String getJvmOptions() {
		return jvmOptions;
	}

	public String getCloudServiceName() {
		return cloudServiceName;
	}

	public AzureCloud getAzureCloud() {
		return azureCloud;
	}

	public String getTemplateName() {
		return templateName;
	}

	public String getTemplateDesc() {
		return templateDesc;
	}

	public int getNoOfExecutors() {
		return noOfExecutors;
	}

	public Descriptor<AzureSlaveTemplate> getDescriptor() {
		return Jenkins.getInstance().getDescriptor(getClass()); // c1
	}

	public Set<LabelAtom> getLabelDataSet() {
		return labelDataSet;
	}

	public AzureSlave provisionSlave(TaskListener listener) throws Exception {
		// ClassLoader thread = Thread.currentThread().getContextClassLoader();
		// System.out.println("Default class loader start");
		// System.out.println(ClassLoaderHelper.showClassLoaderHierarchy(thread));
		// System.out.println("Default class loader end");
		// System.out.println("Class class loader start");
		// Thread.currentThread().setContextClassLoader(AzureManagementServiceDelegate.class.getClassLoader());
		// System.out.println(ClassLoaderHelper.showClassLoaderHierarchy(AzureManagementServiceDelegate.class.getClassLoader()));
		// System.out.println("Class class loader end");
		try {
			
			return AzureManagementServiceDelegate.createVirtualMachine(this,
					listener);
		} finally {
			// Thread.currentThread().setContextClassLoader(thread);
		}
	}
	
	public void waitForReadyRole(AzureSlave slave) throws Exception {
		Configuration config = AzureManagementServiceDelegate.loadConfiguration(azureCloud.getSubscriptionId(),
				azureCloud.getServiceManagementCert(),
				azureCloud.getPassPhrase(),
				azureCloud.getServiceManagementURL());
		
		String status = "NA";
		
		while (!status.equalsIgnoreCase("ReadyRole")) {
			status = AzureManagementServiceDelegate.getVirtualMachineStatus(config, slave.getCloudServiceName(), DeploymentSlot.Production, slave.getNodeName());
			
			LOGGER.info("Current status of virtual machine "+slave.getNodeName()+" is "+status);
			LOGGER.info("Waiting for 30 more seconds for role to be ready");
			Thread.sleep(30 * 1000);
			
			//TODO: Implement timeout
		}
		
		LOGGER.info("virtual machine "+slave.getNodeName()+" is in ready state");
	}

	@Extension
	public static final class DescriptorImpl extends
			Descriptor<AzureSlaveTemplate> {

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return null;
		}

		public ListBoxModel doFillVirtualMachineSizeItems(
				@RelativePath("..") @QueryParameter String subscriptionId,
				@RelativePath("..") @QueryParameter String serviceManagementCert,
				@RelativePath("..") @QueryParameter String passPhrase,
				@RelativePath("..") @QueryParameter String serviceManagementURL)
				throws IOException, ServletException {

			System.out.println("Virtual machines sizes");
			System.out.println("subscriptionId" + subscriptionId);
			System.out.println("serviceManagementCert" + serviceManagementCert);
			System.out.println("passPhrase" + passPhrase);
			System.out.println("serviceManagementURL" + serviceManagementURL);

			ListBoxModel model = new ListBoxModel();

			if ((subscriptionId == null || subscriptionId.trim().length() == 0)
					&& (serviceManagementCert == null || serviceManagementCert
							.trim().length() == 0)
					&& (serviceManagementURL == null || serviceManagementURL
							.trim().length() == 0)) {
				return model;
			}
			
			try {

				List<String> vmSizes = AzureManagementServiceDelegate
						.getVMSizes(subscriptionId,
								serviceManagementCert, passPhrase,
								serviceManagementURL);

				for (String vmSize : vmSizes) {
					model.add(vmSize);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return model;

		}

		public ListBoxModel doFillLocationItems(
				@RelativePath("..") @QueryParameter String subscriptionId,
				@RelativePath("..") @QueryParameter String serviceManagementCert,
				@RelativePath("..") @QueryParameter String passPhrase,
				@RelativePath("..") @QueryParameter String serviceManagementURL)
				throws IOException, ServletException {
			System.out.println("subscriptionId" + subscriptionId);
			System.out.println("serviceManagementCert" + serviceManagementCert);
			System.out.println("passPhrase" + passPhrase);
			System.out.println("serviceManagementURL" + serviceManagementURL);

			ListBoxModel model = new ListBoxModel();

			// validate
			if ((subscriptionId == null || subscriptionId.trim().length() == 0)
					&& (serviceManagementCert == null || serviceManagementCert
							.trim().length() == 0)
					&& (serviceManagementURL == null || serviceManagementURL
							.trim().length() == 0)) {
				return model;
			}

			try {

				List<String> locations = AzureManagementServiceDelegate
						.getVirtualMachineLocations(subscriptionId,
								serviceManagementCert, passPhrase,
								serviceManagementURL);

				for (String location : locations) {
					model.add(location);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return model;


		}

		public FormValidation doGenerateSSHKeys(StaplerResponse rsp) {
			return FormValidation.ok(Messages.Azure_SSH_Keys_Success());
		}

	}

	public void setVirtualMachineDetails(AzureSlave slave) throws Exception {
		AzureManagementServiceDelegate.setVirtualMachineDetails(slave,this);		
	}
}
