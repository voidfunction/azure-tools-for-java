package com.microsoft.azure.hdinsight;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class SparkSubmissionToolWindowView extends ViewPart {

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		parent.setLayout(layout);

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
