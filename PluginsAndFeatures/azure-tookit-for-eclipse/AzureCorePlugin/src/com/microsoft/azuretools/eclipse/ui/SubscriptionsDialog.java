package com.microsoft.azuretools.eclipse.ui;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class SubscriptionsDialog extends TitleAreaDialog {
    private Table table;
    
    private List<SubscriptionDetail> sdl;

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }
    
    /**
     * Create the dialog.
     * @param parentShell
     */
    private SubscriptionsDialog(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    }    
    
    public static SubscriptionsDialog go(Shell parentShell, List<SubscriptionDetail> sdl) {
        SubscriptionsDialog d = new SubscriptionsDialog(parentShell);
        d.sdl = sdl;
        d.create();

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
        container.setLayout(new FillLayout(SWT.HORIZONTAL));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        table = new Table(container, SWT.BORDER | SWT.CHECK);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(250);
        tblclmnNewColumn.setText("Subsription Name");
        
        TableColumn tblclmnNewColumn_1 = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn_1.setWidth(300);
        tblclmnNewColumn_1.setText("Subscription ID");
        
        for (SubscriptionDetail sd : sdl) {
            TableItem item = new TableItem(table, SWT.NULL);
            item.setText(new String[] {sd.getSubscriptionName(), sd.getSubscriptionId()});
            item.setChecked(sd.isSelected());
        }    

        return area;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button btnOk = createButton(parent, IDialogConstants.FINISH_ID, IDialogConstants.OK_LABEL, true);
        btnOk.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onOk();
            }
        });
        Button btnCancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(582, 367);
    }
    
    private void onOk() {
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
        
        super.okPressed();
    }
}
