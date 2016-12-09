package com.microsoft.azuretools.authmanage.interact;

import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;

/**
 * Created by shch on 10/8/2016.
 */
public interface ISelectAuthMethod {
    void init(AuthMethodDetails details);
    AuthMethodDetails update();
}
