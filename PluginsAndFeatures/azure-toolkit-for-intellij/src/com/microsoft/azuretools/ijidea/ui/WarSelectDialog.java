/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.packaging.artifacts.Artifact;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class WarSelectDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;

    private List<Artifact> artifactList;
    private Artifact selectedArtifact;

    public Artifact getSelectedArtifact() {
        return selectedArtifact;
    }

    private int result = JOptionPane.CANCEL_OPTION;

    public static WarSelectDialog go(List<Artifact> artifactList, Component parent) {
        WarSelectDialog d = new WarSelectDialog(artifactList);
        d.artifactList = artifactList;
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        if (d.result == JOptionPane.OK_OPTION)
            return d;
        return null;
    }

    protected WarSelectDialog(List<Artifact> artifactList) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Select WAR Artifact");

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.artifactList = artifactList;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Name");
        tableModel.addColumn("Path");
        for (Artifact artifact : artifactList) {
            tableModel.addRow(new String[] {artifact.getName(), artifact.getOutputFilePath()});
        }
        table.setModel(tableModel);
    }

    private void onOK() {
        DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
        int i = table.getSelectedRow();
        if (i < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an artifact",
                    "Select artifact info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        selectedArtifact = artifactList.get(i);

        result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
