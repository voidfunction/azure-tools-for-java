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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.AzureDockerException;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.keyvault.SecretPermissions;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceNamer;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AzureDockerCertVaultOps {
  private static final String DELETE_SECRET = "*DELETE*";
  private static final String SECRETENTRY_DOCKERHOSTNAMES = "dockerhostnames";

  public static String generateUniqueKeyVaultName(String prefix) {
    return ResourceNamer.randomResourceName(prefix, 20);
  }

  private static Map<String, String> getSecretsMap(AzureDockerCertVault certVault) {
    if (certVault == null || certVault.hostName == null) return null;

    Map<String, String> secretsMap = new HashMap<>();
    secretsMap.put("vmUsername",     certVault.vmUsername != null ?     certVault.vmUsername : DELETE_SECRET);
    secretsMap.put("vmPwd",          certVault.vmPwd != null ?          certVault.vmPwd : DELETE_SECRET);
    secretsMap.put("sshKey",         certVault.sshKey != null ?         certVault.sshKey : DELETE_SECRET);
    secretsMap.put("sshPubKey",      certVault.sshPubKey != null ?      certVault.sshPubKey : DELETE_SECRET);
    secretsMap.put("tlsCACert",      certVault.tlsCACert != null ?      certVault.tlsCACert : DELETE_SECRET);
    secretsMap.put("tlsCAKey",       certVault.tlsCAKey != null ?       certVault.tlsCAKey : DELETE_SECRET);
    secretsMap.put("tlsClientCert",  certVault.tlsClientCert != null ?  certVault.tlsClientCert : DELETE_SECRET);
    secretsMap.put("tlsClientKey",   certVault.tlsClientKey != null ?   certVault.tlsClientKey : DELETE_SECRET);
    secretsMap.put("tlsServerCert",  certVault.tlsServerCert != null ?  certVault.tlsServerCert : DELETE_SECRET);
    secretsMap.put("tlsServerKey",   certVault.tlsServerKey != null ?   certVault.tlsServerKey : DELETE_SECRET);

    return secretsMap;
  }

  public static void createOrUpdateVault(Azure azureClient, AzureDockerCertVault certVault, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClient == null  || keyVaultClient == null || certVault == null ||
        certVault.name == null || certVault.hostName == null ||
        certVault.resourceGroupName == null || certVault.region == null ||
        (certVault.servicePrincipalId == null && certVault.userName == null)) {
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name, hostName, resourceGroupName, region and userName/servicePrincipalId cannot be null");
    }

    try {
      Vault vault;

      try {
        vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);
      } catch (CloudException e) {
        if (e.getBody().getCode().equals("ResourceNotFound") || e.getBody().getCode().equals("ResourceGroupNotFound")) {
          // Vault does not exist so this is a create op
          vault = azureClient.vaults()
              .define(certVault.name)
              .withRegion(certVault.region)
              .withNewResourceGroup(certVault.resourceGroupName)
              .withEmptyAccessPolicy()
              .create();
        } else {
          throw e;
        }
      }

      // Attempt to set permissions to the userName and/or servicePrincipalId identities
      // If login authority is a service principal, we might fail to set vault permissions
      try {
        setVaultPermissionsAll(azureClient, certVault);
      } catch (Exception e) {}

      // vault is not immediately available so the next operation could fail
      // add a retry policy to make sure it got created
      for (int retries = 0; retries <= 1500; retries = 100 + retries * 2 ) {
        try {
          keyVaultClient.listSecrets(vault.vaultUri());
          break;
        } catch (Exception e) {}
      }

      Map<String, String> secretsMap = getSecretsMap(certVault);
      boolean storeDockerHostNames = false;
      for( Map.Entry<String, String> entry : secretsMap.entrySet()) {
        SecretBundle secret;
        // check if secret exists
        try {
          secret = keyVaultClient.getSecret(vault.vaultUri(), entry.getKey());
        } catch (Exception e) {
          secret = null;
        }

        if (entry.getValue().equals(DELETE_SECRET)) {
          // delete the secret from keyvault
          if (secret != null) {
            keyVaultClient.deleteSecret(vault.vaultUri(), entry.getKey());
          }
        } else {
          keyVaultClient.setSecret(new SetSecretRequest.Builder(vault.vaultUri(), entry.getKey(), entry.getValue()).build());
          storeDockerHostNames = true;
        }
      }

      SecretBundle secretDockerHostNames;
      try {
        secretDockerHostNames = keyVaultClient.getSecret(vault.vaultUri(), SECRETENTRY_DOCKERHOSTNAMES);
      } catch (Exception e) {
        secretDockerHostNames = null;
      }
      if (storeDockerHostNames) {
        String secretValue = (secretDockerHostNames != null) ? secretDockerHostNames.value() : certVault.hostName;
        if (!secretValue.contains(certVault.hostName)) {
          // hostname is not registered
          secretValue += " " + certVault.hostName;
        }
        keyVaultClient.setSecret(new SetSecretRequest.Builder(vault.vaultUri(), SECRETENTRY_DOCKERHOSTNAMES, secretValue).build());
      } else {
        keyVaultClient.deleteSecret(vault.vaultUri(), SECRETENTRY_DOCKERHOSTNAMES);
      }

    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static void setVaultPermissionsAll(Azure azureClient, AzureDockerCertVault certVault) throws AzureDockerException {
    if (azureClient == null || certVault.name == null || certVault.resourceGroupName == null ||
        (certVault.servicePrincipalId == null && certVault.userName == null)){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name, resourceGroupName and userName/servicePrincipalId cannot be null");
    }

    try {
      Vault vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);

      if (certVault.servicePrincipalId != null) {
        vault.update()
            .defineAccessPolicy()
              .forServicePrincipal(certVault.servicePrincipalId)
              .allowSecretAllPermissions()
              .attach()
            .apply();
      }

      if (certVault.userName != null) {
        vault.update()
            .defineAccessPolicy()
            .forUser(certVault.userName)
            .allowSecretAllPermissions()
            .attach()
            .apply();
      }

    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static void setVaultPermissionsRead(Azure azureClient, AzureDockerCertVault certVault) throws AzureDockerException {
    if (azureClient == null || certVault.name == null || certVault.resourceGroupName == null ||
        (certVault.servicePrincipalId == null && certVault.userName == null)){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name, resourceGroupName and userName/servicePrincipalId cannot be null");
    }

    try {
      Vault vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);

      if (certVault.userName != null) {
        vault.update()
            .defineAccessPolicy()
              .forUser(certVault.userName)
              .allowSecretPermissions(SecretPermissions.LIST)
              .allowSecretPermissions(SecretPermissions.GET)
              .attach()
            .apply();
      }

      if (certVault.servicePrincipalId != null) {
        vault.update()
            .defineAccessPolicy()
              .forServicePrincipal(certVault.servicePrincipalId)
              .allowSecretPermissions(SecretPermissions.LIST)
              .allowSecretPermissions(SecretPermissions.GET)
              .attach()
            .apply();
      }
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault getVault(Azure azureClient, String name, String resourceGroupName, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClient == null || keyVaultClient == null || name == null || resourceGroupName == null) {
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name and resourceGroupName cannot be null");
    }

    AzureDockerCertVault tempVault = new AzureDockerCertVault();
    tempVault.name = name;
    tempVault.resourceGroupName = resourceGroupName;

    return getVault(azureClient, tempVault, keyVaultClient);
  }

  public static AzureDockerCertVault getVault(Azure azureClient, AzureDockerCertVault certVault, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClient == null || certVault == null || keyVaultClient == null ||
        certVault.name == null || certVault.resourceGroupName == null){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name and resourceGroupName cannot be null");
    }

    try {
      Vault vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), SECRETENTRY_DOCKERHOSTNAMES);
        if (secret != null) certVault.hostName = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "vmUsername");
        if (secret != null) certVault.vmUsername = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "vmPwd");
        if (secret != null) certVault.vmPwd = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "sshKey");
        if (secret != null) certVault.sshKey = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "sshPubKey");
        if (secret != null) certVault.sshPubKey = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsCACert");
        if (secret != null) certVault.tlsCACert = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsCAKey");
        if (secret != null) certVault.tlsCAKey = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsClientCert");
        if (secret != null) certVault.tlsClientCert = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsClientKey");
        if (secret != null) certVault.tlsClientKey = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsServerCert");
        if (secret != null) certVault.tlsServerCert = secret.value();
      } catch (Exception e){}

      try {
        SecretBundle secret = keyVaultClient.getSecret(vault.vaultUri(), "tlsServerKey");
        if (secret != null) certVault.tlsServerKey = secret.value();
      } catch (Exception e){}

    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return certVault;
  }

  public static void deleteVault(Azure azureClient, AzureDockerCertVault certVault) throws AzureDockerException {
    if (azureClient == null || certVault.name == null || certVault.resourceGroupName == null){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name and resourceGroupName cannot be null");
    }

    try {
      azureClient.vaults().deleteByGroup(certVault.resourceGroupName, certVault.name);

    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static void copyVault(Azure azureClientSource, AzureDockerCertVault certVaultSource, Azure azureClientDest, AzureDockerCertVault certVaultDest, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClientSource == null || certVaultSource == null || certVaultSource.name == null || certVaultSource.resourceGroupName == null ||
        azureClientDest == null || certVaultDest == null || certVaultDest.name == null || certVaultDest.resourceGroupName == null || certVaultDest.region == null ||
        (certVaultDest.servicePrincipalId == null && certVaultDest.userName == null) || keyVaultClient == null ){
        throw new AzureDockerException("Unexpected argument values; azureClient, vault name, hostName, resourceGroupName and destination region and userName/servicePrincipalId cannot be null");
    }

    try {
      AzureDockerCertVault certVaultResult = getVault(azureClientSource, certVaultSource, keyVaultClient);
      certVaultResult.name = certVaultDest.name;
      certVaultResult.resourceGroupName = certVaultDest.resourceGroupName;
      certVaultResult.region = certVaultDest.region;
      certVaultResult.userName = certVaultDest.userName;
      certVaultResult.servicePrincipalId = certVaultDest.servicePrincipalId;

      createOrUpdateVault(azureClientDest, certVaultResult, keyVaultClient);
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault getSSHKeysFromLocalFile(String localPath) throws AzureDockerException {
    AzureDockerCertVault certVault = new AzureDockerCertVault();

    try {
      certVault.sshKey         = new String(Files.readAllBytes(Paths.get(localPath + "/id_rsa")));
      certVault.sshPubKey      = new String(Files.readAllBytes(Paths.get(localPath + "/id_rsa.pub")));
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return certVault;
  }

  public static AzureDockerCertVault getTLSCertsFromLocalFile(String localPath) throws AzureDockerException {
    AzureDockerCertVault certVault = new AzureDockerCertVault();

    try {
      certVault.tlsCACert      = new String(Files.readAllBytes(Paths.get(localPath + "/ca.pem")));
      certVault.tlsCACert      = new String(Files.readAllBytes(Paths.get(localPath + "/ca-key.pem")));
      certVault.tlsClientCert  = new String(Files.readAllBytes(Paths.get(localPath + "/cert.pem")));
      certVault.tlsClientKey   = new String(Files.readAllBytes(Paths.get(localPath + "/key.pem")));
      certVault.tlsServerCert  = new String(Files.readAllBytes(Paths.get(localPath + "/server.pem")));
      certVault.tlsServerKey   = new String(Files.readAllBytes(Paths.get(localPath + "/server-key.pem")));
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return certVault;
  }

  public static void saveToLocalFiles(String localPath, AzureDockerCertVault certVault) throws AzureDockerException {
    try {
      if(certVault.sshKey != null)         { FileWriter file = new FileWriter(localPath + "/id_rsa");         file.write(certVault.sshKey);         file.close();}
      if(certVault.sshPubKey != null)      { FileWriter file = new FileWriter(localPath + "/id_rsa.pub");     file.write(certVault.sshPubKey);      file.close();}
      if(certVault.tlsCACert != null)      { FileWriter file = new FileWriter(localPath + "/ca.pem");         file.write(certVault.tlsCACert);      file.close();}
      if(certVault.tlsCAKey != null)       { FileWriter file = new FileWriter(localPath + "/ca-key.pem");     file.write(certVault.tlsCACert);      file.close();}
      if(certVault.tlsClientCert != null)  { FileWriter file = new FileWriter(localPath + "/cert.pem");       file.write(certVault.tlsClientCert);  file.close();}
      if(certVault.tlsClientKey != null)   { FileWriter file = new FileWriter(localPath + "/key.pem");        file.write(certVault.tlsClientKey);   file.close();}
      if(certVault.tlsServerCert != null)  { FileWriter file = new FileWriter(localPath + "/server.pem");     file.write(certVault.tlsServerCert);  file.close();}
      if(certVault.tlsServerKey != null)   { FileWriter file = new FileWriter(localPath + "/server-key.pem"); file.write(certVault.tlsServerKey);   file.close();}
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault generateSSHCerts(String passPhrase, String comment) throws AzureDockerException{
    try {
      AzureDockerCertVault result = new AzureDockerCertVault();
      JSch jsch = new JSch();
      KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
      ByteArrayOutputStream privateKeyBuff = new ByteArrayOutputStream(2048);
      ByteArrayOutputStream publicKeyBuff = new ByteArrayOutputStream(2048);

      keyPair.writePublicKey(publicKeyBuff, (comment != null) ? comment : "DockerSSHCerts");

      if (passPhrase == null  || passPhrase.isEmpty()) {
        keyPair.writePrivateKey(privateKeyBuff);
      } else {
        keyPair.writePrivateKey(privateKeyBuff, passPhrase.getBytes());
      }

      result.sshKey = privateKeyBuff.toString();
      result.sshPubKey = publicKeyBuff.toString();

      return result;
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault generateTLSCerts(String passPhrase) {
    AzureDockerCertVault result = new AzureDockerCertVault();

    return result;
  }

}
