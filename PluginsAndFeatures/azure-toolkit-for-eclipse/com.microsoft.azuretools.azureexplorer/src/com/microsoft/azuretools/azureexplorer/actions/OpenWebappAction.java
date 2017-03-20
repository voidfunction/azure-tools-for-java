package com.microsoft.azuretools.azureexplorer.actions;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappNode;

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
    	try {
    		WebApp webApp = webappNode.getWebApp();
    		String appServiceLink = "https://" + webApp.defaultHostName();
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(appServiceLink));
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }
}
