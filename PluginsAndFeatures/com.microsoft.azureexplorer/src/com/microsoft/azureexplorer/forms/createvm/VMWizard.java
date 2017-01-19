package com.microsoft.azureexplorer.forms.createvm;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import com.microsoft.tooling.msservices.model.Subscription;
import com.microsoft.tooling.msservices.model.vm.VirtualMachineSize;

public abstract class VMWizard extends Wizard {

	protected Subscription subscription;
	protected String name;
	protected String userName;
	protected String password;
	protected String certificate;
	protected String subnet;
	protected VirtualMachineSize size;

	public Subscription getSubscription() {
	    return subscription;
	}

	public void setSubscription(Subscription subscription) {
	    this.subscription = subscription;
	}

	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	public String getUserName() {
	    return userName;
	}

	public void setUserName(String userName) {
	    this.userName = userName;
	}

	public String getPassword() {
	    return password;
	}

	public void setPassword(String password) {
	    this.password = password;
	}

	public String getCertificate() {
	    return certificate;
	}

	public void setCertificate(String certificate) {
	    this.certificate = certificate;
	}

	public String getSubnet() {
	    return subnet;
	}

	public void setSubnet(String subnet) {
	    this.subnet = subnet;
	}

	public Browser createImageDescriptor(Composite container) {
	    GridData gridData = new GridData();
	    gridData.horizontalAlignment = SWT.FILL;
	    gridData.verticalAlignment = SWT.FILL;
	    gridData.grabExcessVerticalSpace = true;
	    gridData.grabExcessHorizontalSpace = true;
	    Browser imageDescription = new Browser(container, SWT.NONE);
	    imageDescription.setLayoutData(gridData);
	    return imageDescription;
	}

	public List configStepList(Composite parent, final int step) {
	        GridData gridData = new GridData();
	        gridData.widthHint = 100;
	//
	        gridData.verticalAlignment = GridData.BEGINNING;
	        gridData.grabExcessVerticalSpace = true;
	        List createVmStepsList = new List(parent, SWT.BORDER);
	        createVmStepsList.setItems(getStepTitleList());
	        createVmStepsList.setSelection(step);
	        createVmStepsList.setLayoutData(gridData);
	        createVmStepsList.addSelectionListener(new SelectionAdapter() {
	            @Override
	            public void widgetSelected(SelectionEvent e) {
	                List l = (List) e.widget;
	                l.setSelection(step);
	            }
	        });
	//        createVmStepsList.setEnabled(false);
	
	
	//        jList.setBorder(new EmptyBorder(10, 0, 10, 0));
	
	//        jList.setCellRenderer(new DefaultListCellRenderer() {
	//            @Override
		// public Component getListCellRendererComponent(JList jList, Object o,
		// int i, boolean b, boolean b1) {
		// return super.getListCellRendererComponent(jList, " " + o.toString(),
		// i, b, b1);
		// }
		// });
		//
		// for (MouseListener mouseListener : jList.getMouseListeners()) {
		// jList.removeMouseListener(mouseListener);
		// }
		//
		// for (MouseMotionListener mouseMotionListener :
		// jList.getMouseMotionListeners()) {
		// jList.removeMouseMotionListener(mouseMotionListener);
		// }
		return createVmStepsList;
	}

	public abstract String[] getStepTitleList();

	public VirtualMachineSize getSize() {
	    return size;
	}

	public void setSize(VirtualMachineSize size) {
	    this.size = size;
	}
}
