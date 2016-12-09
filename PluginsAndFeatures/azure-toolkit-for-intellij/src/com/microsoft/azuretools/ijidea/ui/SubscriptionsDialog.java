package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class SubscriptionsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;
    private List<SubscriptionDetail> sdl;
    private int result = JOptionPane.CANCEL_OPTION;


    private DefaultTableModel model = new DefaultTableModel() {
        final Class[] columnClass = new Class[] {
                Boolean.class, String.class, String.class
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

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    public int getResult() {
        return result;
    }

    public static SubscriptionsDialog go(List<SubscriptionDetail> sdl, Component parent) throws Exception {
        SubscriptionsDialog d = new SubscriptionsDialog();
        d.sdl = sdl;
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d;
    }

    private SubscriptionsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Select Subscriptions");

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
                setSubscriptions();
            }
        });

        model.addColumn("");
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");

        table.setModel(model);

        TableColumn column = table.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);

        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

    }

    private void setSubscriptions() {
        for (SubscriptionDetail sd : sdl) {
            model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
            model.fireTableDataChanged();
        }
    }

    private void onOK() {
        int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            if (!selected) unselectedCount++;
        }

        if (unselectedCount == rc) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least one subscription",
                    "Subscription dialog info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            this.sdl.get(ri).setSelected(selected);
        }

        result = JOptionPane.OK_OPTION;

        dispose();
    }

    private void onCancel() {
        dispose();
    }

//    private void deleteTableData() {
//        while (model.getRowCount() != 0) model.removeRow(0);
//    }


//    private void loadSubscriptions() {
////        LoadSubscriptionTask task = new LoadSubscriptionTask(null, this.sm);
////        task.queue();
//
//        ProgressManager.getInstance().run(new Task.Modal(null, "Loading Subscriptions...", true) {
//            @Override
//            public void run(ProgressIndicator progressIndicator) {
//                try {
//                    progressIndicator.setIndeterminate(true);
//
//                    sdl = sm.getSubscriptionDetails();
//                    ApplicationManager.getApplication().invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            deleteTableData();
//                            for (SubscriptionDetail sd : sdl) {
//                                model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
//                                model.fireTableDataChanged();
//                            }
//                        }
//                    }, ModalityState.any());
//
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
//
//    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        table = new JBTable();
    }
/*
    private class LoadSubscriptionTask extends Task.Modal  {
        SubscriptionManager sm;

        public LoadSubscriptionTask(@Nullable Project project, SubscriptionManager sm) {
            super(project, "Loading Subscriptions", true);
            this.sm = sm;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            try {
                progressIndicator.setIndeterminate(true);

                sdl = sm.getSubscriptionDetails();
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        deleteTableDate();
                        for (SubscriptionDetail sd : sdl) {
                            statusTableModel.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
                        }
                    }
                }, ModalityState.any());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
*/
}
