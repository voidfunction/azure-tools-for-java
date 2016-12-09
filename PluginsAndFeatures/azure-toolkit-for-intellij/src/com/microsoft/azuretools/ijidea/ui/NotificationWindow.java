package com.microsoft.azuretools.ijidea.ui;

import com.microsoft.azuretools.authmanage.interact.INotification;

import javax.swing.*;

/**
 * Created by shch on 10/12/2016.
 */
public class NotificationWindow implements INotification {
    @Override
    public void deliver(String subject, String message) {
        JPanel panel = new JPanel();
        JOptionPane.showMessageDialog(panel,
                message,
                subject,
                JOptionPane.INFORMATION_MESSAGE);
    }
}
