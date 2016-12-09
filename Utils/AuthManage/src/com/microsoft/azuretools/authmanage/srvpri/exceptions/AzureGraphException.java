package com.microsoft.azuretools.authmanage.srvpri.exceptions;

import com.microsoft.azuretools.authmanage.srvpri.entities.AzureErrorGraph;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * Created by vlashch on 10/25/16.
 */

public class AzureGraphException extends AzureException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AzureErrorGraph azureError;

    public AzureGraphException(String json){
        super(json);
        try {
            ObjectMapper mapper = new ObjectMapper();
            azureError = mapper.readValue(json, AzureErrorGraph.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCode() {
        String desc = "";
        if (azureError != null) {
            desc = azureError.error.code;
        }
        return desc;

    }

    @Override
    public String getDescription() {
        String desc = "";
        if (azureError != null) {
            String detailes = "";
            if (azureError.error.values != null) {
                for (AzureErrorGraph.Error.Value v : azureError.error.values) {
                    detailes += v.item + ": " + v.value + ";";
                }
            }
            if (detailes.isEmpty()) {
                desc = azureError.error.message.value;
            } else {
                desc = azureError.error.message.value + " (" + detailes + ")";
            }
        }
        return desc;
    }
}

