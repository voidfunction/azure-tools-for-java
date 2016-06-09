package com.microsoft.azure.hdinsight.projects;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.microsoft.azure.hdinsight.Activator;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class SparkLibraryOptionsPanel extends Composite {
	private Combo comboBox;
	private Button button;
	
	private List<String> cachedLibraryPath = new ArrayList<>();
	
	public SparkLibraryOptionsPanel(Composite parent, int style) {
		super(parent, style);
//		Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        setLayout(gridLayout);
        setLayoutData(gridData);
        Label lblProjName = new Label(this, SWT.LEFT | SWT.TOP);
        lblProjName.setText("Spark SDK:");
//		Composite composite = new Composite(parent, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		comboBox = new Combo(composite, SWT.READ_ONLY);
		comboBox.setLayoutData(gridData);

		String[] tmp = DefaultLoader.getIdeHelper().getProperties(CommonConst.CACHED_SPARK_SDK_PATHS);
		if (tmp != null) {
            cachedLibraryPath.addAll(Arrays.asList(tmp));
        }
		for (int i = 0; i < cachedLibraryPath.size(); ++i) {

			comboBox.add(cachedLibraryPath.get(i));
			try {
				SparkLibraryInfo info = new SparkLibraryInfo(cachedLibraryPath.get(i));
				comboBox.setData(cachedLibraryPath.get(i), info);
			} catch (Exception e) {
				// do nothing if we can not get the library info
			}

		}
		button = new Button(composite, SWT.PUSH);
		button.setText("Select...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog dialog = new FileDialog(SparkLibraryOptionsPanel.this.getShell());
				String[] extensions = { "*.jar", "*.JAR" };
				dialog.setFilterExtensions(extensions);
				String file = dialog.open();
				if (file != null) {
					try {
						comboBox.add(file);
						comboBox.setData(file, new SparkLibraryInfo(file));
						comboBox.select(comboBox.getItems().length - 1);
					} catch (Exception e) {
						Activator.getDefault().log("Error adding Spark library", e);
					}
				}
			}
		});

		Link tipLabel = new Link(this, SWT.FILL);
		tipLabel.setText(
				"You can either download Spark library from <a href=\"http://go.microsoft.com/fwlink/?LinkID=723585&clcid=0x409\">here</a> or add Apache Spark packages from Maven repository in the project manually.");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		tipLabel.setLayoutData(gridData);
		tipLabel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
				} catch (Exception ex) {
					/*
					 * only logging the error in log file not showing anything
					 * to end user.
					 */
					Activator.getDefault().log("Error opening link", ex);
				}
			}
		});
	}	
        
	public String getSparkLibraryPath() {
		return comboBox.getText();
	}
}
