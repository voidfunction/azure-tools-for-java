package com.microsoft.azureexplorer.actions;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.AzureManagerImpl;
import com.microsoft.tooling.msservices.model.ws.WebSite;
import com.microsoft.tooling.msservices.model.ws.WebSitePublishSettings;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappNode;
import com.microsoftopentechnologies.wacommon.utils.Messages;
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
        WebSite webapp = webappNode.getWebSite();
        try {
            if (webapp.getWebSitePublishSettings() == null) {
                webapp.setWebSitePublishSettings(AzureManagerImpl.getManager().
                        getWebSitePublishSettings(webapp.getSubscriptionId(), webapp.getWebSpaceName(), webapp.getName()));
            }
            WebSitePublishSettings.PublishProfile profile = webapp.getWebSitePublishSettings().getPublishProfileList().get(0);
            if (profile != null) {
                String url = profile.getDestinationAppUrl();
//            if (!chkBoxDeployRoot.isSelected()) {
//                url = url + "/" + artifactDescriptor.getName();
//            }
//        }
                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
            } else {
                DefaultLoader.getUIHelper().showException("No publish profile found",
                        null, "No publish profile", false, true);
            }
        } catch (Exception e1) {
            DefaultLoader.getUIHelper().showException(Messages.err, e1, Messages.err, false, true);
        }
    }
}
