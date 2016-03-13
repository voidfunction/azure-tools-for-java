package com.microsoftopentechnologies.azure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.apache.commons.codec.binary.Base64;

import hudson.Extension;
import hudson.Util;
import hudson.model.Failure;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;

import com.microsoftopentechnologies.azure.AzureSlaveTemplate;

import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.util.FormValidation;
import hudson.util.StreamTaskListener;

public class AzureCloud extends Cloud {

	public final String subscriptionId;
	public final String serviceManagementCert;
	public final String passPhrase;
	public final String serviceManagementURL;
	public final int maxVirtualMachinesLimit;
	public final String DEFAULT_MANAGEMENT_URL = "https://management.core.windows.net";

	public final List<AzureSlaveTemplate> instTemplates;

	public static final Logger LOGGER = Logger.getLogger(AzureCloud.class
			.getName());

	@DataBoundConstructor
	public AzureCloud(String id, String subscriptionId,
			String serviceManagementCert, String passPhrase, String serviceManagementURL,
			String maxVirtualMachinesLimit,
			List<AzureSlaveTemplate> instTemplates, String fileName, String fileData ) {
		super(id);
		System.out.println("Identity " + id);
		this.subscriptionId = subscriptionId;
		this.serviceManagementCert = serviceManagementCert;
		if (serviceManagementURL == null || serviceManagementURL.trim().length() == 0) {
			serviceManagementURL = DEFAULT_MANAGEMENT_URL;
		}
		this.serviceManagementURL = serviceManagementURL;
		this.passPhrase = passPhrase;

		if (maxVirtualMachinesLimit.equals("")) {
			this.maxVirtualMachinesLimit = Integer.MAX_VALUE;
		} else {
			this.maxVirtualMachinesLimit = Integer
					.parseInt(maxVirtualMachinesLimit);
		}

		if (instTemplates == null) {
			this.instTemplates = Collections.emptyList();
		} else {
			this.instTemplates = instTemplates;
		}
		readResolve();
	}

	protected Object readResolve() {
		for (AzureSlaveTemplate template : instTemplates)
			template.azureCloud = this;
		return this;
	}

	@Override
	public boolean canProvision(Label label) {
		return getAzureSlaveTemplate(label) != null;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public String getServiceManagementCert() {
		return serviceManagementCert;
	}

	public String getServiceManagementURL() {
		return serviceManagementURL;
	}

	public int getMaxVirtualMachinesLimit() {
		return maxVirtualMachinesLimit;
	}
	
	public String getPassPhrase() {
		return passPhrase;
	}

	private AzureSlaveTemplate getAzureSlaveTemplate(Label label) {
		for (AzureSlaveTemplate slaveTemplate : instTemplates) {
			// TODO: Introduce default template concept
			if ((null == label)
					|| (label.matches(slaveTemplate.getLabelDataSet()))) {
				return slaveTemplate;
			}
		}
		return null;
	}

	@Override
	public Collection<PlannedNode> provision(Label label, int workLoad) {

		final AzureSlaveTemplate slaveTemplate = getAzureSlaveTemplate(label);
		List<PlannedNode> plannedNodes = new ArrayList<PlannedNode>();

		while (workLoad > 0) {
			// TODO: Need manual test here
			if (getInstancesCount() > maxVirtualMachinesLimit) {
				LOGGER.info("Max. no of virtual machine instances limit reached while trying to provision instance with label "
						+ label);
				break;
			}

			plannedNodes.add(new PlannedNode(slaveTemplate.getTemplateName(),
					Computer.threadPoolForRemoting.submit(new Callable<Node>() {
						public Node call() throws Exception {
							AzureSlave slave = slaveTemplate
									.provisionSlave(new StreamTaskListener(
											System.out));
													
							// Get virtual machine properties
							 LOGGER.info("Azure cloud: Getting virtual machine properties");
							 slaveTemplate.setVirtualMachineDetails(slave);
							 
							 
							 if (slave.getOsType().equals("Linux")) {
								 Hudson.getInstance().addNode(slave);
								 slaveTemplate.waitForReadyRole(slave);
								 LOGGER.info("Azure Cloud: Trying to connect via ssh ");
								 slave.toComputer().connect(false).get();
								 LOGGER.info("Azure Cloud: verified connectivity");
							 } else {
//								 Hudson.getInstance().addNode(new DumbSlave(slave.getNodeName(),slave.getNodeDescription(),slave.getRemoteFS(),slave.getNumExecutors()+"",slave.getMode(),slave.getLabelString(),new JNLPLauncher(),
//								 new RetentionStrategy.Always(),new LinkedList()));
								 Hudson.getInstance().addNode(slave);
								 slaveTemplate.waitForReadyRole(slave);
								 
								 LOGGER.info("Azure Cloud: Waiting for init script to be executed on Windows slave");
								 Thread.sleep(3 * 60 * 1000);
								 // ??? Is there any other better way to check the init script status
								 LOGGER.info("Azure Cloud: Expecting init script to be completed");
								 
								 LOGGER.info("Azure Cloud: Checking for slave status");
								 if (slave.toComputer().isOffline()) {
									 LOGGER.info("Azure Cloud: Slave is still offline , sleeping for 1 more minute");
									 Thread.sleep(1 * 60 * 1000);
								 }
							 }
							 
							return slave;
						}
					}), slaveTemplate.getNoOfExecutors()));

			// Decrement workload
			workLoad -= slaveTemplate.getNoOfExecutors();
		}
		return plannedNodes;
	}

	private int getInstancesCount() {
		// TODO Write code to get current instances in cloud
		return 0;
	}

	public List<AzureSlaveTemplate> getInstTemplates() {
		return Collections.unmodifiableList(instTemplates);
	}
	
	

	@Extension
	public static class DescriptorImpl extends Descriptor<Cloud> {

		public String getDisplayName() {
			return "Microsoft Azure";
		}

		public String getDefaultserviceManagementURL() {
			return "https://management.core.windows.net";
		}

		public FormValidation doImportCert(StaplerResponse rsp) {
			return FormValidation.ok(Messages.Azure_Import_Cert_Success());
		}

		public FormValidation doVerifyConfiguration(@QueryParameter String subscriptionId,
				@QueryParameter String serviceManagementCert, @QueryParameter String passPhrase, @QueryParameter String serviceManagementURL) {
			
			// TODO: find out why serviceManagementURL coming has null
			System.out.println("serviceManagementURL "+serviceManagementURL);
			if (serviceManagementURL == null || serviceManagementURL.trim().length() == 0) {
				serviceManagementURL = getDefaultserviceManagementURL();
			}
			
			String response = AzureManagementServiceDelegate.verifyConfiguration(subscriptionId, serviceManagementCert, passPhrase, serviceManagementURL);
			
			System.out.println("response ---------------------->"+response);
			if (response.equalsIgnoreCase("Success")) {
				return FormValidation.ok(Messages.Azure_Config_Success());
			} else {
				return FormValidation.error(response);
			}
		}
		
		}
}
