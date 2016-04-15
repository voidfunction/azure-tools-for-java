package com.microsoft.azure.hdinsight.spark.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Created by guizha on 8/25/2015.
 */
public class InteractiveRenderer extends DefaultTableCellRenderer {
    private int interactiveColumn;

    public InteractiveRenderer(int interactiveColumn) {
        this.interactiveColumn = interactiveColumn;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component =  super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == interactiveColumn && hasFocus) {

            InteractiveTableModel tableModel = (InteractiveTableModel)table.getModel();
            if(tableModel != null)

            if ((tableModel.getRowCount() - 1) == row && !tableModel.hasEmptyRow())
            {
               tableModel.addEmptyRow();
            }

            int lastRow = tableModel.getRowCount();
            if (row == lastRow - 1) {
                table.setRowSelectionInterval(lastRow - 1, lastRow - 1);
            } else {
                table.setRowSelectionInterval(row + 1, row + 1);
            }

            table.setColumnSelectionInterval(0, 0);
        }

        return component;
    }
}
