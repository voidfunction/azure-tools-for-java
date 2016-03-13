package com.microsoftopentechnologies.azure;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.SlaveComputer;

public class AzureComputer extends AbstractCloudComputer<AzureSlave>  {
	
	private static final Logger LOGGER = Logger.getLogger(AzureComputer.class.getName());

	public AzureComputer(AzureSlave slave) {
		super(slave);
	}
	
	public AzureSlave getNode() {
        return (AzureSlave)super.getNode();
    }
}
