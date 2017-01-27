/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.docker.model;

import static com.microsoft.azure.docker.model.KnownDockerImages.KnownDefaultDockerfiles.*;

public enum KnownDockerImages {
  JBOSS_WILDFLY("JBoss WildFly", JBOSS_WILDFLY_DEFAULT_DOCKERFILE, "18080:80"),
  TOMCAT8("tomcat:8.0.20-jre8", TOMCAT8_DEFAULT_DOCKERFILE, "18080:80");

  private final String dockerfileContent;
  private final String name;
  private final String portSettings;

  KnownDockerImages(String name, String dockerFile, String defaultPortSettings) {
    this.dockerfileContent = dockerFile;
    this.name = name;
    this.portSettings = defaultPortSettings;
  }

  public String toString(){
    return name;
  }

  public String getPortSettings() {return portSettings;}
  public String getDockerfileContent() {return  dockerfileContent;}

  public static class KnownDefaultDockerfiles {
    public static final String JBOSS_WILDFLY_DEFAULT_DOCKERFILE =
        "FROM jboss/wildfly\n" +
            "ADD _MyArtifact.war_ /opt/jboss/wildfly/standalone/deployments/\n";
    public static final String TOMCAT8_DEFAULT_DOCKERFILE =
        "FROM tomcat:8.0.20-jre8\n" +
            "ADD _MyArtifact.war_ /usr/local/tomcat/webapps/\n";
  }
}
