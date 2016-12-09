package com.microsoft.azuretools.authmanage.srvpri.exceptions;

import com.microsoft.azuretools.authmanage.srvpri.entities.AzureErrorArm;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * Created by vlashch on 10/25/16.
 */
public class AzureArmException extends AzureException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AzureErrorArm azureError;

    public AzureArmException(String json){
        super(json);
        try {
            ObjectMapper mapper = new ObjectMapper();
            azureError = mapper.readValue(json, AzureErrorArm.class);
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
            desc = azureError.error.message;
        }
        return desc;
    }
}
