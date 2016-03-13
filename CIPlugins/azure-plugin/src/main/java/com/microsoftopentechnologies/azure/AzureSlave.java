package com.microsoftopentechnologies.azure;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.microsoftopentechnologies.ssh.AzureLauncher;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Descriptor.FormException;
import hudson.model.Slave;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;

public class AzureSlave extends AbstractCloudSlave  {

	private static final long serialVersionUID = 1L;

	private final String cloudName;
	
	private final String adminUserName;
	private final String sshPrivateKey;
	private final String sshPassPhrase;
	private final String adminPassword;
	private final String jvmOptions;
	private final boolean shutdownOnIdle;
	
	private final String cloudServiceName;
	private final int retentionTimeInMin;
	private final String initScript;
	private final String deploymentName;
	private final String osType;
	
	// set during post create step
	private String publicDNSName;
	private int sshPort;
	private final Mode mode;
	private final String subscriptionID;
	private final String managementCert;
	private final String passPhrase;
	private final String managementURL;
	
	private static final Logger LOGGER = Logger
			.getLogger(AzureSlave.class.getName());
	


	@DataBoundConstructor
	public AzureSlave(String name, String nodeDescription, String osType, String remoteFS,
			int numExecutors, Mode mode, String labelString,
			ComputerLauncher launcher, RetentionStrategy<AzureComputer> retentionStrategy,
			List<? extends NodeProperty<?>> nodeProperties, String cloudName,
			String adminUserName, String sshPrivateKey, String sshPassPhrase,
			String adminPassword, String jvmOptions, boolean shutdownOnIdle, 
			String cloudServiceName, String deploymentName, int retentionTimeInMin,
			String initScript, String subscriptionID, String managementCert, String passPhrase,
			String managementURL) throws FormException,
			IOException {
		super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
				launcher, retentionStrategy, nodeProperties);
		this.cloudName = cloudName;
		this.adminUserName = adminUserName;
		this.sshPrivateKey = sshPrivateKey;
		this.sshPassPhrase = sshPassPhrase;
		this.adminPassword = adminPassword;
		this.jvmOptions = jvmOptions;
		this.shutdownOnIdle = shutdownOnIdle;
		this.cloudServiceName = cloudServiceName;
		this.deploymentName = deploymentName;
		this.retentionTimeInMin = retentionTimeInMin;
		this.initScript = initScript;
		this.osType = osType;
		this.mode = mode;
		this.subscriptionID = subscriptionID;
		this.managementCert = managementCert;
		this.passPhrase = passPhrase;
		this.managementURL = managementURL;
	}
	
	public AzureSlave(String name, String nodeDescription, String osType, String remoteFS,
			int numExecutors, Mode mode, String labelString,
			String cloudName, String adminUserName, String sshPrivateKey, String sshPassPhrase,
			String adminPassword, String jvmOptions, boolean shutdownOnIdle,
			String cloudServiceName, String deploymentName, int retentionTimeInMin,
			String initScript, String subscriptionID, String managementCert, String passPhrase,
			String managementURL) throws FormException, IOException {
		
		this(name, nodeDescription, remoteFS, osType, numExecutors, mode, labelString, osType.equalsIgnoreCase("Linux")? new AzureLauncher() : new JNLPLauncher(),
				new AzureCloudRetensionStrategy(retentionTimeInMin), Collections.<NodeProperty<?>> emptyList(), cloudName, adminUserName,
				sshPrivateKey, sshPassPhrase, adminPassword, jvmOptions, shutdownOnIdle, cloudServiceName, deploymentName, retentionTimeInMin, initScript,
				subscriptionID, managementCert, passPhrase, managementURL);
				
	}
	
	

	public String getCloudName() {
		return cloudName;
	}
	
	public Mode getMode() {
		return mode;
	}



	public String getAdminUserName() {
		return adminUserName;
	}



	public String getSubscriptionID() {
		return subscriptionID;
	}

	public String getManagementCert() {
		return managementCert;
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	public String getManagementURL() {
		return managementURL;
	}

	public String getSshPrivateKey() {
		return sshPrivateKey;
	}
	
	public String getOsType() {
		return osType;
	}



	public String getSshPassPhrase() {
		return sshPassPhrase;
	}
	
	public String getCloudServiceName() {
		return cloudServiceName;
	}
	
	public String getDeploymentName() {
		return deploymentName;
	}

	public String getAdminPassword() {
		return adminPassword;
	}



	public String getJvmOptions() {
		return jvmOptions;
	}



	public boolean isShutdownOnIdle() {
		return shutdownOnIdle;
	}



	public String getPublicDNSName() {
		return publicDNSName;
	}
	
	public void setPublicDNSName(String publicDNSName) {
		this.publicDNSName = publicDNSName;
	}



	public int getSshPort() {
		return sshPort;
	}
	
	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}




	public int getRetentionTimeInMin() {
		return retentionTimeInMin;
	}



	public String getInitScript() {
		return initScript;
	}



	protected void _terminate(TaskListener arg0) throws IOException,
			InterruptedException {
		LOGGER.info("Terminate method called for slave "+getNodeName());
		
	}

	@Override
	public AbstractCloudComputer<AzureSlave> createComputer() {
		// TODO Auto-generated method stub
		return new AzureComputer(this);
	}
	
	public void idleTimeout() throws Exception {
		
		if (shutdownOnIdle) {
			AzureManagementServiceDelegate.shutdownVirtualMachine(this);
		} else {
			AzureManagementServiceDelegate.terminateVirtualMachine(this);
		}
		
	}

	
	@Extension
	public static final class AzureSlaveDescriptor extends SlaveDescriptor {

		@Override
		public String getDisplayName() {
			return "Azure Slave";
		}

		public boolean isInstantiable() {
			return false;
		}
	}


}
