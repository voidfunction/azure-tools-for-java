package com.microsoft.azuretools.sdkmanage;

import com.microsoft.azure.management.Azure;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vlashch on 1/27/17.
 */
public abstract class AzureManagerBase implements AzureManager {
    protected Map<String, Azure> sidToAzureMap = new HashMap<>();

}
