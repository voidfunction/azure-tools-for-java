package com.microsoft.azuretools.authmanage.srvpri.report;

/**
 * Created by vlashch on 10/20/16.
 */
public interface IListener<T> {
    void listen(T message);
}
