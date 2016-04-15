package com.microsoft.azure.hdinsight.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoftopentechnologies.auth.browser.BrowserLauncher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
    Temporary Fix: Cannot login to Azure and get cluster on Ubuntu15/Fedora23
    Load jar package manually
 */
public class DefaultLinuxBrowserLauncher implements BrowserLauncher {

    private static final String pluginRoot = PluginUtil.getPluginRootDirectory();
    private static final String rootPath =  pluginRoot + File.separator + "lib";
    private static final String attachPath = pluginRoot + File.separator + "attach";
    private String dependenciesPath = StringHelper.concat(rootPath, "/gson-2.4.jar:", attachPath, "/commons-codec-1.6.jar:", rootPath, "/guava-16.0.jar:");

    @Override
    public ListenableFuture<Void> browseAsync(String url, String redirectUrl, String callbackUrl, String windowTitle, boolean noShell) {
        List<String> args = new ArrayList<>();

        // fetch path to the currently running JVM
        File javaHome = new File(System.getProperty("java.home"));
        File javaExecutable = new File(javaHome, "bin" + File.separator + "java");
        args.add(javaExecutable.getAbsolutePath());
        boolean isX64 = System.getProperty("os.arch").toLowerCase().contains("64");

        if(isX64) {
            args.add("-d64");
            dependenciesPath += attachPath + "/AzureLoginLinuxX64.jar";
        } else {
            args.add("-d32");
            dependenciesPath += attachPath + "/AzureLoginLinuxX86.jar";
        }

        args.add("-cp");
        args.add(dependenciesPath);
        args.add("com.microsoftopentechnologies.adinteractiveauth.Program");
        args.add(url);
        args.add(redirectUrl);
        args.add(callbackUrl);
        args.add(windowTitle);
        // process should exit after sign in is complete
        args.add("true");
        args.add(String.valueOf(noShell));

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.start();
            return Futures.immediateFuture(null);
        }catch (IOException e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}
