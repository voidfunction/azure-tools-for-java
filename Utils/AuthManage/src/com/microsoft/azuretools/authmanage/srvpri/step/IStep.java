package com.microsoft.azuretools.authmanage.srvpri.step;

import java.util.Map;

/**
 * Created by shch on 8/20/2016.
 */
public interface IStep {
    void execute(Map<String, Object> params) throws Throwable;
    void rollback(Map<String, Object> params) throws Throwable;
    String getName();
}
