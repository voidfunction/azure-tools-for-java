package com.microsoft.azure.hdinsight.projects.template;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.projects.samples.ProjectSampleUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.StringHelper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TemplatesUtil {
    private static final String TEMPLATE_ROOT_FOLDER_NAME = "templates";
    private static final String XML_FILE_NAME = "template.xml";

    private static JAXBContext jaxbContext = null;
    private static Unmarshaller unmarshaller = null;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(CustomTemplateInfo.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CustomHDInsightTemplateItem> getCustomTemplate() {
        List<CustomHDInsightTemplateItem> items = new ArrayList<>();
        File[] templatesFiles = getAllTemplateFolders();
        if (templatesFiles == null) {
            return items;
        }

        for (int i = 0; i < templatesFiles.length; ++i) {
            CustomTemplateInfo info = getTemplateInfo(templatesFiles[i]);
            if (info != null) {
                items.add(new CustomHDInsightTemplateItem(info));
            }
        }

        return items;
    }

    public static File[] getAllTemplateFolders() {
        final File file = getTemplateRootFolder();
        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    File[] files = pathname.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.equals(XML_FILE_NAME);
                        }
                    });

                    if (files.length == 1) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public static String getTemplateRootFolderPath() {
        return getTemplateRootFolder().getAbsolutePath();
    }

    public static void createTemplateSampleFiles(Module module, CustomTemplateInfo info) {
        List<String> sourceFiles = info.getSourceFiles();
        String sourceRootPath = ProjectSampleUtil.getRootOrSourceFolder(module, true);

        if (sourceRootPath != null) {
            for (int i = 0; i < sourceFiles.size(); ++i) {
                try {
                    FileUtil.copy(new File(TemplatesUtil.getTemplateRootFolderPath() + sourceFiles.get(i)), new File(sourceRootPath + sourceFiles.get(i)));
                } catch (IOException e) {

                }
            }
        }
    }

    private static File getTemplateRootFolder() {
        File templateRoot = new File(StringHelper.concat(HDInsightUtil.getPluginRootDirectory(), File.separator, TEMPLATE_ROOT_FOLDER_NAME));
        return templateRoot;
    }

    private static CustomTemplateInfo getTemplateInfo(File file) {
        File templateInfoFile = new File(StringHelper.concat(file.getAbsolutePath(), File.separator, XML_FILE_NAME));
        if (templateInfoFile == null) {
            return null;
        }

        CustomTemplateInfo info = null;
        try {
            info = (CustomTemplateInfo) unmarshaller.unmarshal(templateInfoFile);
        } catch (JAXBException e) {
            DefaultLoader.getUIHelper().showException("Failed to get template info from template.xml", e, "Custom Template", true, true);
        }
        return info;
    }
}
