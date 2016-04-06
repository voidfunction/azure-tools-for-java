package com.microsoft.azure.hdinsight.serverexplore;

public interface AzureManagerRequestCallback<T> {
    T execute() throws Exception;
}
