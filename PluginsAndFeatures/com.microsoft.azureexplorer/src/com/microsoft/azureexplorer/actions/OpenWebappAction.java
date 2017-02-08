package com.microsoft.azureexplorer.actions;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappNode;
import com.microsoftopentechnologies.wacommon.utils.Messages;
import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

import org.eclipse.ui.PlatformUI;

import java.net.URL;

@Name("Open in Browser")
public class OpenWebappAction extends NodeActionListener {

    private WebappNode webappNode;

    public OpenWebappAction(WebappNode webappNode) {
        this.webappNode = webappNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        /*WebSite webapp = webappNode.getWebSite();
        try {
            if (webapp.getWebSitePublishSettings() == null) {
                webapp.setWebSitePublishSettings(AzureManagerImpl.getManager().
                        getWebSitePublishSettings(webapp.getSubscriptionId(), webapp.getWebSpaceName(), webapp.getName()));
            }
            WebSitePublishSettings.PublishProfile profile = webapp.getWebSitePublishSettings().getPublishProfileList().get(0);
            if (profile != null) {
            	String url = profile.getDestinationAppUrl();
            	PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
            } else {
            	DefaultLoader.getUIHelper().showException("No publish profile found",
            			null, Messages.err, false, true);
            }
        } catch (AzureCmdException e1) {
        	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
        			"An error occurred while opening web apps in browser", e1);
        } catch (Exception e1) {
        	PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(), Messages.err,
        			"An error occurred while opening web apps in browser", e1);
        }*/
    }
}
