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
package com.microsoft.azure.docker.ops.utils;

public class AzureDockerVMSetupScriptsForUbuntu {
  public static final String INSTALL_DOCKER_FOR_UBUNTU =
      "sudo apt-get update \n" +
          "sudo apt-get -y install docker.io \n" +
          "sudo groupadd docker \n" +
          "sudo usermod -aG docker $USER \n";

  public static final String DOCKER_API_PORT_TLS_DISABLED = "2375";
  public static final String DOCKER_API_PORT_TLS_ENABLED = "2376";

  /* Bash script that creates a default unsecured Docker configuration file; must be run on the Docker host after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$DOCKER_HOST_PORT_PARAM$" - TCP port to be opened for communicating with Docker API
   */
  public static final String CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED =
      "sudo service docker stop \n" +
          "mkdir ~/.azuredocker \n" +
          "sudo echo DOCKER_OPTS=\\\"--tls=false -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config \n" +
          "sudo cp -f ~/docker.config /etc/default/docker \n" +
          "sudo service docker start \n";

  /* Bash script that creates a default TLS secured Docker configuration file; must be run on the Docker host after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$DOCKER_API_PORT_PARAM$" - TCP port to be opened for communicating with Docker API
   */
  public static final String CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED =
      "sudo service docker stop \n" +
          "sudo echo DOCKER_OPTS=\\\"--tlsverify --tlscacert=/etc/docker/tls/ca.pem --tlscert=/etc/docker/tls/server.pem --tlskey=/etc/docker/tls/server-key.pem -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config \n" +
          "sudo cp -f ~/docker.config /etc/default/docker \n" +
          "sudo service docker start \n";

  public static final String DOCKER_API_PORT_PARAM = "[$]DOCKER_API_PORT_PARAM[$]";

  /* Bash script that creates the TLS certs; must be run on the Docker host after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$CERT_CA_PWD_PARAM$" - some randomly generated password
   *  "$HOSTNAME$" - Docker host name
   *  "$FQDN_PARAM$" - fully qualified name of the Docker host
   *  "$DNS_PARAM$" - domain of the Docker host
   */
  public static final String CERT_CA_PWD_PARAM = "[$]CERT_CA_PWD_PARAM[$]";
  public static final String HOSTNAME = "[$]HOSTNAME[$]";
  public static final String FQDN_PARAM = "[$]FQDN_PARAM[$]";
  public static final String DOMAIN_PARAM = "[$]DOMAIN_PARAM[$]";
  public static final String CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU =
      "mkdir ~/.azuredocker \n" +
          "mkdir ~/.azuredocker/tls \n" +
          "cd ~/.azuredocker/tls \n" +
          // Generate CA certificate
          "openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -aes256 -out ca-key.pem 2048 \n" +
          // Generate Server certificates
          "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=Docker Host CA/C=US' -new -x509 -days 365 -key ca-key.pem -sha256 -out ca.pem \n" +
          "openssl genrsa -out server-key.pem 2048 \n" +
//      "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=$HOSTNAME$' -sha256 -new -key server-key.pem -out server.csr \n" +
          "openssl req -subj '/CN=$HOSTNAME$' -sha256 -new -key server-key.pem -out server.csr \n" +
          "echo subjectAltName = DNS:$FQDN_PARAM$, DNS:*$DOMAIN_PARAM$, IP:127.0.0.1 > extfile.cnf \n" +
          "openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server.pem -extfile extfile.cnf \n" +
          // Generate Client certificates
          "openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -out key.pem \n" +
          "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=client' -new -key key.pem -out client.csr \n" +
          "echo extendedKeyUsage = clientAuth,serverAuth > extfile.cnf \n" +
          "openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile.cnf \n" +
          "cd ~\n";

  /* Bash script that sets up the TLS certificates to be used in a secured Docker configuration file; must be run on the Docker host after the VM is provisioned
   */
  public static final String INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU =
      "sudo mkdir /etc/docker \n" +
          "sudo mkdir /etc/docker/tls \n" +
          "sudo cp -f ~/.azuredocker/tls/ca.pem /etc/docker/tls/ca.pem \n" +
          "sudo cp -f ~/.azuredocker/tls/server.pem /etc/docker/tls/server.pem \n" +
          "sudo cp -f ~/.azuredocker/tls/server-key.pem /etc/docker/tls/server-key.pem \n" +
          "sudo chmod 644 /etc/docker/tls/* \n";

  private static final String GET_DOCKERHOST_TLSCACERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/ca.pem";
  private static final String GET_DOCKERHOST_TLSCERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/cert.pem";
  private static final String GET_DOCKERHOST_TLSCLIENTKEY_FOR_UBUNTU =
      "cat ~/.azuredockertls/key.pem";
  private static final String GET_DOCKERHOST_TLSSERVERCERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/server.pem";
  private static final String GET_DOCKERHOST_TLSSERVERKEY_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/server-key.pem";
}
