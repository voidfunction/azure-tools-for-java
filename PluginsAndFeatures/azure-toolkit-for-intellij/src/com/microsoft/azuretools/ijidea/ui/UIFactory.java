package com.microsoft.azuretools.ijidea.ui;

import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;

/**
 * Created by shch on 10/4/2016.
 */
public class UIFactory implements IUIFactory{

//    @Override
//    public ISelectAuthMethod getAuthMethodDialog() {
//        return new AuthMethodDialog();
//    }

    @Override
    public INotification getNotificationWindow() {
        return new NotificationWindow();
    }

    @Override
    public IWebUi getWebUi() { return new WebUi(); }
}
