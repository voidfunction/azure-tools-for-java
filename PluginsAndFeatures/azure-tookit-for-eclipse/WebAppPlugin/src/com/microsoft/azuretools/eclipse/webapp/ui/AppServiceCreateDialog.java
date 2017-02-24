package com.microsoft.azuretools.eclipse.webapp.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.log.LogService;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppServicePricingTier;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.eclipse.utils.ProgressDialog;
import com.microsoft.azuretools.eclipse.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzulZuluModel;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.StorageAccoutUtils;
import com.microsoft.azuretools.utils.WebAppUtils;

public class AppServiceCreateDialog extends TitleAreaDialog {
	@Inject
	private LogService LOGGER;

    private Text textAppName;
    private Text textNewResGrName;
    private Text textAppSevicePlanName;
    private Text textJdkOwnUrl;
    private Text textJdkOwnStorageAccountKey;
    private Combo comboWebContainer;
    private Combo comboSubscription;
    private Combo comboSelectResGr;
    private Combo comboAppServicePlan;
    private Combo comboAppServicePlanLocation;
    private Combo comboAppServicePlanPricingTier;
    private Combo comboJdk3Party;
    private Label lblAppSevicePlanLocation;
    private Label lblAppServicePlanPricingTier;
    private TabFolder tabFolderAppServicePlan;
    private TabFolder tabFolderResourceGroup;
    private TabFolder tabFolderJdk;
    private TabItem tabItemAppServicePlanUseExisting;
    private TabItem tabItemResGrUseExisting;
    private TabItem tabItemResGrCreateNew;
    private TabItem tabItemAppServicePlanCreateNew;
    private TabItem tabItemJdkDefaut;
    private TabItem tabItemJdk3rdParty;
    private TabItem tabItemJdkMyOwn;
    
    private ControlDecoration dec_textAppName;
    private ControlDecoration dec_textNewResGrName;
    private ControlDecoration dec_textAppSevicePlanName;
    private ControlDecoration dec_textJdkOwnUrl;
    private ControlDecoration dec_comboWebContainer;
    private ControlDecoration dec_comboSubscription;
    private ControlDecoration dec_comboSelectResGr;
    private ControlDecoration dec_comboAppServicePlan;
    //private ControlDecoration dec_comboAppServicePlanLocation;
    //private ControlDecoration dec_comboAppServicePlanPricingTier;
    private ControlDecoration dec_comboJdk3Party;

    private final static String textNotAvailable = "N/A";
    
    // controls to types bindings by index 
    private List<WebContainer> binderWebConteiners;
    private List<SubscriptionDetail> binderSubscriptionDetails;
    private List<ResourceGroup> binderResourceGroup;
    private List<AppServicePlan> binderAppServicePlan;
    private List<Location> binderAppServicePlanLocation;
    private List<AppServicePricingTier> binderAppServicePlanPricingTier;
    private List<AzulZuluModel> binderJdk3Party;
    
    protected WebApp webApp;

    public WebApp getWebApp() {
        return this.webApp;
    }

    
    public static AppServiceCreateDialog go(Shell parentShell) {
        AppServiceCreateDialog d = new AppServiceCreateDialog(parentShell);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    private AppServiceCreateDialog(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    } 
    
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Create Azure App Service");
        setTitle("Create App Service");
        Composite area = (Composite) super.createDialogArea(parent);
        
        Composite composite = new Composite(area, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        
        Group grpAppService = new Group(composite, SWT.NONE);
        grpAppService.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        grpAppService.setText("App Service");
        grpAppService.setLayout(new GridLayout(2, false));
        
        Label lblAppName = new Label(grpAppService, SWT.NONE);
        lblAppName.setText("Enter Name");
        new Label(grpAppService, SWT.NONE);
        
        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_textAppName = decorateContorolAndRegister(textAppName);
        
        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(".azurewebsites.net");
        
        Label lblWebContainer = new Label(grpAppService, SWT.NONE);
        lblWebContainer.setText("Web Container");
        new Label(grpAppService, SWT.NONE);
        
        comboWebContainer = new Combo(grpAppService, SWT.READ_ONLY);
        comboWebContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        dec_comboWebContainer = decorateContorolAndRegister(comboWebContainer);
        
        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setText("Subscription");
        new Label(grpAppService, SWT.NONE);
        
        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
                fillResourceGroups();
                fillAppServicePlans();
                fillAppServicePlansDetails();
                fillAppServicePlanLocations();
        	}
        });
        dec_comboSubscription = decorateContorolAndRegister(comboSubscription);
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        Group grpResourceGroup = new Group(grpAppService, SWT.NONE);
        grpResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        grpResourceGroup.setText("Resource Group");
        grpResourceGroup.setLayout(new GridLayout(2, false));
        new Label(grpResourceGroup, SWT.NONE);
        
        tabFolderResourceGroup = new TabFolder(grpResourceGroup, SWT.NONE);
        tabFolderResourceGroup.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		cleanError();
        	}
        });
        tabFolderResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        
        tabItemResGrUseExisting = new TabItem(tabFolderResourceGroup, SWT.NONE);
        tabItemResGrUseExisting.setText("Use Existing");
        
        Composite compositeResGrUseExisting = new Composite(tabFolderResourceGroup, SWT.NONE);
        tabItemResGrUseExisting.setControl(compositeResGrUseExisting);
        compositeResGrUseExisting.setLayout(new GridLayout(1, false));
        
        Label lblSelectResourceGroup = new Label(compositeResGrUseExisting, SWT.NONE);
        lblSelectResourceGroup.setBounds(0, 0, 59, 14);
        lblSelectResourceGroup.setText("Select Resource Group");
        
        comboSelectResGr = new Combo(compositeResGrUseExisting, SWT.READ_ONLY);
        comboSelectResGr.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboSelectResGr.setBounds(0, 0, 26, 22);
        dec_comboSelectResGr = decorateContorolAndRegister(comboSelectResGr);
        
        tabItemResGrCreateNew = new TabItem(tabFolderResourceGroup, SWT.NONE);
        tabItemResGrCreateNew.setText("Create New");
        
        Composite compositeResGrCreateNew = new Composite(tabFolderResourceGroup, SWT.NONE);
        tabItemResGrCreateNew.setControl(compositeResGrCreateNew);
        compositeResGrCreateNew.setLayout(new GridLayout(1, false));
        
        Label lblNewResourceGroup = new Label(compositeResGrCreateNew, SWT.NONE);
        lblNewResourceGroup.setBounds(0, 0, 59, 14);
        lblNewResourceGroup.setText("Enter Name");
        
        textNewResGrName = new Text(compositeResGrCreateNew, SWT.BORDER);
        textNewResGrName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textNewResGrName.setBounds(0, 0, 64, 19);
        dec_textNewResGrName = decorateContorolAndRegister(textNewResGrName);
        
        Group groupAppServicePlan = new Group(composite, SWT.NONE);
        groupAppServicePlan.setLayout(new GridLayout(1, false));
        groupAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        groupAppServicePlan.setText("App Service Plan");
        
        tabFolderAppServicePlan = new TabFolder(groupAppServicePlan, SWT.NONE);
        tabFolderAppServicePlan.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		cleanError();
        	}
        });
        tabFolderAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        tabItemAppServicePlanUseExisting = new TabItem(tabFolderAppServicePlan, SWT.NONE);
        tabItemAppServicePlanUseExisting.setText("Use Existing");
        
        Composite compositeAppServicePlanUseExisting = new Composite(tabFolderAppServicePlan, SWT.NONE);
        tabItemAppServicePlanUseExisting.setControl(compositeAppServicePlanUseExisting);
        compositeAppServicePlanUseExisting.setLayout(new GridLayout(1, false));
        
        Label lblSelectAppService = new Label(compositeAppServicePlanUseExisting, SWT.NONE);
        lblSelectAppService.setBounds(0, 0, 59, 14);
        lblSelectAppService.setText("Select App Service Plan");
        
        comboAppServicePlan = new Combo(compositeAppServicePlanUseExisting, SWT.READ_ONLY);
        comboAppServicePlan.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fillAppServicePlansDetails();
        	}
        });
        comboAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        comboAppServicePlan.setBounds(0, 0, 26, 22);
        dec_comboAppServicePlan = decorateContorolAndRegister(comboAppServicePlan);
        
        Composite compositeAppServicePlanDetails = new Composite(compositeAppServicePlanUseExisting, SWT.NONE);
        compositeAppServicePlanDetails.setLayout(new GridLayout(2, false));
        compositeAppServicePlanDetails.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        
        Label lblLocation = new Label(compositeAppServicePlanDetails, SWT.NONE);
        lblLocation.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        lblLocation.setText("Location:");
        
        lblAppSevicePlanLocation = new Label(compositeAppServicePlanDetails, SWT.NONE);
        lblAppSevicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        lblAppSevicePlanLocation.setText("N/A");
        
        Label lblPrisingTier = new Label(compositeAppServicePlanDetails, SWT.NONE);
        lblPrisingTier.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        lblPrisingTier.setText("Pricing tier:");
        
        lblAppServicePlanPricingTier = new Label(compositeAppServicePlanDetails, SWT.NONE);
        lblAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        lblAppServicePlanPricingTier.setText("N/A");
        
        tabItemAppServicePlanCreateNew = new TabItem(tabFolderAppServicePlan, SWT.NONE);
        tabItemAppServicePlanCreateNew.setText("Create New");
        
        Composite compositeAppServicePlanCreateNew = new Composite(tabFolderAppServicePlan, SWT.NONE);
        tabItemAppServicePlanCreateNew.setControl(compositeAppServicePlanCreateNew);
        compositeAppServicePlanCreateNew.setLayout(new GridLayout(1, false));
        
        Label lblNewAppService = new Label(compositeAppServicePlanCreateNew, SWT.NONE);
        lblNewAppService.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        lblNewAppService.setText("Enter Name");
        
        textAppSevicePlanName = new Text(compositeAppServicePlanCreateNew, SWT.BORDER);
        GridData gd_textAppSevicePlanName = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
        gd_textAppSevicePlanName.widthHint = 413;
        textAppSevicePlanName.setLayoutData(gd_textAppSevicePlanName);
        dec_textAppSevicePlanName = decorateContorolAndRegister(textAppSevicePlanName);
        
        Label lblSelectLocation = new Label(compositeAppServicePlanCreateNew, SWT.NONE);
        lblSelectLocation.setText("Select Location");
        
        comboAppServicePlanLocation = new Combo(compositeAppServicePlanCreateNew, SWT.READ_ONLY);
        comboAppServicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        //dec_comboAppServicePlanLocation = decorateContorolAndRegister(comboAppServicePlanLocation);
        
        Label lblPricingTier = new Label(compositeAppServicePlanCreateNew, SWT.NONE);
        lblPricingTier.setText("Select Pricing Tier");
        
        comboAppServicePlanPricingTier = new Combo(compositeAppServicePlanCreateNew, SWT.READ_ONLY);
        comboAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        //dec_comboAppServicePlanPricingTier = decorateContorolAndRegister(comboAppServicePlanPricingTier);
        
        Link linkAppServicePricing = new Link(groupAppServicePlan, SWT.NONE);
        linkAppServicePricing.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        linkAppServicePricing.setText("<a>App Service Pricing</a>");
        
        Group grpJdk = new Group(composite, SWT.NONE);
        grpJdk.setLayout(new GridLayout(1, false));
        grpJdk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpJdk.setText("JDK");
        
        tabFolderJdk = new TabFolder(grpJdk, SWT.NONE);
        tabFolderJdk.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		cleanError();
        	}
        });
        tabFolderJdk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        
        tabItemJdkDefaut = new TabItem(tabFolderJdk, SWT.NONE);
        tabItemJdkDefaut.setText("Default");
        
        Composite compositeJdkDefault = new Composite(tabFolderJdk, SWT.NONE);
        tabItemJdkDefaut.setControl(compositeJdkDefault);
        compositeJdkDefault.setLayout(new GridLayout(1, false));
        
        Label lblNewLabel = new Label(compositeJdkDefault, SWT.NONE);
        lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblNewLabel.setText("Deploy the default JDK offered by Azure Web Apps service (JDK 8)");
        
        tabItemJdk3rdParty = new TabItem(tabFolderJdk, SWT.NONE);
        tabItemJdk3rdParty.setText("3rd Party");
        
        Composite compositeJdk3rdParth = new Composite(tabFolderJdk, SWT.NONE);
        tabItemJdk3rdParty.setControl(compositeJdk3rdParth);
        compositeJdk3rdParth.setLayout(new GridLayout(1, false));
        
        Label lblNewLabel_1 = new Label(compositeJdk3rdParth, SWT.NONE);
        lblNewLabel_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblNewLabel_1.setText("Deploy a 3rd party JDK available on Azure");
        
        comboJdk3Party = new Combo(compositeJdk3rdParth, SWT.READ_ONLY);
        comboJdk3Party.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_comboJdk3Party = decorateContorolAndRegister(comboJdk3Party);
        
        Link link = new Link(compositeJdk3rdParth, SWT.NONE);
        link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        link.setText("<a>GNU General Public License</a>");
        
        tabItemJdkMyOwn = new TabItem(tabFolderJdk, SWT.NONE);
        tabItemJdkMyOwn.setText("My Own");
        
        Composite compositeJdkMyOwn = new Composite(tabFolderJdk, SWT.NONE);
        tabItemJdkMyOwn.setControl(compositeJdkMyOwn);
        compositeJdkMyOwn.setLayout(new GridLayout(1, false));
        
        Label lblJdkZipArchive = new Label(compositeJdkMyOwn, SWT.NONE);
        lblJdkZipArchive.setText("JDK Zip Archive URL");
        
        textJdkOwnUrl = new Text(compositeJdkMyOwn, SWT.BORDER);
        textJdkOwnUrl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        dec_textJdkOwnUrl = decorateContorolAndRegister(textJdkOwnUrl);
        
        Label lblStorageAccountKey = new Label(compositeJdkMyOwn, SWT.NONE);
        lblStorageAccountKey.setText("Storage Account Key (if the URL above is a private blob)");
        
        textJdkOwnStorageAccountKey = new Text(compositeJdkMyOwn, SWT.BORDER);
        textJdkOwnStorageAccountKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        decorateContorolAndRegister(textJdkOwnStorageAccountKey);
        
        fillWebContainers();
        fillSubscriptions();
        fillResourceGroups();
        fillAppServicePlans();
        fillAppServicePlansDetails();
        fillAppServicePlanLocations();
        fillAppServicePlanPricingTiers();
        fill3PartyJdk();
        
        return area;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Create");
    }

    protected void fillWebContainers() {
        try {
            List<WebContainer> wcl = createListFromClassFields(WebContainer.class);
            comboWebContainer.removeAll();
            binderWebConteiners = new ArrayList<WebContainer>();
            for (WebContainer wc : wcl) {
                comboWebContainer.add(wc.toString());
                binderWebConteiners.add(wc);
            }
            if (comboWebContainer.getItemCount() > 0) {
            	comboWebContainer.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(LogService.LOG_ERROR, "fillWebContainers", ex);
        }
    }
    
    protected static <T> List<T> createListFromClassFields(Class<?> c) throws IllegalAccessException {
        List<T> list = new LinkedList<T>();

        Field[] declaredFields = c.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && Modifier.isPublic(modifiers)) {
                @SuppressWarnings("unchecked")
                T value = (T)field.get(null);
                list.add(value);
            }
        }
        return list;
    }

    protected void fillSubscriptions(){
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillSubscriptions();
        } else {
            doFillSubscriptions();
        }
    }
    
    private void updateAndFillSubscriptions() {
    	try {
			ProgressDialog.get(this.getShell(), "Getting App Services...").run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) {
		   			monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
	    			if (monitor.isCanceled()) {
	    				AzureModel.getInstance().setResourceGroupToWebAppMap(null);
	    				Display.getDefault().asyncExec(new Runnable() {
	    					@Override
	    					public void run() {
	    						AppServiceCreateDialog.super.cancelPressed();
	    					}
	    				});
	    			}
	    			
	    			try {
						AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								doFillSubscriptions();
							};
						});					
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void doFillSubscriptions() {
        try {
            // reset model
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            if (sdl == null) {
                System.out.println("sdl is null");
                return;
            }
            
            comboSubscription.removeAll();;
            binderSubscriptionDetails = new ArrayList<SubscriptionDetail>();
            for (SubscriptionDetail sd : sdl) {
                if (!sd.isSelected()) continue;
                comboSubscription.add(sd.getSubscriptionName());
                binderSubscriptionDetails.add(sd);
            }
            if (comboSubscription.getItemCount() > 0) {
            	comboSubscription.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(LogService.LOG_ERROR,"doFillSubscriptions", ex);
        }
    }
    
    protected void fillResourceGroups(){
    	int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        comboSelectResGr.removeAll();
        binderResourceGroup = new ArrayList<ResourceGroup>();
        for (ResourceGroup rg : rgl) {
        	comboSelectResGr.add(rg.name());
        	binderResourceGroup.add(rg);
        }
        
        if (comboSelectResGr.getItemCount() > 0) {
        	comboSelectResGr.select(0);
        }
    }
    
    protected void fillAppServicePlans() {
    	int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }
        
        comboAppServicePlan.removeAll();
        binderAppServicePlan = new ArrayList<AppServicePlan>();
        for (ResourceGroup rg : rgl) {
            List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
            for (AppServicePlan asp : aspl) {
            	binderAppServicePlan.add(asp);
            	comboAppServicePlan.add(asp.name());
            }
        }
        
        if (comboAppServicePlan.getItemCount() > 0) {
        	comboAppServicePlan.select(0);
        }
    }
    
    protected void fillAppServicePlansDetails() {
    	int i = comboAppServicePlan.getSelectionIndex();
        if (i < 0) {
        	lblAppSevicePlanLocation.setText(textNotAvailable);
            lblAppServicePlanPricingTier.setText(textNotAvailable);
        } else {
            AppServicePlan asp = binderAppServicePlan.get(i);
            lblAppSevicePlanLocation.setText(asp.region().label());
            lblAppServicePlanPricingTier.setText(asp.pricingTier().toString());
        }
    }
    
    protected void fillAppServicePlanLocations() {
    	int i = comboSubscription.getSelectionIndex();
    	if (i < 0) { // empty
    		System.out.println("No subscription is selected");
    		return;
    	}
    	
    	comboAppServicePlanLocation.removeAll();
    	binderAppServicePlanLocation = new ArrayList<Location>();
    	//List<Location> locl = AzureModel.getInstance().getSubscriptionToLocationMap().get(binderSubscriptionDetails.get(i));
    	Map<SubscriptionDetail, List<Location>> sdlocMap = AzureModel.getInstance().getSubscriptionToLocationMap();
    	SubscriptionDetail sd = binderSubscriptionDetails.get(i);
        List<Location> locl = sdlocMap.get(sd);
    	for (Location loc : locl) {
    		comboAppServicePlanLocation.add(loc.displayName());
    		binderAppServicePlanLocation.add(loc);
    	}
    	
    	if (comboAppServicePlanLocation.getItemCount() > 0)  {
    		comboAppServicePlanLocation.select(0);
    	}
    }
    
    protected void fillAppServicePlanPricingTiers() {
        try {
        	comboAppServicePlanPricingTier.removeAll();
        	binderAppServicePlanPricingTier = new ArrayList<AppServicePricingTier>();
            List<AppServicePricingTier> l = createListFromClassFields(AppServicePricingTier.class);
            for (AppServicePricingTier aspt : l) {
            	comboAppServicePlanPricingTier.add(aspt.toString());
            	binderAppServicePlanPricingTier.add(aspt);
            }
            if (comboAppServicePlanPricingTier.getItemCount() > 0) {
            	comboAppServicePlanPricingTier.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(LogService.LOG_ERROR,"fillAppServicePlanPricingTiers", ex);
        }
    }
    
    protected void fill3PartyJdk() {
    	comboJdk3Party.removeAll();
        binderJdk3Party = new ArrayList<AzulZuluModel>();
        for (AzulZuluModel jdk : AzulZuluModel.values()) {
            if (jdk.isDeprecated()) continue;
            binderJdk3Party.add(jdk);
            comboJdk3Party.add(jdk.getName());
        }
        if (comboJdk3Party.getItemCount() > 0) {
        	comboJdk3Party.select(0);
        }
    }
    
    @Override
    protected void okPressed() {
    	if (validated()) {
    		String errTitle = "Create App Service Error";
        	try {
    			ProgressDialog.get(this.getShell(), "Create App Service Progress").run(true, true, new IRunnableWithProgress() {
    				@Override
    				public void run(IProgressMonitor monitor) {
    		   			monitor.beginTask("Creating App Service....", IProgressMonitor.UNKNOWN);
    	    			if (monitor.isCanceled()) {
    	    				AzureModel.getInstance().setResourceGroupToWebAppMap(null);
    	    				Display.getDefault().asyncExec(new Runnable() {
    	    					@Override
    	    					public void run() {
    	    						AppServiceCreateDialog.super.cancelPressed();
    	    					}
    	    				});
    	    			}
    	    			
    	    			try {
    	    				webApp = createAppService(new UpdateProgressIndicator(monitor));
    						Display.getDefault().asyncExec(new Runnable() {
    							@Override
    							public void run() {
    								AppServiceCreateDialog. super.okPressed();
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
    		} catch (InvocationTargetException | InterruptedException e) {
    			e.printStackTrace();
    			LOGGER.log(LogService.LOG_ERROR, " okPressed", e);
    			ErrorWindow.go(getShell(), e.getMessage(), errTitle);;
    		}
    	}
    }
    
    private List<ControlDecoration> decorations = new LinkedList<ControlDecoration>();
    
    protected ControlDecoration decorateContorolAndRegister(Control c) {
    	ControlDecoration d = new ControlDecoration(c, SWT.TOP|SWT.RIGHT);
    	FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
    	Image img = fieldDecoration.getImage();
    	d.setImage(img);
    	d.hide();
    	decorations.add(d);
    	return d;
    }
    
    protected void setError(ControlDecoration d, String message) {
    	d.setDescriptionText(message);
    	setErrorMessage("Form validation error.");
    	d.show();
    }
    
    protected void cleanError() {
    	for (ControlDecoration d: decorations) {
    		d.hide();
    	}
    	setErrorMessage(null);
    }
     
    protected boolean validated() {
        model.collectData();

        cleanError();
        String webappName = model.webAppName;
        if (webappName.length() > 60 || !webappName.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
            StringBuilder builder = new StringBuilder();
            builder.append("The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. ");
            builder.append("The length can be between 2 and 60 characters. ");
            setError(dec_textAppName, builder.toString());
            return false;
        } else {
            for (List<WebApp> wal : AzureModel.getInstance().getResourceGroupToWebAppMap().values()) {
                for (WebApp wa : wal) {
                    if (wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                    	setError(dec_textAppName,"The name is already taken");
                        return false;
                    }
                }
            }
        }
        
        if (model.webContainer == null) {
        	setError(dec_comboWebContainer,"Select a valid web container.");
        	return false;
        }
        if (model.subscriptionDetail == null) {
        	setError(dec_comboSubscription,"Select a valid subscription.");
        	return false;
        }

        if (model.isResourceGroupCreateNew) {
            if (model.resourceGroupNameCreateNew.isEmpty()) {
            	setError(dec_textNewResGrName,"Enter a valid resource group name");
            	return false;
            } else {
                if (!model.resourceGroupNameCreateNew.matches("^[A-Za-z0-9-_()\\.]*[A-Za-z0-9-_()]$")) {
                	setError(dec_textNewResGrName,"Resounce group name can only include alphanumeric characters, periods, underscores, hyphens, and parenthesis and can't end in a period.");
                	return false;
                }

                for (List<ResourceGroup> rgl : AzureModel.getInstance().getSubscriptionToResourceGroupMap().values()) {
                    for (ResourceGroup rg : rgl) {
                        if (rg.name().toLowerCase().equals(model.resourceGroupNameCreateNew.toLowerCase())) {
                        	setError(dec_textNewResGrName,"The name is already taken");
                            return false;
                        }
                    }
                }
            }
        } else {
            if (model.resourceGroup == null ) {
            	setError(dec_comboSelectResGr, "Select a valid resource group.");
            	return false;
            }
        }

        if (model.isAppServicePlanCreateNew) {
            if (model.appServicePlanNameCreateNew.isEmpty()) {
            	setError(dec_textAppSevicePlanName, "Enter a valid App Service Plan name.");
                return false;
            } else {
                if (!model.appServicePlanNameCreateNew.matches("^[A-Za-z0-9-]*[A-Za-z0-9-]$")) {
                	setError(dec_textAppSevicePlanName, "App Service Plan name can only include alphanumeric characters and hyphens.");
                    return false;
                }
                // App service plan name must be unique in each subscription
                SubscriptionDetail sd = model.subscriptionDetail;
                List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd);
                for (ResourceGroup rg : rgl ) {
                    List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
                    for (AppServicePlan asp : aspl) {
                        if (asp.name().toLowerCase().equals(model.appServicePlanNameCreateNew.toLowerCase())) {
                        	setError(dec_textAppSevicePlanName, "App service plan name must be unuque in each subscription.");
                        	return false;
                        }
                    }
                }
            }
        } else {
            if (model.appServicePlan == null ) {
            	setError(dec_comboAppServicePlan,"Select a valid App Service Plan.");
            	return false;
            }
        }

    	return volidatedJdkTab();
    }
    
    protected boolean volidatedJdkTab() {
        try {
            switch (model.jdkTab) {
                case Default:
                    // do nothing
                    model.jdkDownloadUrl = null;
                    break;
                case ThirdParty:
                    if (!WebAppUtils.isUrlAccessible(model.jdk3PartyUrl)) {
                    	setError(dec_comboJdk3Party, "Please check the URL is valid.");
                    	return false;
                    }
                    model.jdkDownloadUrl = model.jdk3PartyUrl;
                    break;
                case Own:
                    if (model.jdkOwnUrl.isEmpty()) {
                    	setError(dec_textJdkOwnUrl, "Enter a valid URL.");
                    	return false;
                    } else {
                        // first check the link is accessible as it is
                        if (!WebAppUtils.isUrlAccessible(model.jdkOwnUrl)) {
                            // create shared access signature url and check its accessibility
                            String sasUrl = StorageAccoutUtils.getBlobSasUri(model.jdkOwnUrl, model.storageAccountKey);
                            if (!WebAppUtils.isUrlAccessible(sasUrl)) {
                            	setError(dec_textJdkOwnUrl,"Please check the storage account key and/or URL is valid.");
                            	return false;
                            } else {
                                model.jdkDownloadUrl = sasUrl;
                            }
                        } else {
                            model.jdkDownloadUrl = model.jdkOwnUrl;
                        }
                    }
                    // link to a ZIP file
                    // consider it's a SAS link
                    String urlPath = new URI(model.jdkOwnUrl).getPath();
                    if (!urlPath.endsWith(".zip")) {
                    	setError(dec_textJdkOwnUrl,"link to a zip file is expected.");
                    	return false;
                    }
                    break;
	            default:
	            	throw new Exception("Unknown JDK tab");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //LOGGER.error("volidateJdkTab", ex);
            //ErrorWindow.show(ex.getMessage(), "Form Data Validation Error", this.contentPane);
            setError(dec_textJdkOwnUrl,"Url validation exception:" + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    protected Model model = new Model();
    
    public enum JdkTab {
        Default,
        ThirdParty,
        Own;
    }
    
    protected class Model {
        public String webAppName;
        public WebContainer webContainer;
        public SubscriptionDetail subscriptionDetail;

        public boolean isResourceGroupCreateNew;
        public ResourceGroup resourceGroup;
        public String resourceGroupNameCreateNew;

        public boolean isAppServicePlanCreateNew;
        public AppServicePlan appServicePlan;
        public String appServicePlanNameCreateNew;
        public Location appServicePlanLocationCreateNew;
        public AppServicePricingTier appServicePricingTierCreateNew;

        public String jdk3PartyUrl;
        public String jdkOwnUrl;
        public String storageAccountKey;
        public JdkTab jdkTab;
        public String jdkDownloadUrl;

        public void collectData() {
            webAppName = textAppName.getText().trim();
            
            int index = comboWebContainer.getSelectionIndex();
            webContainer = index < 0 ? null : binderWebConteiners.get(index);
            
            index = comboSubscription.getSelectionIndex();
            subscriptionDetail = index < 0 ? null : binderSubscriptionDetails.get(index);


            isResourceGroupCreateNew = tabFolderResourceGroup.getSelection()[0] == tabItemResGrCreateNew;
            index = comboSelectResGr.getSelectionIndex();
            resourceGroup = index < 0 ? null : binderResourceGroup.get(index);
            resourceGroupNameCreateNew = textNewResGrName.getText().trim();

            isAppServicePlanCreateNew = tabFolderAppServicePlan.getSelection()[0] == tabItemAppServicePlanCreateNew;
            index = comboAppServicePlan.getSelectionIndex();
            appServicePlan = index < 0 ? null : binderAppServicePlan.get(index);

            appServicePlanNameCreateNew = textAppSevicePlanName.getText().trim();

            index = comboAppServicePlanPricingTier.getSelectionIndex();
            appServicePricingTierCreateNew = index < 0 ? null : binderAppServicePlanPricingTier.get(index);

            index = comboAppServicePlanLocation.getSelectionIndex();
            appServicePlanLocationCreateNew = index < 0 ? null : binderAppServicePlanLocation.get(index);

            TabItem selectedJdkPanel = tabFolderJdk.getSelection()[0];
            jdkTab = (selectedJdkPanel == tabItemJdkDefaut)
                ? JdkTab.Default
                : (selectedJdkPanel == tabItemJdk3rdParty)
                    ? JdkTab.ThirdParty
                    : (selectedJdkPanel == tabItemJdkMyOwn)
                        ? JdkTab.Own
                        : null;

            index = comboJdk3Party.getSelectionIndex();
            AzulZuluModel jdk3Party = index < 0 ? null : binderJdk3Party.get(index);
            jdk3PartyUrl = jdk3Party == null ? null : jdk3Party.getDownloadUrl();
            jdkOwnUrl = textJdkOwnUrl.getText().trim();
            storageAccountKey = textJdkOwnStorageAccountKey.getText().trim();
            jdkDownloadUrl = null; // get the value in the validate phase
        }
    }
    
    protected WebApp createAppService(IProgressIndicator progressIndicator) throws Exception {

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return null; }

        Azure azure = azureManager.getAzure(model.subscriptionDetail.getSubscriptionId());
        WebApp.DefinitionStages.Blank definitionStages = azure.webApps().define(model.webAppName);
        WebApp.DefinitionStages.WithAppServicePlan ds1;

        if (model.isResourceGroupCreateNew) {
            ds1 = definitionStages.withNewResourceGroup(model.resourceGroupNameCreateNew);
        } else {
            ds1 = definitionStages.withExistingResourceGroup(model.resourceGroup);
        }

        WebAppBase.DefinitionStages.WithCreate<WebApp> ds2;
        if (model.isAppServicePlanCreateNew) {
            ds2 = ds1.withNewAppServicePlan(model.appServicePlanNameCreateNew)
                    .withRegion(model.appServicePlanLocationCreateNew.name())
                    .withPricingTier(model.appServicePricingTierCreateNew);
        } else {
            ds2 = ds1.withExistingAppServicePlan(model.appServicePlan);
        }

        if (model.jdkDownloadUrl == null) { // no custom jdk
            ds2 = ds2.withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer);
        }

        WebApp myWebApp = ds2.create();

        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Deploying custom jdk...");
            WebAppUtils.deployCustomJdk(myWebApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        }

        // update cache
        if (model.isResourceGroupCreateNew) {
            ResourceGroup rg = azure.resourceGroups().getByName(model.resourceGroupNameCreateNew);
            //azureModel.getSubscriptionToResourceGroupMap().get(model.subscriptionDetail).add(rg);
            AzureModelController.addNewResourceGroup(model.subscriptionDetail, rg);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                //azureModel.getResourceGroupToAppServicePlanMap().put(rg, Arrays.asList(asp));
                //azureModel.getResourceGroupToWebAppMap().put(rg, Arrays.asList(myWebApp));
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, asp);
                AzureModelController.addNewWebAppToJustCreatedResourceGroup(rg, myWebApp);
            }
        } else {
            ResourceGroup rg = model.resourceGroup;
            //azureModel.getResourceGroupToWebAppMap().get(rg).add(myWebApp);
            AzureModelController.addNewWebAppToExistingResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                AzureModelController.addNewAppServicePlanToExistingResourceGroup(rg, asp);
            }
        }

        return myWebApp;
    }

}


