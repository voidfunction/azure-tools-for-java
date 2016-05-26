package com.microsoft.azure.hdinsight.projects;

import java.net.URL;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.hdinsight.Activator;

public class SparkLibraryOptionsPanel extends Composite {
	private Combo comboBox;
	private Button button;
	
	public SparkLibraryOptionsPanel(Composite parent, int style) {
		super(parent, style);
//		Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        setLayout(gridLayout);
        setLayoutData(gridData);

        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
		comboBox = new Combo(this, SWT.READ_ONLY);
		comboBox.setLayoutData(gridData);
		button = new Button(this, SWT.PUSH);
		button.setText("Select...");
		
		Link tipLabel = new Link(this, SWT.FILL);
		tipLabel.setText("You can either download Spark library from <a href=\"http://go.microsoft.com/fwlink/?LinkID=723585&clcid=0x409\">here</a> or add Apache Spark packages from Maven repository in the project manually.");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		tipLabel.setLayoutData(gridData);
		tipLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
            	try {
            		PlatformUI.getWorkbench().getBrowserSupport().
            		getExternalBrowser().openURL(new URL(event.text));
            	}
            	catch (Exception ex) {
            		/*
            		 * only logging the error in log file
            		 * not showing anything to end user.
            		 */
            		Activator.getDefault().log("Error opening link", ex);
            	}
            }
        });
	}
	
	private void doCreate() {
//		VirtualFile root = LocalFileSystem.getInstance().findFileByPath(PluginUtil.getPluginRootDirectory());
//        NewLibraryConfiguration libraryConfiguration = this.myLibraryDescription.createNewLibrary(this.button, root);
//
//        if (libraryConfiguration != null) {
//            NewLibraryEditor libraryEditor = new NewLibraryEditor(libraryConfiguration.getLibraryType(), libraryConfiguration.getProperties());
//            libraryEditor.setName(suggestUniqueLibraryName(libraryConfiguration.getDefaultLibraryName()));
//            libraryConfiguration.addRoots(libraryEditor);
//
//            try {
//                SparkLibraryInfo info = new SparkLibraryInfo(myLibraryDescription.getLocalPath());
//                if(info != null) {
//                    libraryEditorMap.put(info, libraryEditor);
//                }
//            } catch (Exception e) {
//                //do nothing if we can not get the library info
//            }
//
//            if (this.comboBox.getItemAt(0) == null) {
//                this.comboBox.remove(0);
//            }
//
//            this.comboBox.addItem(libraryEditor);
//            this.comboBox.setSelectedItem(libraryEditor);
	}

}
