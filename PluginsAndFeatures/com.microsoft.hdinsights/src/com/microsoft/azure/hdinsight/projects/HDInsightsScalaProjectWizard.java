package com.microsoft.azure.hdinsight.projects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.scalaide.core.SdtConstants;
import org.scalaide.ui.ScalaImages;
import org.scalaide.ui.internal.wizards.NewScalaProjectWizardPageOne;
import org.scalaide.ui.internal.wizards.NewScalaProjectWizardPageTwo;
import org.scalaide.ui.internal.wizards.ScalaProjectWizard;

import com.microsoft.azure.hdinsight.Activator;

public class HDInsightsScalaProjectWizard extends JavaProjectWizard implements IExecutableExtension  {
	private String id;
	private Composite sparkLibraryOptionsPanel;

	public HDInsightsScalaProjectWizard() {
		this(new HDInsightScalaPageOne());
	}
	
	public HDInsightsScalaProjectWizard(HDInsightScalaPageOne page1) {
		this(page1, new HDInsightScalaPageTwo(page1));
	}
	
	public HDInsightsScalaProjectWizard(HDInsightScalaPageOne page1, HDInsightScalaPageTwo page2) {
		super(page1, page2);
		setWindowTitle("New Scala Project");
		setDefaultPageImageDescriptor(ScalaImages.SCALA_PROJECT_WIZARD());

		page1.setTitle("Create a Scala project");
		page1.setDescription("Create a Scala project in the workspace or in an external location.");
		page2.setTitle("Scala Settings");
		page2.setDescription("Define the Scala build settings.");
	}
	
	@Override
	public void setInitializationData(IConfigurationElement parameter, String arg1, Object arg2) {
		super.setInitializationData(parameter, arg1, arg2);
		this.id = parameter.getAttribute("id");
	}

	static class HDInsightScalaPageOne extends NewScalaProjectWizardPageOne {
		private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;
	
		@Override
		public IClasspathEntry[] getDefaultClasspathEntries() {
			final IClasspathEntry[] entries = super.getDefaultClasspathEntries();
			final IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];			
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					IPath jarPath = new Path(sparkLibraryOptionsPanel.getSparkLibraryPath());
					IClasspathEntry sparkEntry = JavaCore.newLibraryEntry(jarPath, null, null);
					System.arraycopy(entries, 0, newEntries, 0, entries.length);
					newEntries[entries.length] = sparkEntry;
				}
			});
			return newEntries;
		}
		
		@Override
		public void createControl(Composite parent) {
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	        final IWorkspaceRoot root = workspace.getRoot();

	        //display help contents
//	        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(),
//	                "com.persistent.winazure.eclipseplugin." +
//	                "windows_azure_project");

//	        GridLayout gridLayout = new GridLayout();
//	        gridLayout.numColumns = 2;
//	        GridData gridData = new GridData();
//	        gridData.grabExcessHorizontalSpace = true;
//	        Composite container = new Composite(parent, SWT.NONE);
	//
//	        container.setLayout(gridLayout);
//	        container.setLayoutData(gridData);

	        super.createControl(parent);
	        Composite container = (Composite) getControl();
	        sparkLibraryOptionsPanel = new SparkLibraryOptionsPanel(container, SWT.NONE);
//	        Text textProjName = new Text(container, SWT.SINGLE | SWT.BORDER);
//	        GridData gridData = new GridData();
//	        gridData.widthHint = 330;
//	        gridData.horizontalAlignment = SWT.FILL;
//	        gridData.grabExcessHorizontalSpace = true;
//	        textProjName.setLayoutData(gridData);

	        setControl(container);
		}
	}
	
	static class HDInsightScalaPageTwo extends NewJavaProjectWizardPageTwo {
		public HDInsightScalaPageTwo(NewJavaProjectWizardPageOne mainPage) {
			super(mainPage);
		}
		
		public void configureJavaProject(String newProjectCompliance, IProgressMonitor monitor) throws CoreException, InterruptedException {
	        if(monitor == null) {
	            monitor = new NullProgressMonitor();
	        }
	        byte nSteps = 6;
	        ((IProgressMonitor)monitor).beginTask(NewWizardMessages.JavaCapabilityConfigurationPage_op_desc_java, nSteps);
	        try {
	        	IProject project = addHDInsightNature(monitor);
	            Method method = JavaCapabilityConfigurationPage.class.getDeclaredMethod("getBuildPathsBlock");
	            method.setAccessible(true);
	            Object r = method.invoke(this);
	            ((BuildPathsBlock) r).configureJavaProject(newProjectCompliance, new SubProgressMonitor((IProgressMonitor)monitor, 5));
	        } catch (OperationCanceledException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
	            throw new InterruptedException();
	        } finally {
	            ((IProgressMonitor)monitor).done();
	        }

	    }

		private IProject addHDInsightNature(IProgressMonitor monitor) throws CoreException {
			if (monitor != null && monitor.isCanceled()) {
			      throw new OperationCanceledException();
			}
			IProject project = this.getJavaProject().getProject();
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				String[] newNatures = new String[natures.length + 3];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = SdtConstants.NatureId();
				newNatures[natures.length + 1] = JavaCore.NATURE_ID;
				newNatures[natures.length + 2] = HDInsightProjectNature.NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			} else {
				monitor.worked(1);
			}
			return project;
		}
		
	}
}
