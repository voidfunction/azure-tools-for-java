package com.microsoftopentechnologies.ssh;



import com.microsoftopentechnologies.azure.AzureSlave;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;

import com.microsoftopentechnologies.azure.AzureCloud;
import com.microsoftopentechnologies.azure.AzureComputer;

import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.Session;

/**
 * TODO: rewrite
 */
public class AzureLauncher extends ComputerLauncher {

    private final int FAILED=-1;
    private final int SAMEUSER=0;
    private final int RECONNECT=-2;
    
    public static final Logger LOGGER = Logger.getLogger(AzureLauncher.class
			.getName());
    
    public void launch(SlaveComputer _computer, TaskListener listener) {
    		LOGGER.info("launch method called for slave ");
            AzureComputer computer = (AzureComputer)_computer;
            PrintStream logger = listener.getLogger();
            
            final Connection bootstrapConn;
            final Connection conn;
            Connection cleanupConn = null; // java's code path analysis for final doesn't work that well.
            boolean successful = false;
            AzureSlave slave = computer.getNode();
            
            if (slave.getOsType().equalsIgnoreCase("windows")) {
            	// Any other better mechanism???
            	computer.setAcceptingTasks(true);
            	LOGGER.info("Slave "+slave.getOsType() + " is having Windows operating system");
            	return;
            }
            
            try {
            	 bootstrapConn = connectToSsh(computer, logger);
                 int bootstrapResult = bootstrap(bootstrapConn, computer, logger);
                 
                 if (bootstrapResult == FAILED) {
                 	LOGGER.info("bootstrapresult failed");
                     return; // bootstrap closed for us.
                 }
                 else if (bootstrapResult == SAMEUSER) {
                     cleanupConn = bootstrapConn; // take over the connection
                     LOGGER.info("take over connection");
                 }
                 
                 conn = cleanupConn;
                 
                 SCPClient scp = conn.createSCPClient();
                 String initScript = slave.getInitScript();

                 if(initScript!=null && initScript.trim().length()>0 && conn.exec("test -e ~/.hudson-run-init", logger) !=0) {
                     LOGGER.info("Executing init script");
                     scp.put(initScript.getBytes("UTF-8"),"init.sh","/tmp","0700");
                     Session sess = conn.openSession();
                     sess.requestDumbPTY(); // so that the remote side bundles stdout and stderr
                     sess.execCommand("/tmp/init.sh");

                     sess.getStdin().close();    // nothing to write here
                     sess.getStderr().close();   // we are not supposed to get anything from stderr
                     IOUtils.copy(sess.getStdout(),logger);

                     int exitStatus = waitCompletion(sess);
                     if (exitStatus!=0) {
                         LOGGER.info("init script failed: exit code="+exitStatus);
                         return;
                     }
                     sess.close();

                     // Needs a tty to run sudo.
                     sess = conn.openSession();
                     sess.requestDumbPTY(); // so that the remote side bundles stdout and stderr
                     sess.execCommand("touch ~/.hudson-run-init");
                     sess.close();
                 }
                 
                 LOGGER.info("Verifying that java exists");
                 if(conn.exec("java -fullversion", logger) !=0) {
                     LOGGER.info("Failed to find java , returning");
                     return;
                 }
                 LOGGER.info("Found java");
                 
                 LOGGER.info("Copying slave.jar");
                 scp.put(Hudson.getInstance().getJnlpJars("slave.jar").readFully(),
                         "slave.jar","/tmp");

                 String jvmopts = slave.getJvmOptions();
                 String launchString = "java " + (jvmopts != null ? jvmopts : "") + " -jar /tmp/slave.jar";
                 LOGGER.info("Launching slave agent: " + launchString);
                 final Session sess = conn.openSession();
                 sess.execCommand(launchString);
                 computer.setChannel(sess.getStdout(),sess.getStdin(),logger,new Listener() {
                     @Override
     				public void onClosed(Channel channel, IOException cause) {
                         sess.close();
                         conn.close();
                     }
                 });
                 
                 successful = true;


                 
            } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
                if(cleanupConn != null && !successful)
                    cleanupConn.close();
            }
            
    }

    private int bootstrap(Connection bootstrapConn, AzureComputer computer, PrintStream logger) throws IOException, InterruptedException {
        LOGGER.info("bootstrap()" );
        boolean closeBootstrap = true;
        try {
            int tries = 20;
            boolean isAuthenticated = false;
            AzureSlave instance = computer.getNode();
            String hostName = instance.getPublicDNSName();
            int sshPort = computer.getNode().getSshPort();
            LOGGER.info("Using hostName: " + hostName + "\n" + "sshPort " + "\n" + sshPort );
            while (tries-- > 0) {
                LOGGER.info("Authenticating as " + instance.getAdminUserName());
                LOGGER.info("Authenticating as " + instance.getAdminPassword());
                try {
                	isAuthenticated = bootstrapConn.authenticateWithPassword(instance.getAdminUserName(), instance.getAdminPassword());
                } catch (Exception e) {
                	LOGGER.severe("Authentication failed for " + instance.getAdminUserName() + " with error "+e);
                }
                if (isAuthenticated) {
                    break;
                }
                LOGGER.info("Authentication failed. Trying again...");
                Thread.sleep(1 * 60 * 1000);
            }
            if (!isAuthenticated) {
                LOGGER.info("Authentication failed");
                return FAILED;
            }
            closeBootstrap = false;
            return SAMEUSER;
        } finally {
            if (closeBootstrap)
                bootstrapConn.close();
        }
    }

    private Connection connectToSsh(AzureComputer computer, PrintStream logger) throws Exception {
    	LOGGER.info("Connecting via ssh");
        final long timeout = 20 * 60 * 1000;
        final long startTime = System.currentTimeMillis();
        while(true) {
            try {
                long waitTime = System.currentTimeMillis() - startTime;
                if ( waitTime > timeout )
                {
                    throw new Exception("Timed out after "+ (waitTime / 1000) + " seconds of waiting for ssh to become available. (maximum timeout configured is "+ (timeout / 1000) + ")" );
                }
                AzureSlave instance = computer.getNode();
                String host = instance.getPublicDNSName();
                
                
                int port = instance.getSshPort();
                LOGGER.info("Connecting to " + host + " on port " + port + ". ");
                Connection conn = new Connection(host, port);
                // currently OpenSolaris offers no way of verifying the host certificate, so just accept it blindly,
                // hoping that no man-in-the-middle attack is going on.
                conn.connect(new ServerHostKeyVerifier() {
                    public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
                        return true;
                    }
                });
                LOGGER.info("Connected via SSH.");
                return conn; // successfully connected
            } catch (IOException e) {
                // keep retrying until SSH comes up
                LOGGER.info("Waiting for SSH to come up. Sleeping 5.");
                Thread.sleep(1 * 60* 1000);
            }
        }
    }

    private int waitCompletion(Session session) throws InterruptedException {
        // I noticed that the exit status delivery often gets delayed. Wait up to 1 sec.
        for( int i=0; i<10; i++ ) {
            Integer r = session.getExitStatus();
            if(r!=null) return r;
            Thread.sleep(100);
        }
        return -1;
    }

    @Override
	public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
