package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.srvpri.SrvPriManager;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class SrvPriCreationStatusDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTable statusTable;
    //private JComboBox comboBoxCredFile;
//    private WideComboBox comboBoxCredFile;
    private JButton buttonCancel;
    private JList filesList;
//    private JTable filesTable;
    private List<String> authFilePathList =  new LinkedList<>();
    private int result = JOptionPane.CANCEL_OPTION;
    String destinationFolder;
    private Map<String, List<String> > tidSidsMap;

    private String selectedAuthFilePath;

    public String getSelectedAuthFilePath() {
        return selectedAuthFilePath;
    }


    public int getResult() {
        return result;
    }

    public List<String> srvPriCreationStatusDialog() {
        return authFilePathList;
    }

    DefaultTableModel statusTableModel = new DefaultTableModel() {
        final Class[] columnClass = new Class[] {
                String.class, String.class, String.class
        };
        @Override
        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return columnClass[columnIndex];
        }
    };

    DefaultListModel<String> filesListModel = new DefaultListModel<String>();

    public static SrvPriCreationStatusDialog go(Map<String, List<String> > tidSidsMap, String destinationFolder, Component parent) {
        SrvPriCreationStatusDialog d = new SrvPriCreationStatusDialog();
        d.tidSidsMap = tidSidsMap;
        d.destinationFolder = destinationFolder;
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d;
    }

    private SrvPriCreationStatusDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Service Principal Creation Status");

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
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }

            @Override
            public void windowOpened (WindowEvent e) {
                createServicePrincipalsAction();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        statusTableModel.addColumn("Step");
        statusTableModel.addColumn("Result");
        statusTableModel.addColumn("Details");
        statusTable.setModel(statusTableModel);
        statusTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
        TableColumn column = statusTable.getColumnModel().getColumn(0);
        column.setMinWidth(150);
        //column.setMaxWidth(400);
        column = statusTable.getColumnModel().getColumn(1);
        column.setMinWidth(100);
        column.setMaxWidth(100);
        column = statusTable.getColumnModel().getColumn(2);
        column.setMinWidth(50);

//        filesTableModel.addColumn("Created authentication file(s)");
//        filesTable.setModel(filesTableModel);
        filesList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        filesList.setLayoutOrientation(JList.VERTICAL);
        filesList.setVisibleRowCount(-1);
        filesList.setModel(filesListModel);
    }

    private void onOK() {
        // add your code here
//        String filepath = (String)comboBoxCredFile.getSelectedItem();
//        if (filepath != null) {
//            credFilePath = filepath;
//        }
        int rc = filesListModel.getSize();
        if (rc > 0) {
            selectedAuthFilePath = filesListModel.getElementAt(0);
        }

        int[] selcectedIndexes = filesList.getSelectedIndices();
        if (selcectedIndexes.length > 0) {
            selectedAuthFilePath =  filesListModel.getElementAt(selcectedIndexes[0]);
        }

        result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        // Do not use created file if any
        dispose();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        statusTable = new JBTable();
        statusTable.setRowSelectionAllowed(false);
        statusTable.setCellSelectionEnabled(false);
    }

    private void createServicePrincipalsAction() {
        ActionRunner task = new ActionRunner(ProjectManager.getInstance().getDefaultProject());
        task.queue();
    }

    private class ActionRunner extends Task.Modal implements IListener<Status> {
        //ProgressIndicator progressIndicator;
        public ActionRunner(Project project) {
            super(project, "Creating Service Principal...", true);
        }
        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            //this.progressIndicator = progressIndicator;
            progressIndicator.setIndeterminate(true);
            for (String tid : tidSidsMap.keySet()) {
                if (progressIndicator.isCanceled()) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            statusTableModel.addRow(new Object[] {"=== Canceled by user", null, null});                        }
                    }, ModalityState.any());
                    return;
                }
                List <String> sidList = tidSidsMap.get(tid);
                if (!sidList.isEmpty()) {
                    try {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                statusTableModel.addRow(new Object[] {"tenant ID: " + tid + " ===", null, null});
                            }
                        }, ModalityState.any());
                        Date now = new Date();
                        String suffix = new SimpleDateFormat("yyyyMMddHHmmss").format(now);;
                        final String authFilepath = SrvPriManager.createSp(tid, sidList, suffix, this, destinationFolder);
                        //Thread.sleep(5000);
                        //final String authFilepath = suffix + new Date().toString();
                        if (authFilepath != null) {
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    filesListModel.addElement(authFilepath);
                                    filesList.setSelectedIndex(0);
                                }
                            }, ModalityState.any());
                        }
                    } catch (Exception ex) {
                        // TODO: use logger
                        System.out.println("Creating Service Principal exception: " + ex.getMessage());
                    }
                }
            }
        }

        @Override
        public void listen(final Status status) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
//                    progressIndicator.setText(status.getAction());
                    statusTableModel.addRow(new Object[] {status.getAction(), status.getResult(), status.getDetails()});
                    statusTableModel.fireTableDataChanged();
                }
            }, ModalityState.any());
        }
    }
}

