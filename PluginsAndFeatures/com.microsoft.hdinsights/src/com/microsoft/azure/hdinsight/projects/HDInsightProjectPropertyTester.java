package com.microsoft.azure.hdinsight.projects;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferenceNode;

import com.microsoft.azure.hdinsight.Activator;
import com.persistent.ui.propertypage.WAProjectNature;

public class HDInsightProjectPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object object, String property, Object[] args, Object expectedValue) {
		try {
			if (object instanceof IProject) {
				IProject project = (IProject) object;
				if (project.isOpen()) {
					if (project.hasNature(WAProjectNature.NATURE_ID)) {
						return true;
					}
				}
			}
		} catch (Exception ex) {
			Activator.getDefault().log("Error", ex);
		}
		return false;
	}
}
