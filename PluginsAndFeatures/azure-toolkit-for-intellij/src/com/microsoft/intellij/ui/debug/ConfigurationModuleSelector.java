package com.microsoft.intellij.ui.debug;

import com.intellij.application.options.ModuleListCellRenderer;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.psi.PsiClass;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.SortedComboBoxModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JComboBox;
import org.jetbrains.annotations.Nullable;

public class ConfigurationModuleSelector
{
    private final Project myProject;
    private final JComboBox myModulesList;

    /**
     * @deprecated
     */
    public ConfigurationModuleSelector(Project project, JComboBox modulesList)
    {
        this(project, modulesList, "<no module>");
    }

    public ConfigurationModuleSelector(Project project, ModulesComboBox modulesComboBox)
    {
        this(project, modulesComboBox, "<no module>");
    }

    public ConfigurationModuleSelector(Project project, ModulesComboBox modulesComboBox, String noModule)
    {
        this.myProject = project;
        this.myModulesList = modulesComboBox;
        modulesComboBox.allowEmptySelection(noModule);
    }

    /**
     * @deprecated
     */
    public ConfigurationModuleSelector(Project project, JComboBox modulesList, final String noModule)
    {
        this.myProject = project;
        this.myModulesList = modulesList;
        new ComboboxSpeedSearch(modulesList)
        {
            protected String getElementText(Object element)
            {
                if ((element instanceof Module)) {
                    return ((Module)element).getName();
                }
                if (element == null) {
                    return noModule;
                }
                return super.getElementText(element);
            }
        };
        this.myModulesList.setModel(new SortedComboBoxModel(ModulesAlphaComparator.INSTANCE));
        this.myModulesList.setRenderer(new ModuleListCellRenderer(noModule));
    }

    public void applyTo(ModuleBasedConfiguration configurationModule)
    {
        configurationModule.setModule((Module)this.myModulesList.getSelectedItem());
    }

    public void reset(ModuleBasedConfiguration configuration)
    {
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        List<Module> list = new ArrayList();
        for (Module module : modules) {
            if (isModuleAccepted(module)) {
                list.add(module);
            }
        }
        setModules(list);
        this.myModulesList.setSelectedItem(configuration.getConfigurationModule().getModule());
    }

    public boolean isModuleAccepted(Module module)
    {
        return ModuleTypeManager.getInstance().isClasspathProvider(ModuleType.get(module));
    }

    public Project getProject()
    {
        return this.myProject;
    }

    public JavaRunConfigurationModule getConfigurationModule()
    {
        JavaRunConfigurationModule configurationModule = new JavaRunConfigurationModule(getProject(), false);
        configurationModule.setModule(getModule());
        return configurationModule;
    }

    private void setModules(Collection<Module> modules)
    {
        if ((this.myModulesList instanceof ModulesComboBox))
        {
            ((ModulesComboBox)this.myModulesList).setModules(modules);
        }
        else
        {
            SortedComboBoxModel<Module> model = (SortedComboBoxModel)this.myModulesList.getModel();
            model.setAll(modules);
            model.add(null);
        }
    }

    public Module getModule()
    {
        return (Module)this.myModulesList.getSelectedItem();
    }

    @Nullable
    public PsiClass findClass(String className)
    {
        return getConfigurationModule().findClass(className);
    }

    public String getModuleName()
    {
        Module module = getModule();
        return module == null ? "" : module.getName();
    }
}
