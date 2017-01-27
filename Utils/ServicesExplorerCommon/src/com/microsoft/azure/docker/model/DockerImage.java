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

import java.util.List;

public class DockerImage {
  public String name;
  public String id;
  public String repository;
  public String tag;
  public long virtualSize;
  public String artifactName;    // .war or .jar output file path representing the application to be deployed and run
  public String ports;           // container᾿s port or a range of ports to the host to be published (i.e. "1234-1236:1234-1236/tcp")
  public String dockerfile;      // Dockerfile input from which the image will be created
  public String imageBase;       // see FROM directive
  public String exposeCMD;       // see EXPOSE directive
  public List<String> addCMDs;   // see ADD directive
  public List<String> runCMDs;   // see RUN directive
  public List<String> copyCMDs;  // see COPY directive
  public List<String> envCMDs;   // see ENV directive
  public List<String> workCMDs;  // see WORK directive

  public DockerHost dockerHost;  // parent Docker host

  public DockerImage() {}

  public DockerImage(String name, String customContent, String ports, String artifactName) {
    this.name = name;
    this.dockerfile = customContent;
    this.ports = ports;
    this.artifactName = artifactName;
  }
}
