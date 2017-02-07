/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.authmanage.srvpri;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.microsoft.azuretools.authmanage.srvpri.entities.SrvPriData;
import com.microsoft.azuretools.authmanage.srvpri.report.FileListener;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.step.ApplicationStep;
import com.microsoft.azuretools.authmanage.srvpri.step.CommonParams;
import com.microsoft.azuretools.authmanage.srvpri.step.RoleAssignmentStep;
import com.microsoft.azuretools.authmanage.srvpri.step.ServicePrincipalStep;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.authmanage.srvpri.step.StepManager;

/**
 * Created by vlashch on 8/16/16.
 */


public class SrvPriManager {
    // relationship;
    // app - sp (1-1)
    // sp - role (1-many)


    public static String createSp(String tenantId, List<String> subscriptionIds, String suffix, IListener<Status> statusListener, String destinationFolder) throws Exception {

        System.out.print(tenantId +": [");
        for (String sid : subscriptionIds) {
            System.out.print(sid + ", ");
        }
        System.out.println("]");

        Path spDirPath = Paths.get(destinationFolder);
        if (spDirPath == null) {
            String baseDir = System.getProperty("user.home");
            String dirName = "MSAzureAuhtFiles";
            spDirPath = Paths.get(baseDir, dirName);
            if (!Files.exists(spDirPath)) {
                Files.createDirectory(spDirPath);
            }
        }

        Reporter<Status> statusReporter = new Reporter<Status>();
        statusReporter.addListener(statusListener);
        CommonParams.setStatusReporter(statusReporter);

        // generate a password
        String password = UUID.randomUUID().toString();

        CommonParams.setSubscriptionIdList(subscriptionIds);
        CommonParams.setResultSubscriptionIdList(new LinkedList<>());
        CommonParams.setTenantId(tenantId);
        String filename = tenantId + "_" + suffix;
        //String filename = tenantId + "_" + subscriptionIds.get(0) + "_" + suffix;
        //String filename = subscriptionIds.get(0) + "_" + suffix;
        String spFilename = "sp-" + filename + ".azureauth";
        String reportFilename = "report-" + filename + ".txt";

        Reporter<String> reporter = new Reporter<String>();
        reporter.addListener(new FileListener(reportFilename, spDirPath.toString()));
//        reporter.addConsoleLister();
        CommonParams.setReporter(reporter);

        StepManager sm = new StepManager();
        sm.getParamMap().put("displayName", "AzureTools4j-" + suffix);
        sm.getParamMap().put("homePage", "https://github.com/Microsoft/azure-tools-for-java");
        // MUST be unique
        sm.getParamMap().put("identifierUri", "file://" + spFilename);
        sm.getParamMap().put("password", password);
        sm.getParamMap().put("status", "standby");

        sm.add(new ApplicationStep());
        sm.add(new ServicePrincipalStep());
        sm.add(new RoleAssignmentStep());

        reporter.report(String.format("== Starting for tenantId: '%s'", tenantId));

        sm.execute();

        String overallStatusText = "=== Overall status";
        // create a file artifact
        if(sm.getParamMap().get("status").toString().equals("done")) {
            Path filePath = Paths.get(spDirPath.toString(), spFilename);;

            createArtifact(
                    filePath.toString(),
                    (UUID) sm.getParamMap().get("appId"),
                    password
            );

            String successSidsResult = String.format("Succeeded for %d of %d subscriptions. ",
                    CommonParams.getResultSubscriptionIdList().size(),
                    CommonParams.getSubscriptionIdList().size());

            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.SUCCESSFUL,
                    successSidsResult
            ));

            reporter.report(String.format("Authentication file created, path: %s", filePath.toString()));
            return filePath.toString();

        } else {
            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.FAILED,
                    "Can't create a service principal for the subscription"
            ));
        }
        
        return null;
    }

    private static void createArtifact(String filepath, UUID appId, String appPassword) {

        Properties prop = new Properties();
        OutputStream output = null;

        try {

            // TODO.shch: best file name?
            output = new FileOutputStream(filepath);

            // set the properties value
            prop.setProperty("tenant", CommonParams.getTenantId());
            int i = 0;
            for (String subscriptionId : CommonParams.getSubscriptionIdList()) {
                if (i==0) {
                    prop.setProperty("subscription", subscriptionId);
                } else {
                    prop.setProperty("subscription"+i, subscriptionId);
                }
                i++;
            }
            prop.setProperty("client", appId.toString());
            prop.setProperty("key", appPassword);
            prop.setProperty("managementURI", "https://management.core.windows.net/");
            prop.setProperty("baseURL", "https://management.azure.com/");
            prop.setProperty("authURL", "https://login.windows.net/");

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    SrvPriData collectSrvPriData(UUID appId) {
        return new SrvPriData();
    }

// ======== Private helpers ===============================


//    private static void printSrvPri(String id) throws Throwable {
//        String resp = GraphRestHelper.get( "servicePrincipals/" + id, null);
//        if(resp != null) {
//            ObjectMapper mapper = new ObjectMapper();
//            ServicePrincipalRet sp = mapper.readValue(resp, ServicePrincipalRet.class);
//        }
//    }
}
