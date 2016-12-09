package com.microsoft.azuretools.authmanage.interact;

import com.microsoft.azuretools.adauth.IWebUi;

/**
 * Created by shch on 10/4/2016.
 */
public interface IUIFactory {
//    ISelectAuthMethod getAuthMethodDialog();
    INotification getNotificationWindow();
    IWebUi getWebUi();
}
