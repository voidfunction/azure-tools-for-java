package com.microsoft.intellij.serviceexplorer.azure.webapps;


import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappNode;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name("Open in Browser")
public class OpenWebappAction extends NodeActionListener {

    private WebappNode webappNode;

    public OpenWebappAction(WebappNode webappNode) {
        this.webappNode = webappNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        WebApp webApp = webappNode.getWebApp();
        try {
//            if (webapp.getWebSitePublishSettings() == null) {
//                webapp.setWebSitePublishSettings(AzureManagerImpl.getManager(webappNode.getProject()).
//                        getWebSitePublishSettings(webapp.getSubscriptionId(), webapp.getWebSpaceName(), webapp.getName()));
//            }
//            WebSitePublishSettings.PublishProfile profile = webapp.getWebSitePublishSettings().getPublishProfileList().get(0);
//            if (profile != null) {
//                String url = profile.getDestinationAppUrl();
//            if (!chkBoxDeployRoot.isSelected()) {
//                url = url + "/" + artifactDescriptor.getName();
//            }
//        }

                String appServiceLink = "https://" + webApp.defaultHostName();
                Desktop.getDesktop().browse(URI.create(appServiceLink));
//            } else {
//                PluginUtil.displayErrorDialog("No publish profile", "No publish profile found");
//            }
        } catch (IOException e1) {
            PluginUtil.displayErrorDialogAndLog(message("error"), message("error"), e1);
        }
    }
}