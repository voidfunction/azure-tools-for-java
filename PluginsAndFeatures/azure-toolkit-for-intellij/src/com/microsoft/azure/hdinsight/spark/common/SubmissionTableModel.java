package com.microsoft.azure.hdinsight.spark.common;

import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.tooling.msservices.helpers.StringHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SubmissionTableModel extends InteractiveTableModel{
    private List<SparkSubmissionJobConfigCheckResult> checkResults;

    public SubmissionTableModel(String[] columnNames) {
        super(columnNames);
    }

    public SparkSubmissionJobConfigCheckResult getFirstCheckResults() {
        return checkResults == null || checkResults.size() == 0 ? null : checkResults.get(0);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        super.setValueAt(aValue, rowIndex, columnIndex);
        checkParameter();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 ? false : super.isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public void addRow(String key, Object value) {
        super.addRow(key, value);
        checkParameter();
    }

    @NotNull
    public Map<String, Object> getJobConfigMap() {
        Map<String, Object> jobConfigMap = new HashMap<>();
        for (int index = 0; index < this.getRowCount(); index++) {
            if (!StringHelper.isNullOrWhiteSpace((String) this.getValueAt(index, 0))) {
                jobConfigMap.put((String) this.getValueAt(index, 0), this.getValueAt(index, 1));
            }
        }

        return jobConfigMap;
    }

    private void checkParameter() {
        final List<SparkSubmissionJobConfigCheckResult> resultList = SparkSubmissionParameter.checkJobConfigMap(getJobConfigMap());

        Collections.sort(resultList, new Comparator<SparkSubmissionJobConfigCheckResult>() {
            @Override
            public int compare(SparkSubmissionJobConfigCheckResult o1, SparkSubmissionJobConfigCheckResult o2) {
                if (o1.getStatus() == o2.getStatus()) {
                    return 0;
                } else if (o1.getStatus() == SparkSubmissionJobConfigCheckStatus.Warning && o2.getStatus() == SparkSubmissionJobConfigCheckStatus.Error) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        checkResults = resultList;
    }

}
