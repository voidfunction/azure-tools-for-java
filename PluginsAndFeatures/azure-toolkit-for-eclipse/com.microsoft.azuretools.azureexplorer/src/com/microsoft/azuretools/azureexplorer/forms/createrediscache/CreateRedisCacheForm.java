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
package com.microsoft.azuretools.azureexplorer.forms.createrediscache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.ParallelExecutor;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.ResourceGroup;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

public class CreateRedisCacheForm extends TitleAreaDialog {

    private static ILog LOG = Activator.getDefault().getLog();
    protected final AzureManager azureManager;
    protected List<SubscriptionDetail> allSubs;
    protected Set<String> allResGrpsofCurrentSub;
    protected SubscriptionDetail currentSub;
    protected boolean noSSLPort = true;
    private final List<String> skus;

    private String dnsNameValue;
    private String selectedRegionValue;
    private String selectedResGrpValue;

    private Combo comboSubs;
    private Combo useExistingCombo;
    private Combo comboLocations;
    private Combo comboPricetiers;

    private Label lblPricingTier;
    private Label lblLocation;
    private Label lblNewLabel;
    private Label lblSubscription;
    private Label lblResourceGroup;
    private Label lblSuffix;
    private Label lblDnsName;

    private Button btnUnblockPort;
    private Button btnUseExisting;
    private Button btnCreateNew;

    private Text dnsName;
    private Text newResGrpName;
    /**
     * Create the dialog.
     * @param parentShell
     * @throws IOException
     */
    public CreateRedisCacheForm(Shell parentShell) throws IOException {
        super(parentShell);
        azureManager = AuthMethodManager.getInstance().getAzureManager();
        allSubs = getAllSubs();
        allResGrpsofCurrentSub = new HashSet<String>();
        currentSub = null;
        skus = new ArrayList<String>();
        skus.add("C0 Basic 250MB");
        skus.add("C1 Basic 1GB");
        skus.add("C2 Basic 2.5GB");
        skus.add("C3 Basic 6GB");
        skus.add("C4 Basic 13GB");
        skus.add("C4 Basic 13GB");
        skus.add("C6 Basic 53GB");
        skus.add("C0 Standard 250MB");
        skus.add("C1 Standard 1GB");
        skus.add("C2 Standard 2.5MB");
        skus.add("C3 Standard 6GB");
        skus.add("C4 Standard 13GB");
        skus.add("C5 Standard 26GB");
        skus.add("C6 Standard 53GB");
        skus.add("P1 Premium 6GB");
        skus.add("P2 Premium 13GB");
        skus.add("P3 Premium 26GB");
        skus.add("P4 Premium 53GB");
    }

    private List<SubscriptionDetail> getAllSubs() throws AuthException, IOException
    {
        return azureManager.getSubscriptionManager().getSubscriptionDetails();
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("New Redis Cache");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        container.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        ComboViewer comboSubsViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboSubsViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboSubs = comboSubsViewer.getCombo();
        comboSubs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                allSubs.forEach(s -> {
                    if (s.getSubscriptionName().equals(comboSubs.getText())) {
                        currentSub = s;
                    }
                });
                btnCreateNew.setSelection(true);
                btnUseExisting.setSelection(false);
                newResGrpName.setVisible(true);
                useExistingCombo.setVisible(false);
            }
        });
        comboSubs.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                Set<String> names = new HashSet<String>(5);
                ParallelExecutor.For(
                        allSubs,
                        // The operation to perform with each item
                        new ParallelExecutor.Operation<SubscriptionDetail>() {
                            public void perform(SubscriptionDetail subDetail) {
                                if(subDetail.isSelected()) {
                                    names.add(subDetail.getSubscriptionName());
                                }
                            };
                        });
                comboSubsViewer.setInput(names);
                comboSubsViewer.refresh();
            }
        });
        comboSubs.setBounds(10, 107, 320, 28);

        ComboViewer comboLocationsViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboLocations = comboLocationsViewer.getCombo();
        comboLocations.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                for(Region r : Region.values()) {
                    comboLocations.add(r.label());
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                selectedRegionValue = comboLocations.getText();
            }
        });
        comboLocations.setBounds(10, 271, 320, 28);

        ComboViewer useExistingComboViewer = new ComboViewer(container, SWT.READ_ONLY);
        useExistingComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        useExistingCombo = useExistingComboViewer.getCombo();
        useExistingCombo.setVisible(false);
        useExistingCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                useExistingComboViewer.setInput(allResGrpsofCurrentSub);
                useExistingComboViewer.refresh();
            }
            @Override
            public void focusLost(FocusEvent e) {
                selectedResGrpValue = useExistingCombo.getText();
            }
        });
        useExistingCombo.setBounds(10, 203, 320, 26);

        ComboViewer comboPricetiersViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboPricetiersViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboPricetiers = comboPricetiersViewer.getCombo();
        comboPricetiers.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                comboPricetiersViewer.setInput(skus);
                comboPricetiersViewer.refresh();
            }
        });
        comboPricetiers.setBounds(10, 340, 320, 28);

        lblDnsName = new Label(container, SWT.NONE);
        lblDnsName.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblDnsName.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblDnsName.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        lblDnsName.setBounds(10, 0, 9, 20);
        lblDnsName.setText("* ");


        lblSuffix = new Label(container, SWT.NONE);
        lblSuffix.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblSuffix.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblSuffix.setBounds(162, 55, 168, 20);
        lblSuffix.setText(".redis.cache.windows.net");

        lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblNewLabel.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblNewLabel.setBounds(22, 0, 70, 20);
        lblNewLabel.setText("DNS name");

        lblSubscription = new Label(container, SWT.NONE);
        lblSubscription.setText("Subscription");
        lblSubscription.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblSubscription.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblSubscription.setBounds(22, 81, 81, 20);

        lblResourceGroup = new Label(container, SWT.NONE);
        lblResourceGroup.setText("Resource group");
        lblResourceGroup.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblResourceGroup.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblResourceGroup.setBounds(22, 151, 105, 20);

        lblLocation = new Label(container, SWT.NONE);
        lblLocation.setText("Location");
        lblLocation.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblLocation.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblLocation.setBounds(22, 245, 81, 20);

        lblPricingTier = new Label(container, SWT.NONE);
        lblPricingTier.setText("Pricing tier");
        lblPricingTier.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblPricingTier.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblPricingTier.setBounds(22, 314, 81, 20);


        btnCreateNew = new Button(container, SWT.RADIO);
        btnCreateNew.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnCreateNew.setSelection(true);
        btnCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newResGrpName.setVisible(true);
                useExistingCombo.setVisible(false);
            }
        });
        btnCreateNew.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        btnCreateNew.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnCreateNew.setBounds(10, 177, 111, 20);
        btnCreateNew.setText("Create new");

        btnUseExisting = new Button(container, SWT.RADIO);
        btnUseExisting.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                doRetriveResourceGroups();
            }
        });
        btnUseExisting.setSelection(false);
        btnUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newResGrpName.setVisible(false);
                useExistingCombo.setVisible(true);
            }
        });
        btnUseExisting.setText("Use existing");
        btnUseExisting.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        btnUseExisting.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnUseExisting.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnUseExisting.setBounds(127, 177, 111, 20);

        btnUnblockPort = new Button(container, SWT.CHECK);
        btnUnblockPort.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                noSSLPort = false;
            }
        });
        btnUnblockPort.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnUnblockPort.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnUnblockPort.setBounds(10, 380, 320, 20);
        btnUnblockPort.setText("Unblock port 6379 (not SSL encrypted)");

        newResGrpName = new Text(container, SWT.BORDER);
        newResGrpName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                selectedResGrpValue = newResGrpName.getText();
            }
        });
        newResGrpName.setBounds(10, 203, 320, 28);

        dnsName = new Text(container, SWT.BORDER);
        dnsName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                dnsNameValue = dnsName.getText();
            }
        });
        dnsName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                // todo yawei validate input
                Text text = (Text) e.widget;
            }
        });
        dnsName.setBounds(10, 23, 320, 26);

        return area;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    if(currentSub != null) {
                        RedisCacheCreator creator = new RedisCacheCreator(azureManager.getAzure(currentSub.getSubscriptionId()).redisCaches(),
                                dnsNameValue,
                                selectedRegionValue,
                                selectedResGrpValue
                                );
                        ProcessingStrategy processor = creator.CreatorMap().get("C0 Basic 250MB noSslPort");
                        doCreateRedisCache(processor);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void doCreateRedisCache(ProcessingStrategy processor) {
        DefaultLoader.getIdeHelper().runInBackground(
                null,
                "Creating Redis Cache " + ((ProcessorBase) processor).DNSName() + "...",
                false,
                true,
                "Creating Redis Cache " + ((ProcessorBase) processor).DNSName() + "...",
                new Runnable() {
                    @Override
                    public void run() {
                        processor.process();
                    }
                });
    }

    private void doRetriveResourceGroups()
    {
        allResGrpsofCurrentSub.clear();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Getting Resource Groups for Selected Subscription...", IProgressMonitor.UNKNOWN);
                try {
                    if(currentSub != null) {
                        ParallelExecutor.For(
                                azureManager.getAzure(currentSub.getSubscriptionId()).resourceGroups().list(),
                                // The operation to perform with each item
                                new ParallelExecutor.Operation<ResourceGroup>() {
                                    public void perform(ResourceGroup group) {
                                        allResGrpsofCurrentSub.add(group.name());
                                    };
                                });
                    }
                } catch (Exception ex) {
                    System.out.println("run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm: " + ex.getMessage());
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm", ex));
                }
            }
        };
        try {
            new ProgressMonitorDialog(this.getShell()).run(true, false, op);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doRetriveResourceGroups@CreateRedisCacheForm", ex));
        }
    }
    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(346, 596);
    }
    @Override
    protected boolean isResizable() {
            return false;
    }
    @Override
    public void create() {
        super.create();
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }
}
