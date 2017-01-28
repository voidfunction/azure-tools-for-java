package com.microsoft.azuretools.ijidea.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ErrorWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextPane textPane;

    public static void show(String message, Component parent) {
        ErrorWindow w = new ErrorWindow(message, null);
        w.pack();
        w.setLocationRelativeTo(parent);
        w.setVisible(true);
    }

    public static void show(String message, String title, Component parent) {
        ErrorWindow w = new ErrorWindow(message, title);
        w.pack();
        w.setLocationRelativeTo(parent);
        w.setVisible(true);
    }

    public ErrorWindow(String message, String title) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        if (title != null && !title.isEmpty()) {
            setTitle(title);
        } else {
            setTitle("Error Notification");
        }

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        textPane.setText(message);
    }

    private void onOK() {
        // add your code here
        dispose();
    }
}
