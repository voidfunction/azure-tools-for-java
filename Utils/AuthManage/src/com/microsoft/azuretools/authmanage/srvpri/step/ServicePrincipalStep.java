package com.microsoft.azuretools.authmanage.srvpri.step;

import com.microsoft.azuretools.authmanage.srvpri.entities.ServicePrincipal;
import com.microsoft.azuretools.authmanage.srvpri.entities.ServicePrincipalRet;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.rest.GraphRestHelper;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * Created by shch on 8/21/2016.
 */
public class ServicePrincipalStep implements IStep {
    private GraphRestHelper graphRestHelper;
    private Reporter<String> reporter;

    @Override
    public void execute(Map<String, Object> params) throws Throwable {
        //System.out.println("ServicePrincipalStep execute...");

        UUID appId = (UUID)params.get("appId");
        //UUID tenantId = (UUID) params.get("tenantId");
        String tenantId = CommonParams.getTenantId();
        graphRestHelper = new GraphRestHelper(tenantId);

        reporter = CommonParams.getReporter();
        reporter.report("appId: " + appId);


        ServicePrincipalRet sp = createAadServicePrincipal(appId);
        params.put("spObjectId", sp.objectId);
        Thread.sleep(1000);
        params.put("status", "sp");
        CommonParams.getStatusReporter().report(new Status(
                getName(),
                Status.Result.SUCCESSFUL,
                String.format("spObjectId: %s", sp.objectId)
        ));

    }

    @Override
    public void rollback(Map<String, Object> params) throws Exception {
        //System.out.println("ServicePrincipalStep rollback...");
        Object spObjectId = params.get("spObjectId");
        if(spObjectId != null)
            destroyAadServicePrincipal((UUID)spObjectId);
    }

    @Override
    public String getName() {
        return "Creating service principal for application";
    }

    // helpers

    // Create Service Principals
    private ServicePrincipalRet createAadServicePrincipal(UUID appId) throws Throwable {
    /*
        POST https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/servicePrincipals?api-version=1.6-internal HTTP/1.1

        {
          "appId": "425817b6-1cd8-48ea-9cd7-1b9472912bee",
          "accountEnabled": true
        }

         */

        ServicePrincipal sp = new ServicePrincipal();
        sp.accountEnabled = true;
        sp.appId = appId;

        ObjectMapper mapper = new ObjectMapper();
        String spJson = mapper.writeValueAsString(sp);

        String resp = graphRestHelper.doPost("servicePrincipals", null, spJson);
        ServicePrincipalRet spr = mapper.readValue(resp, ServicePrincipalRet.class);
        return spr;
    }

    private void destroyAadServicePrincipal(UUID objectId) throws Exception {
        // DELETE https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/servicePrincipals/f144ba9d-f4af-48b8-a992-3e328f2710b9?api-version=1.6-internal

        @SuppressWarnings("unused")
		String resp = graphRestHelper.doDelete("servicePrincipals/" + objectId.toString(), null, null);
    }

        @SuppressWarnings("unused")
		private static ServicePrincipalRet getAadServicePrincipal(UUID objectId) throws Exception {
        // GET https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/servicePrincipals/f144ba9d-f4af-48b8-a992-3e328f2710b9?api-version=1.6-internal
        return null;
    }

}
