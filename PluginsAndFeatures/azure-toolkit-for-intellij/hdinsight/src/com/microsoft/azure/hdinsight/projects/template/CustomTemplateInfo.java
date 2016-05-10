package com.microsoft.azure.hdinsight.projects.template;

import com.microsoft.tooling.msservices.helpers.StringHelper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.List;

@XmlRootElement(name = "Template")
public class CustomTemplateInfo {

    @XmlAttribute(name = "TemplateName")
    private String name;

    @XmlElement(name = "IconPath")
    private String iconPath;

    @XmlElement(name = "Description")
    private String description;

    @XmlElementWrapper(name = "SourceFilePaths")
    @XmlElement(name = "Path")
    private List<String> sourceFiles;

    @XmlElementWrapper(name = "DependencyLibraryPaths")
    @XmlElement(name = "Path")
    private List<String> dependencyFiles;

    @XmlElement(name = "IsNeedSparkSDK")
    private boolean isNeedSparkSDK;

    @XmlElement(name = "IsNeedScalaSDK")
    private boolean isNeedScalaSDK;

    @XmlElement(name = "IsSparkProject")
    private boolean isSparkProject;

    public CustomTemplateInfo() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return getTemplateRootPath() + iconPath;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    public List<String> getDependencyFiles() {
        return dependencyFiles;
    }

    public boolean isNeedSparkSDK() {
        return isNeedSparkSDK;
    }

    public boolean isNeedScalaSDK() {
        return isNeedScalaSDK;
    }

    public boolean isSparkProject() {
        return isSparkProject;
    }

    private String getTemplateRootPath() {
        return StringHelper.concat(TemplatesUtil.getTemplateRootFolderPath(), File.separator, name);
    }
}
