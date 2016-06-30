/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azure.hdinsight.projects;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.microsoft.azure.hdinsight.Activator;
import com.microsoft.azure.hdinsight.common.CommonConst;
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
				SparkLibraryInfoForEclipse info = new SparkLibraryInfoForEclipse(cachedLibraryPath.get(i));
				comboBox.setData(cachedLibraryPath.get(i), info);
			} catch (Exception e) {
				// do nothing if we can not get the library info
			}
		}
		if (cachedLibraryPath.size() > 0) {
			comboBox.select(0);
		}
		button = new Button(composite, SWT.PUSH);
		button.setText("Select...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog dialog = new FileDialog(SparkLibraryOptionsPanel.this.getShell());
				String[] extensions = { "*.jar;*.JAR;*.zip;*.ZIP", };
				dialog.setFilterExtensions(extensions);
				String file = dialog.open();
				if (file != null) {
					try {
						comboBox.add(file);
						comboBox.setData(file, new SparkLibraryInfoForEclipse(file));
						comboBox.select(comboBox.getItems().length - 1);
						DefaultLoader.getIdeHelper().setProperties(CommonConst.CACHED_SPARK_SDK_PATHS, comboBox.getItems());
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
