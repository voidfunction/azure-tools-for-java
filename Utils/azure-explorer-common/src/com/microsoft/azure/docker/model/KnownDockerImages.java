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
  TOMCAT8("tomcat:8.0.20-jre8", TOMCAT8_DEFAULT_DOCKERFILE, "8080", false, null),
  TOMCAT8_DEBUG("tomcat:8.0.20-jre8 Debug Enabled Port 5005", TOMCAT8_DEBUG_DOCKERFILE, "8080", false, "5005"),
  JBOSS_WILDFLY("JBoss WildFly", JBOSS_WILDFLY_DEFAULT_DOCKERFILE, "8080", false, null),
  JBOSS_WILDFLY_DEBUG("JBoss WildFly Debug Enabled Port 8787", JBOSS_WILDFLY_DEBUG_DOCKERFILE, "8080", false, "8787"),
  OPENSDK_7("OpenSDK 7", OPENSDK_7_DEFAULT_DOCKERFILE, "80", true, null),
  OPENSDK_8("OpenSDK 8", OPENSDK_8_DEFAULT_DOCKERFILE, "80", true, null),
  OPENSDK_9("OpenSDK 9", OPENSDK_9_DEFAULT_DOCKERFILE, "80", true, null),
  OPENSDK_LATEST("OpenSDK Latest", OPENSDK_LATEST_DEFAULT_DOCKERFILE, "80", true, null),
  OPENSDK_7_DEBUG("OpenSDK 7 Debug Enabled Port 5005", OPENSDK_7_DEBUG_DOCKERFILE, "80", true, "5005"),
  OPENSDK_8_DEBUG("OpenSDK 8 Debug Enabled Port 5005", OPENSDK_8_DEBUG_DOCKERFILE, "80", true, "5005"),
  OPENSDK_9_DEBUG("OpenSDK 9 Debug Enabled Port 5005", OPENSDK_9_DEBUG_DOCKERFILE, "80", true, "5005"),
  OPENSDK_LATEST_DEBUG("OpenSDK Latest Debug Enabled Port 5005", OPENSDK_LATEST_DEBUG_DOCKERFILE, "80", true, "5005");

  private final String dockerfileContent;
  private final String name;
  private final String portSettings;
  private final String debugPortSettings;
  private final boolean canRunJarFile;

  public final static String DOCKER_ARTIFACT_FILENAME = "[$]DOCKER_ARTIFACT_FILENAME[$]";

  KnownDockerImages(String name, String dockerFile, String defaultPortSettings, boolean canRunJarFile, String debugPortSettings) {
    this.dockerfileContent = dockerFile;
    this.name = name;
    this.portSettings = defaultPortSettings;
    this.canRunJarFile = canRunJarFile;
    this.debugPortSettings = debugPortSettings;
  }

  public String toString(){
    return name;
  }
  public String getName() { return name;}
  public String getPortSettings() { return portSettings;}
  public String getDockerfileContent() { return  dockerfileContent;}
  public String getDebugPortSettings() { return debugPortSettings;}
  public boolean isCanRunJarFile() { return canRunJarFile;}

  public static class KnownDefaultDockerfiles {
    public static final String JBOSS_WILDFLY_DEFAULT_DOCKERFILE =
        "FROM jboss/wildfly\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /opt/jboss/wildfly/standalone/deployments/\n";
    public static final String JBOSS_WILDFLY_DEBUG_DOCKERFILE =
        "FROM jboss/wildfly\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /opt/jboss/wildfly/standalone/deployments/\n" +
            "EXPOSE 8787\n" +
            "CMD [\"/opt/jboss/wildfly/bin/standalone.sh\", \"--debug\", \"-b\", \"0.0.0.0\"]\n";
    public static final String TOMCAT8_DEFAULT_DOCKERFILE =
        "FROM tomcat:8.0.20-jre8\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /usr/local/tomcat/webapps/\n";
    public static final String TOMCAT8_DEBUG_DOCKERFILE =
        "FROM tomcat:8.0.20-jre8\n" +
            "ENV JPDA_ADDRESS=5005\n" +
            "ENV JPDA_TRANSPORT=dt_socket\n" +
            "EXPOSE 5005\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /usr/local/tomcat/webapps/\n";
    public static final String OPENSDK_7_DEFAULT_DOCKERFILE =
        "FROM openjdk:7\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_7_DEBUG_DOCKERFILE =
        "FROM openjdk:7\n" +
            "EXPOSE 5005\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-Xdebug\", \"-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_8_DEFAULT_DOCKERFILE =
        "FROM openjdk:8\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_8_DEBUG_DOCKERFILE =
        "FROM openjdk:8\n" +
            "EXPOSE 5005\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-Xdebug\", \"-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_9_DEFAULT_DOCKERFILE =
        "FROM openjdk:9\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_9_DEBUG_DOCKERFILE =
        "FROM openjdk:9\n" +
            "EXPOSE 5005\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-Xdebug\", \"-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_LATEST_DEFAULT_DOCKERFILE =
        "FROM openjdk:latest\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "EXPOSE 80\n" +
            "CMD [\"java\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
    public static final String OPENSDK_LATEST_DEBUG_DOCKERFILE =
        "FROM openjdk:latest\n" +
            "EXPOSE 5005\n" +
            "EXPOSE 80\n" +
            "ADD [$]DOCKER_ARTIFACT_FILENAME[$] /home/\n" +
            "CMD [\"java\", \"-Xdebug\", \"-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005\", \"-jar\", \"/home/[$]DOCKER_ARTIFACT_FILENAME[$]\"]\n";
  }
}
