package com.microsoft.azuretools.authmanage.interact;

/**
 * Created by shch on 10/12/2016.
 */
public interface INotification {
    void deliver(String subject, String message);
}
