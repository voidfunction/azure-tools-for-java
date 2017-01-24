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
package com.microsoft.azure.docker.ops;

import com.jcraft.jsch.*;
import com.microsoft.azure.docker.model.AzureDockerException;
import com.microsoft.azure.docker.model.DockerHost;

import java.io.*;
import java.net.URI;

public class AzureDockerSSHOps {
  public static Session createLoginInstance(DockerHost dockerHost) {
    if (dockerHost == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }

    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(dockerHost.certVault.vmUsername, dockerHost.hostVM.dnsName);
      session.setPassword(dockerHost.certVault.vmPwd);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      return session;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String executeCommand(String command, Session session, Boolean getExitStatus) {
    String result = "";
    try {
      Channel channel = session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);
      InputStream commandOutput = channel.getInputStream();
      channel.connect();
      byte[] tmp  = new byte[1024];
      while(true){
        while(commandOutput.available()>0){
          int i=commandOutput.read(tmp, 0, 1024);
          if(i<0)break;
          result += new String(tmp, 0, i);
        }
        if(channel.isClosed()){
          if(commandOutput.available()>0) continue;
          if (getExitStatus)
            result += "exit-status: "+channel.getExitStatus();
          break;
        }
        try{Thread.sleep(100);}catch(Exception ee){}
      }
      channel.disconnect();

      return result;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String download(String fileName, String fromPath, Session session) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BufferedOutputStream buff = new BufferedOutputStream(outputStream);
      channel.cd(fromPath);
      channel.get(fileName, buff);

      channel.disconnect();

      return outputStream.toString();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void download(String fileName, String fromPath, String toPath, Session session) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      File toFile = new File(toPath, fileName);
      OutputStream outputStream = new FileOutputStream(toFile);
      BufferedOutputStream buff = new BufferedOutputStream(outputStream);
      channel.cd(fromPath);
      channel.get(fileName, buff);

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void upload(InputStream from, String fileName, String toPath, Session session) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      channel.cd(toPath);
      channel.put(from, fileName);

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void upload(String fileName, String fromPath, String toPath, Session session) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      FileInputStream inputStream = new FileInputStream(fromPath + File.separator + fileName);
      channel.cd(toPath);
      channel.put(inputStream, fileName);

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }
}
