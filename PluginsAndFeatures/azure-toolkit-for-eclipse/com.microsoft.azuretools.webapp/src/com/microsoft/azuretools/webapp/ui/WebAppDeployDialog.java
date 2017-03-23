package com.microsoft.azuretools.webapp.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.webapp.Activator;


@SuppressWarnings("restriction")
public class WebAppDeployDialog extends TitleAreaDialog {
	private static ILog LOG = Activator.getDefault().getLog();
    
    private Table table;
    private Browser browserAppServiceDetailes;
    private Button btnDeployToRoot;
    private String browserFontStyle;
    
    private IProject project;
    
    static class WebAppDetails {
        public SubscriptionDetail subscriptionDetail;
        public ResourceGroup resourceGroup;
        public AppServicePlan appServicePlan;
        public WebApp webApp;
    }

    private Map<String, WebAppDetails> webAppDetailsMap = new HashMap<>();
   
    /**
     * Create the dialog.
     * @param parentShell
     */
    private WebAppDeployDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
        this.project = project;
    }
    
    public static WebAppDeployDialog go(Shell parentShell, IProject project) {
        WebAppDeployDialog d = new WebAppDeployDialog(parentShell, project);
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
        setMessage("Select App Service to deploy to:");
        setTitle("Deploy Web App");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData gd_container = new GridData(GridData.FILL_BOTH);
        gd_container.widthHint = 622;
        container.setLayoutData(gd_container);
        
        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 325;
        table.setLayoutData(gd_table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        
        TableColumn tblclmnName = new TableColumn(table, SWT.LEFT);
        tblclmnName.setWidth(230);
        tblclmnName.setText("Name");
        
        TableColumn tblclmnJdk = new TableColumn(table, SWT.LEFT);
        tblclmnJdk.setWidth(100);
        tblclmnJdk.setText("JDK");
        
        TableColumn tblclmnWebContainer = new TableColumn(table, SWT.LEFT);
        tblclmnWebContainer.setWidth(150);
        tblclmnWebContainer.setText("Web container");
        
        TableColumn tblclmnResourceGroup = new TableColumn(table, SWT.LEFT);
        tblclmnResourceGroup.setWidth(190);
        tblclmnResourceGroup.setText("Resource group");
        
        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new RowLayout(SWT.VERTICAL));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        
        Button btnCreate = new Button(composite, SWT.NONE);
        btnCreate.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createAppService();
            }
        });
        btnCreate.setText("Create...");
        
        Button btnDelete = new Button(composite, SWT.NONE);
        btnDelete.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteAppService();
            }
        });
        btnDelete.setText("Delete...");
        
        Button btnRefresh = new Button(composite, SWT.NONE);
        btnRefresh.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                table.removeAll();
                browserAppServiceDetailes.setText("");
                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                fillTable();
            }
        });
        btnRefresh.setText("Refresh");
        
        Group grpAppServiceDetails = new Group(container, SWT.NONE);
        grpAppServiceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gd_grpAppServiceDetails = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
        gd_grpAppServiceDetails.heightHint = 100;
        gd_grpAppServiceDetails.widthHint = 396;
        grpAppServiceDetails.setLayoutData(gd_grpAppServiceDetails);
        grpAppServiceDetails.setText("App service details");
        
        browserAppServiceDetailes = new Browser(grpAppServiceDetails, SWT.NONE);
        FontData browserFontData = btnRefresh.getFont().getFontData()[0];
        //browserFontStyle = String.format("font-family: '%s';", browserFontData.getHeight(), browserFontData.getName());
        browserFontStyle = String.format("font-family: '%s'; font-size: 9pt;", browserFontData.getName());
        browserAppServiceDetailes.addLocationListener(new LocationListener() {
            public void changing(LocationEvent event) {
                try {
                    //System.out.println("LocationEvent.location: " + event.location);
                    if (!event.location.contains("http")) return;
                    event.doit = false;
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.location));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "changing@LocationListener@browserAppServiceDetailes@AppServiceCreateDialog", ex));
                }       
            }
            public void changed(LocationEvent event) {}
        });
        new Label(container, SWT.NONE);
        
        btnDeployToRoot = new Button(container, SWT.CHECK);
        btnDeployToRoot.setText("Deploy to root");
        new Label(container, SWT.NONE);
        
        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                fillAppServiceDetails();
            }
        });
        
        return area;
    }
    
    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                System.out.println("fillTable() async");
                fillTable();
            }
        });
    } 

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Deploy");
    }
    
    private void fillAppServiceDetails() {
        TableItem[] selections = table.getSelection();
        if (selections.length == 0) return;
        String appServiceName = selections[0].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        SubscriptionDetail sd = wad.subscriptionDetail;
        AppServicePlan asp = wad.appServicePlan;
        
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin: 7px 7px 7px 7px; " + browserFontStyle + "\">");
        sb.append(String.format("App Service name:&nbsp;<b>%s</b>;<br/>", appServiceName));
        sb.append(String.format("Subscription name:&nbsp;<b>%s</b>;&nbsp;id:&nbsp;<b>%s</b>;<br/>", sd.getSubscriptionName(), sd.getSubscriptionId()));
        String aspName = asp == null ? "N/A" : asp.name();
        String aspPricingTier = asp == null ? "N/A" : asp.pricingTier().toString();
        sb.append(String.format("App Service Plan name:&nbsp;<b>%s</b>;&nbsp;Pricing tier:&nbsp;<b>%s</b>;<br/>", aspName, aspPricingTier));

        String link = buildSiteLink(wad.webApp, null);
        sb.append(String.format("Link:&nbsp;<a href=\"%s\">%s</a>", link, link));
        sb.append("</div>");
        browserAppServiceDetailes.setText(sb.toString());
    }
    
    private static String buildSiteLink(WebApp webApp, String artifactName) {
        String appServiceLink = "https://" + webApp.defaultHostName();
        if (artifactName != null && !artifactName.isEmpty())
            return appServiceLink + "/" + artifactName;
        else
            return appServiceLink;
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(800, 550);
    }
    
    private void updateAndFillTable() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
                    try {
                        if (monitor.isCanceled()) {
                            throw new CanceledByUserException();
                        }
                        
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor)); 
                        
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                doFillTable();
                            }
                        });
                    } catch (CanceledByUserException ex) {
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("updateAndFillTable(): Canceled by user");
                                cancelPressed();
                            }
                        });

                    } catch (Exception ex) {
                    	ex.printStackTrace();
                    	LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@updateAndFillTable@AppServiceCreateDialog", ex));
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
        	ex.printStackTrace();
        	LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillTable@AppServiceCreateDialog", ex));
        }
    }
    
    private void doFillTable() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance().getResourceGroupToAppServicePlanMap();

        webAppDetailsMap.clear();
        table.removeAll();
        
        for (SubscriptionDetail sd : srgMap.keySet()) {
            if (!sd.isSelected()) continue;

            Map<String, AppServicePlan> aspMap = new HashMap<>();
            for (ResourceGroup rg : srgMap.get(sd)) {
                for (AppServicePlan asp : rgaspMap.get(rg)) {
                    aspMap.put(asp.id(), asp);
                }
            }
            
            for (ResourceGroup rg : srgMap.get(sd)) {
                for (WebApp wa : rgwaMap.get(rg)) {
                    TableItem item = new TableItem(table, SWT.NULL);

                    if (wa.javaVersion() != JavaVersion.OFF) {
                        item.setText(new String[] {
                            wa.name(),
                            wa.javaVersion().toString(),
                            wa.javaContainer() + " " + wa.javaContainerVersion(),
                            wa.resourceGroupName()
                        });
                    } else {
                        item.setText(new String[] {
                            wa.name(),
                            "Off",
                            "N/A",
                            wa.resourceGroupName()
                        });
                    }
                    
                    WebAppDetails webAppDetails = new WebAppDetails();
                    webAppDetails.webApp = wa;
                    webAppDetails.subscriptionDetail = sd;
                    webAppDetails.resourceGroup = rg;
                    webAppDetails.appServicePlan = aspMap.get(wa.appServicePlanId());
                    webAppDetailsMap.put(wa.name(), webAppDetails);
                }
            }
        }
    }
    
    private void createAppService() {
        AppServiceCreateDialog d = AppServiceCreateDialog.go(getShell());
        if (d == null) {
            // something went wrong - report an error!
            return;
        }
        WebApp wa = d.getWebApp();
        doFillTable();
        selectTableRowWithWebAppName(wa.name());
        fillAppServiceDetails();
    }
    
    private boolean validated() {
        setErrorMessage(null);
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            setErrorMessage("Select App Service to deploy to.");
            return false;
        }
        
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        if (wad.webApp.javaVersion()  == JavaVersion.OFF ) {
            setErrorMessage("Please select java based App Service");
            return false;
        }
        return true; 
    }
    
    @Override
    protected void okPressed () {
        if (!validated()) return;
        try {
            String projectName = project.getName();
            String destinationPath = project.getLocation() + "/" + projectName + ".war";
            export(projectName, destinationPath);
            deploy(projectName, destinationPath);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
        };
        super.okPressed();
    }
    
    public void export(String projectName, String destinationPath) throws Exception {

        System.out.println("Building project '" + projectName + "'...");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        project.build(IncrementalProjectBuilder.FULL_BUILD, null);

        System.out.println("Exporting to WAR...");
        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, projectName);
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, destinationPath);

        dataModel.getDefaultOperation().execute(null, null);
        System.out.println("Done.");
    }
    
    private void fillTable() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillTable();
        } else {
            doFillTable();
        }
    }
    
    private void selectTableRowWithWebAppName(String webAppName) {
        for (int ri = 0; ri < table.getItemCount(); ++ri) {
            String waName = table.getItem(ri).getText(0);
            if (waName.equals(webAppName)) {
                table.select(ri);
                break;
            }
        }
    }
    
    private void deploy(String artifactName, String artifactPath) {
        int selectedRow = table.getSelectionIndex();
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        WebApp webApp = wad.webApp;
        boolean isDeployToRoot = btnDeployToRoot.getSelection();
        String errTitle = "Deploy Web App Error";
        try {
            ProgressDialog.get(this.getShell(), "Deploy Web App Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                       monitor.beginTask("Deploying Web App...", IProgressMonitor.UNKNOWN);
                    try {
                        PublishingProfile pp = webApp.getPublishingProfile();
                        WebAppUtils.deployArtifact(artifactName, artifactPath,
                                pp, isDeployToRoot, new UpdateProgressIndicator(monitor));
                        String sitePath = buildSiteLink(wad.webApp,  isDeployToRoot ? null : artifactName);
                        monitor.setTaskName("Checking Web App availability...");
                        monitor.subTask("Link: " + sitePath);
                        // to make warn up cancelable
                        int stepLimit = 5;
                        int sleepMs = 2000;
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    for (int step = 0; step < stepLimit; ++step) {

                                        if (WebAppUtils.isUrlAccessible(sitePath))  { // warm up
                                            break;
                                        }
                                        Thread.sleep(sleepMs);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@Thread@run@ProgressDialog@deploy@AppServiceCreateDialog@SingInDialog", ex));
                                }
                            }
                        });
                        thread.run();
                        while (thread.isAlive()) {
                            if (monitor.isCanceled()) return;
                            else Thread.sleep(sleepMs);
                        }
                        monitor.done();
                        showLink(sitePath);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@deploy@AppServiceCreateDialog", ex));
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
                            }
                        });
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "deploy@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
        }
    }
    
    private void showLink(String link) {
        Display.getDefault().asyncExec(new Runnable() {
              @Override
              public void run() {
                  MessageBox messageBox = new MessageBox(
                          Display.getDefault().getActiveShell(), 
                          SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                  messageBox.setMessage( "Web App has been uploaded successfully.\nLink: " + link + "\nOpen in browser?");
                  messageBox.setText("Upload Web App Status");
                  
                  
                  int response = messageBox.open();
                  if (response == SWT.YES) {
                      try {
                          PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(link));
                      } catch (Exception ex) {
                    	  ex.printStackTrace();
                    	  LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@Display@showLink@AppServiceCreateDialog", ex));
                      }
                  }
              }
        });
    }
    
    private void deleteAppService() {
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            return;
        }

        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        
        boolean confirmed = MessageDialog.openConfirm(getShell(), 
                "Delete App Service", 
                "Do you really want to delete the App Service '" + appServiceName + "'?");
        
        if (!confirmed) {
            return;
        }
        
        String errTitle = "Delete App Service Error";
        try{
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) { 
                return;
            }
            ProgressDialog.get(this.getShell(), "Delete App Service Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                    monitor.beginTask("Deleting App Service...", IProgressMonitor.UNKNOWN);
                    
                    try {
                        manager.getAzure(wad.subscriptionDetail.getSubscriptionId()).webApps().deleteById(wad.webApp.id());
                        AzureModelController.removeWebAppFromResourceGroup(wad.resourceGroup, wad.webApp);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                table.remove(selectedRow);
                                browserAppServiceDetailes.setText("");
                            };
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@deleteAppService@AppServiceCreateDialog", ex));
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
                            }
                        });
                        
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "deleteAppService@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
        }
    }
}
