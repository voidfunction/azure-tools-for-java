/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.core.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.osgi.service.log.LogService;

import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.utils.ProgressDialog;

public class SubscriptionsDialog extends TitleAreaDialog {

    @Inject
    private static LogService LOGGER;
    
    private Table table;
    
    private SubscriptionManager subscriptionManager;
    private List<SubscriptionDetail> sdl;

//    public List<SubscriptionDetail> getSubscriptionDetails() {
//        return sdl;
//    }
    
    /**
     * Create the dialog.
     * @param parentShell
     */
    private SubscriptionsDialog(Shell parentShell, SubscriptionManager subscriptionManage) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
        this.subscriptionManager = subscriptionManage;
    }    
    
    public static SubscriptionsDialog go(Shell parentShell, SubscriptionManager subscriptionManager) {
        SubscriptionsDialog d = new SubscriptionsDialog(parentShell, subscriptionManager);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Select subscription(s) you want to use.");
        setTitle("Your Subscriptions");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        table = new Table(container, SWT.BORDER | SWT.CHECK);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(250);
        tblclmnNewColumn.setText("Subsription Name");
        
        TableColumn tblclmnNewColumn_1 = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn_1.setWidth(300);
        tblclmnNewColumn_1.setText("Subscription ID");
        
        Button btnRefresh = new Button(container, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshSubscriptions();
            }
        });
        GridData gd_btnRefresh = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        gd_btnRefresh.widthHint = 78;
        btnRefresh.setLayoutData(gd_btnRefresh);
        btnRefresh.setText("Refresh");

        return area;
    }
    
    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              System.out.println("refreshSubscriptionsAsync");
              refreshSubscriptionsAsync();
              setSubscriptionDetails();
            }
          });
    } 

    public void refreshSubscriptionsAsync() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Reading subscriptions...", IProgressMonitor.UNKNOWN);
                    try {
                        subscriptionManager.getSubscriptionDetails();
                    } catch (Exception e) {
                        LOGGER.log(LogService.LOG_ERROR,"refreshSubscriptionsAsync::ProgressDialog", e);
                        e.printStackTrace();
                    }
                    monitor.done();
                }
            });
        } catch (Exception e) {
            LOGGER.log(LogService.LOG_ERROR,"refreshSubscriptionsAsync", e);
            e.printStackTrace();
        }
    }
    
    private void setSubscriptionDetails() {
        try {
            sdl = subscriptionManager.getSubscriptionDetails();
            for (SubscriptionDetail sd : sdl) {
                TableItem item = new TableItem(table, SWT.NULL);
                item.setText(new String[] {sd.getSubscriptionName(), sd.getSubscriptionId()});
                item.setChecked(sd.isSelected());
            }
        } catch (Exception e) {
            LOGGER.log(LogService.LOG_ERROR,"subscriptionManager.getSubscriptionDetails", e);
            e.printStackTrace();
        }
    }
    
    private void refreshSubscriptions() {
        try {
            System.out.println("refreshSubscriptions");
            table.removeAll();
            subscriptionManager.cleanSubscriptions();
            refreshSubscriptionsAsync();
            setSubscriptionDetails();
            subscriptionManager.setSubscriptionDetails(sdl);
        } catch (Exception e) {
            LOGGER.log(LogService.LOG_ERROR,"subscriptionManager.setSubscriptionDetails", e);
            e.printStackTrace();
        }
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Select");
    }
    
    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(668, 410);
    }
    
    @Override
    public void okPressed() {
        TableItem[] tia = table.getItems();
        int chekedCount = 0;
        for (TableItem ti : tia) {
            if (ti.getChecked()) {
                chekedCount++;
            }
        }
        
        if (chekedCount == 0) {
            this.setErrorMessage("Please select at least one subscription");
            return;
        }        
        
        for (int i = 0; i < tia.length; ++i) {
            this.sdl.get(i).setSelected(tia[i].getChecked());
        }
        
        try {
            subscriptionManager.setSubscriptionDetails(sdl);
        } catch (Exception e) {
            LOGGER.log(LogService.LOG_ERROR,"subscriptionManager.setSubscriptionDetails", e);
            e.printStackTrace();
        }
        
        super.okPressed();
    }
}
