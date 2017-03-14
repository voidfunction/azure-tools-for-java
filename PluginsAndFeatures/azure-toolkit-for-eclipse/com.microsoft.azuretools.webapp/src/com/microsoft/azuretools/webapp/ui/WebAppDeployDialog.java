package com.microsoft.azuretools.webapp.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.osgi.service.log.LogService;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.eclipse.utils.ProgressDialog;
import com.microsoft.azuretools.eclipse.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.WebAppUtils;


public class WebAppDeployDialog extends TitleAreaDialog {
    @Inject
    private LogService LOGGER;
    
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

    private Map<String, WebAppDetails> webAppWebAppDetailsMap = new HashMap<>();
   
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
        setMessage("Select an App Service to deploy to");
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
        
        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(100);
        tblclmnNewColumn.setText("Name");
        
        TableColumn tblclmnJdk = new TableColumn(table, SWT.NONE);
        tblclmnJdk.setWidth(100);
        tblclmnJdk.setText("JDK");
        
        TableColumn tblclmnWebContainer = new TableColumn(table, SWT.NONE);
        tblclmnWebContainer.setWidth(100);
        tblclmnWebContainer.setText("Web Container");
        
        TableColumn tblclmnResourceGroup = new TableColumn(table, SWT.NONE);
        tblclmnResourceGroup.setWidth(100);
        tblclmnResourceGroup.setText("Resource Group");
        
        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new RowLayout(SWT.VERTICAL));
        composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        
        Button btnRefresh = new Button(composite, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                table.clearAll();
                browserAppServiceDetailes.setText("");
                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                fillTable();
            }
        });
        btnRefresh.setText("Refresh");
        
        Button btnCreate = new Button(composite, SWT.NONE);
        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createAppService();
            }
        });
        btnCreate.setText("Create..");
        
        Button btnDelete = new Button(composite, SWT.NONE);
        btnDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteAppService();
            }
        });
        btnDelete.setText("Delete...");
        
        Group grpAppServiceDetails = new Group(container, SWT.NONE);
        grpAppServiceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gd_grpAppServiceDetails = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
        gd_grpAppServiceDetails.heightHint = 100;
        gd_grpAppServiceDetails.widthHint = 396;
        grpAppServiceDetails.setLayoutData(gd_grpAppServiceDetails);
        grpAppServiceDetails.setText("App Service Details");
        
        browserAppServiceDetailes = new Browser(grpAppServiceDetails, SWT.NONE);
        FontData browserFontData = browserAppServiceDetailes.getFont().getFontData()[0];
        browserFontStyle = String.format("font: %spt %s;", browserFontData.getHeight(), browserFontData.getName());
        browserAppServiceDetailes.addLocationListener(new LocationListener() {
            public void changing(LocationEvent event) {
                try {
                    System.out.println("LocationEvent.location: " + event.location);
                    if (!event.location.contains("http")) return;
                    event.doit = false;
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.location));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
        
        fillTable();
        
        return area;
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
        WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);
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
    
    private void collectProjectDate() throws Exception {
        IProject project = null;
        ISelectionService selectionService = 
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        ISelection selection = selectionService.getSelection();

        if(selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection)selection).getFirstElement();
        }
        
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot weRoot = ws.getRoot();
        //weRoot.get
        
        IWorkspaceDescription wsd =  ws.getDescription();
        //wsd.
        
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject p : projects) {
            System.out.println(p.getName() + " : " + p.getFullPath());
            System.out.println(p.getFolder( p.getFullPath()));
            System.out.println(p.getFolder( p.getLocation()));
            System.out.println(p.getType());
            IProjectDescription dp = p.getDescription(); 
            System.out.println(dp.getName());
            System.out.println(dp.getLocationURI());
            
            System.out.println(String.join("\n", dp.getNatureIds()));
            
            System.out.println("\n");
        }
    }
    
    @Override
    protected void okPressed () {
        //super.okPressed();
        try {
            String projectName = project.getName();
            String destinationPath = project.getLocation() + "/" + projectName + ".war";
            export(projectName, destinationPath);
            deploy(projectName, destinationPath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
    
    private void updateAndFillTable() {
        try {
            ProgressDialog.get(this.getShell(), "Getting App Services...").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
                    try {
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor)); 
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                doFillTable();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void doFillTable() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance().getResourceGroupToAppServicePlanMap();

        webAppWebAppDetailsMap.clear();
        
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
                    webAppWebAppDetailsMap.put(wa.name(), webAppDetails);
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
        setErrorMessage(null);
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            setErrorMessage("Please select App Service to deploy to");
            return;
        }
        
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);
        WebApp webApp = wad.webApp;
        boolean isDeployToRoot = btnDeployToRoot.getSelection();
        String errTitle = "Deploying Web App Error";
        try {
            ProgressDialog.get(this.getShell(), "Deploy Web App Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                       monitor.beginTask("Deploying Web App...", IProgressMonitor.UNKNOWN);
                    try {
                        PublishingProfile pp = webApp.getPublishingProfile();
                        WebAppUtils.deployArtifact(artifactName, artifactPath,
                                pp, isDeployToRoot, new UpdateProgressIndicator(monitor));
                        String sitePath = buildSiteLink(wad.webApp, artifactName);
                        monitor.setTaskName("Checking the web app is available...");
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
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    LOGGER.log(LogService.LOG_ERROR,"deploy::warmup", e);
                                }
                            }
                        });
                        thread.run();
                        while (thread.isAlive()) {
                            if (monitor.isCanceled()) return;
                            else Thread.sleep(sleepMs);
                        }
                        monitor.done();
                        
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                showLink(sitePath);
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.go(getShell(), e.getMessage(), errTitle);;
                            }
                        });
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
            ErrorWindow.go(getShell(), e.getMessage(), errTitle);;
        }
    }
    
    private void showLink(String link) {
        MessageBox messageBox = new MessageBox(
                getShell(), 
                SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        messageBox.setMessage( "Web App has been uploaded successfully.\nLink: " + link + "\nOpen in browser?");
        messageBox.setText("Upload Web App Status");
        
        
        int response = messageBox.open();
        if (response == SWT.YES) {
            try {
                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(link));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                LOGGER.log(LogService.LOG_ERROR,"showLink", e);
                e.printStackTrace();
            }
        }
    }
    
    private void deleteAppService() {
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            return;
        }

        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppWebAppDetailsMap.get(appServiceName);
        
        boolean confirmed = MessageDialog.openConfirm(getShell(), 
                "Detete App Service", 
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
                    monitor.beginTask("Deleteting App Service...", IProgressMonitor.UNKNOWN);
                    
                    try {
                        manager.getAzure(wad.subscriptionDetail.getSubscriptionId()).webApps().deleteById(wad.webApp.id());
                        AzureModelController.removeWebAppFromResourceGroup(wad.resourceGroup, wad.webApp);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                table.remove(selectedRow);
                                //table.redraw();
                                browserAppServiceDetailes.setText("");
                            };
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.go(getShell(), e.getMessage(), errTitle);;
                            }
                        });
                        
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.log(LogService.LOG_ERROR,"deleteAppService", e);
            ErrorWindow.go(getShell(), e.getMessage(), errTitle);
        }
    }
}
