package com.microsoftopentechnologies.azure;

import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
import hudson.util.TimeUnit2;

public class AzureCloudRetensionStrategy extends
RetentionStrategy<AzureComputer>  {

	public final int idleTerminationMinutes;
	
	private static final Logger LOGGER = Logger.getLogger(AzureManagementServiceDelegate.class.getName());
	

	@DataBoundConstructor
	public AzureCloudRetensionStrategy(int idleTerminationMinutes) {
		this.idleTerminationMinutes = idleTerminationMinutes;
	}

	@Override
	public long check(AzureComputer slaveNode) {

        /* If we've been told never to terminate, then we're done. */
        if  (idleTerminationMinutes == 0) {
        	return 1;
        }

        if (slaveNode.isIdle() && slaveNode.isOnline()) {
            if (idleTerminationMinutes > 0) {
                // TODO: really think about the right strategy here
                final long idleMilliseconds = System.currentTimeMillis() - slaveNode.getIdleStartMilliseconds();
                if (idleMilliseconds > TimeUnit2.MINUTES.toMillis(idleTerminationMinutes)) {
                    LOGGER.info("Idle timeout: "+slaveNode.getName());
                    try {
						slaveNode.getNode().idleTimeout();
					} catch (Exception e) {
						// TODO: how to handle this case
						LOGGER.info("Exception occured while calling timeout on node");
						e.printStackTrace();
					}
                }
            } 
        }
        return 1;
		
	}

	@Override
	public void start(AzureComputer c) {
		c.connect(false);
	}

	public static class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
		@Override
		public String getDisplayName() {
			return "Azure";
		}
	}

}
