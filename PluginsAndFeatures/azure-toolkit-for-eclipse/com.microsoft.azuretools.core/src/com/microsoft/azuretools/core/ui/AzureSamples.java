package com.microsoft.azuretools.core.ui;

import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.core.ui.commoncontrols.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class AzureSamples  extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(Messages.azureSamplesPageLinkMS));
		} catch (Exception ex) {
			PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(),
					Messages.error,
					Messages.azureSamplesDlgErMsg, ex);
		}
		return null;
	}
}
