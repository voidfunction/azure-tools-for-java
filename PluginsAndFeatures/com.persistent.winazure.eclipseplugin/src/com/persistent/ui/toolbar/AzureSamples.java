package com.persistent.ui.toolbar;

import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import com.microsoftopentechnologies.wacommon.utils.PluginUtil;

public class AzureSamples  extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(Messages.azureSamplesPageLinkMS));
		} catch (Exception ex) {
			PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(),
					Messages.errTtl,
					Messages.azureSamplesDlgErMsg, ex);
		}
		return null;
	}
}
