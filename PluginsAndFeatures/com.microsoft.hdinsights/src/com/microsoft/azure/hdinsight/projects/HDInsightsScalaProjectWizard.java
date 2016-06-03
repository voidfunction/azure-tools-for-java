package com.microsoft.azure.hdinsight.projects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.scalaide.ui.internal.wizards.ScalaProjectWizard;

import com.microsoft.azure.hdinsight.Activator;

public class HDInsightsScalaProjectWizard extends ScalaProjectWizard {

	@Override
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			final IJavaElement newElement= getCreatedElement();

//			selectAndReveal(javaPageTwo.getJavaProject().getProject());

//			Display.getDefault().asyncExec(new Runnable() {
//				@Override
//				public void run() {
//					IWorkbenchPart activePart= getActivePart();
//					if (activePart instanceof IPackagesViewPart) {
//						PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
//						view.tryToReveal(newElement);
//					}
//				}
//			});

			String projectName= pageOne().getProjectName();
			try {
				IProject project = getCreatedElement().getJavaProject().getProject(); // ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = HDInsightProjectNature.NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			} catch (Exception ex) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(null, "Error", "Error creating project");
					}
				});
				Activator.getDefault().log("Error cretaing project", ex);
			}
		}
		return res;
	}

}
