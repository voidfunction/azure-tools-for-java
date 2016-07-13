/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azureexplorer.hdinsight;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azure.hdinsight.jobs.JobUtils;
import com.microsoft.azureexplorer.Activator;

public class FxClassLoader {
	
	private static URLClassLoader createSWTFXClassLoader(ClassLoader parent, String jobViewUrl) throws Exception {
		File javaHome = null; 
		try {
			javaHome = new File (System.getProperty("java.home")).getCanonicalFile(); 
		} catch (IOException e) {
			throw new IllegalStateException("Unable to get java home", e); 
		}
		if (!javaHome.exists()) {
			throw new IllegalStateException("Java home \"" + javaHome.getAbsolutePath() + "\" doesn't exits");
		}
		
		File swtFxFile = new File(new File(javaHome.getAbsolutePath(),"lib"),"jfxswt.jar");
		File jobViewFile = new File(jobViewUrl);
		if( swtFxFile.exists() && jobViewFile.exists()) {
			return new URLClassLoader(new URL[] {swtFxFile.getCanonicalFile().toURI().toURL(), jobViewFile.getCanonicalFile().toURI().toURL() }, parent);							
		}
		return null;
	}
	
	private static String JOB_VIEW_FX_UTILS_NAME = "com.microsoft.hdinsight.jobs.JobViewFxUtil";
	
	public static void loadJavaFxForJobView(Composite composite, String url, String jobViewUrl) {
		try {
			ClassLoader myClassLoader = createSWTFXClassLoader(FxClassLoader.class.getClassLoader(), jobViewUrl);
            Class jobViewFxUtilslCLass = Class.forName(JOB_VIEW_FX_UTILS_NAME, true, myClassLoader);
            Method method = jobViewFxUtilslCLass.getMethod("startFx", Object.class, String.class, Object.class);
            Object object = method.invoke(null, composite, url, new JobUtils());
		} catch (Exception e) {
			Activator.getDefault().log("HDInsight: load JavaFx error", e);
		}
		
	} 
}
