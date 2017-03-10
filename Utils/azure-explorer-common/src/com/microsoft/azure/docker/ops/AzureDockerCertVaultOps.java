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
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.keyvault.SecretPermissions;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.rest.ServiceCallback;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;

public class AzureDockerCertVaultOps {
//  private static final String DELETE_SECRET = "*DELETE*";
  private static final String SECRETENTRY_DOCKERHOSTNAMES = "dockerhostnames";
  private static final String[] DOCKERHOST_SECRETS = new String[]{
      "vmUsername",
      "vmPwd",
      "sshKey",
      "sshPubKey",
      "tlsCACert",
      "tlsCAKey",
      "tlsClientCert",
      "tlsClientKey",
      "tlsServerCert",
      "tlsServerKey"
  };

//  private static Map<String, String> getSecretsUpdateMap(AzureDockerCertVault certVault) {
//    if (certVault == null) return null;
//    Map<String, String> secretsUpdateMap = new HashMap<>();
//    secretsUpdateMap.put("vmUsername",     certVault.vmUsername != null ?     certVault.vmUsername : DELETE_SECRET);
//    secretsUpdateMap.put("vmPwd",          certVault.vmPwd != null ?          certVault.vmPwd : DELETE_SECRET);
//    secretsUpdateMap.put("sshKey",         certVault.sshKey != null ?         certVault.sshKey : DELETE_SECRET);
//    secretsUpdateMap.put("sshPubKey",      certVault.sshPubKey != null ?      certVault.sshPubKey : DELETE_SECRET);
//    secretsUpdateMap.put("tlsCACert",      certVault.tlsCACert != null ?      certVault.tlsCACert : DELETE_SECRET);
//    secretsUpdateMap.put("tlsCAKey",       certVault.tlsCAKey != null ?       certVault.tlsCAKey : DELETE_SECRET);
//    secretsUpdateMap.put("tlsClientCert",  certVault.tlsClientCert != null ?  certVault.tlsClientCert : DELETE_SECRET);
//    secretsUpdateMap.put("tlsClientKey",   certVault.tlsClientKey != null ?   certVault.tlsClientKey : DELETE_SECRET);
//    secretsUpdateMap.put("tlsServerCert",  certVault.tlsServerCert != null ?  certVault.tlsServerCert : DELETE_SECRET);
//    secretsUpdateMap.put("tlsServerKey",   certVault.tlsServerKey != null ?   certVault.tlsServerKey : DELETE_SECRET);
//
//    return secretsUpdateMap;
//  }

  private static Map<String, String> getSecretsMap(AzureDockerCertVault certVault) {
    if (certVault == null || certVault.hostName == null) return null;

    Map<String, String> secretsMap = new HashMap<>();
    secretsMap.put("vmUsername",     certVault.vmUsername);
    secretsMap.put("vmPwd",          certVault.vmPwd);
    secretsMap.put("sshKey",         certVault.sshKey);
    secretsMap.put("sshPubKey",      certVault.sshPubKey);
    secretsMap.put("tlsCACert",      certVault.tlsCACert);
    secretsMap.put("tlsCAKey",       certVault.tlsCAKey);
    secretsMap.put("tlsClientCert",  certVault.tlsClientCert);
    secretsMap.put("tlsClientKey",   certVault.tlsClientKey);
    secretsMap.put("tlsServerCert",  certVault.tlsServerCert);
    secretsMap.put("tlsServerKey",   certVault.tlsServerKey);

    return secretsMap;
  }

  public static void createOrUpdateVault(Azure azureClient, AzureDockerCertVault certVault, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClient == null  || keyVaultClient == null || certVault == null ||
        certVault.name == null || certVault.hostName == null ||
        certVault.resourceGroupName == null || certVault.region == null ||
        (certVault.servicePrincipalId == null && certVault.userId == null)) {
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name, hostName, resourceGroupName, region and userName/servicePrincipalId cannot be null");
    }

    try {
      Vault vault;

      try {
        vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);
      } catch (CloudException e) {
        if (e.getBody().getCode().equals("ResourceNotFound") || e.getBody().getCode().equals("ResourceGroupNotFound")) {
          vault = null; // Vault does no exist
        } else {
          throw e;
        }
      }

      if (vault == null) {
        // Vault does not exist so this is the create op
        Vault.DefinitionStages.WithAccessPolicy withAccessPolicy = azureClient.vaults()
            .define(certVault.name)
            .withRegion(certVault.region)
            .withNewResourceGroup(certVault.resourceGroupName);

        Vault.DefinitionStages.WithCreate withCreate = certVault.servicePrincipalId != null ?
            withAccessPolicy.defineAccessPolicy()
                .forServicePrincipal(certVault.servicePrincipalId)
                .allowSecretAllPermissions()
                .attach() :
            withAccessPolicy.defineAccessPolicy()
                .forUser(certVault.userId)
                .allowSecretAllPermissions()
                .attach();

        withCreate
              .withTag("dockerhost", "true")
              .create();

      } else {
        // Attempt to set permissions to the userName and/or servicePrincipalId identities
        // If original owner is an AD user, we might fail to set vault permissions
        try {
          setVaultPermissionsAll(azureClient, certVault);
        } catch (Exception ignored) {
        }
      }

      vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);
      String vaultUri = vault.vaultUri();
      // vault is not immediately available so the next operation could fail
      // add a retry policy to make sure it got created and it is readable
      for (int sleepMs = 5000; sleepMs <= 200000; sleepMs += 5000 ) {
        try {
          keyVaultClient.listSecrets(vaultUri);
          break;
        } catch (Exception e) {
          try {
            if (DEBUG) System.out.format("WARN: can't find %s (sleepMs: %d)\n", vaultUri, sleepMs);
            if (DEBUG) System.out.println(e.getMessage());
            Thread.sleep(5000);
          } catch (Exception ignored) {
          }
        }
      }

      Map<String, String> secretsMap = getSecretsMap(certVault);
//      boolean storeDockerHostNames = false;

      //Execute Key Vault Secret Update in parallel
      Observable.from(DOCKERHOST_SECRETS).flatMap( secretName -> {
            return Observable.create(new Observable.OnSubscribe<SecretBundle>() {
              @Override
              public void call(Subscriber<? super SecretBundle> subscriber) {
                String secretValue = secretsMap.get(secretName);
                if (secretValue != null && !secretValue.isEmpty()) {
                  keyVaultClient.setSecretAsync(new SetSecretRequest.Builder(vaultUri, secretName, secretValue).build(),
                      new ServiceCallback<SecretBundle>() {
                        @Override
                        public void failure(Throwable throwable) {
                          subscriber.onError(throwable);
                        }

                        @Override
                        public void success(SecretBundle secretBundle) {
                          subscriber.onNext(secretBundle);
                          subscriber.onCompleted();
                        }
                      }
                  );
                }
              }
            }).subscribeOn(Schedulers.io());
          }
      ).toBlocking().subscribe();

      // TODO: remove this after enabling parallel secrets write from above
//      for( Map.Entry<String, String> entry : secretsMap.entrySet()) {
//        try {
//          if (entry.getValue() != null && !entry.getValue().isEmpty()) {
//            keyVaultClient.setSecret(new SetSecretRequest.Builder(vaultUri, entry.getKey(), entry.getValue()).build());
//            storeDockerHostNames = true;
//          }
//        } catch (Exception e) {
//          System.out.format("ERROR: can't write %s secret %s: %s\n", vaultUri, entry.getKey(), entry.getValue());
//          System.out.println(e.getMessage());
//        }
//      }

      if (keyVaultClient.listSecrets(vaultUri).size() > 0 && certVault.hostName != null && !certVault.hostName.isEmpty()) {
        keyVaultClient.setSecret(new SetSecretRequest.Builder(vaultUri, SECRETENTRY_DOCKERHOSTNAMES, certVault.hostName).build());
      } else {
        // something unexpected went wrong... delete the vault
        if (DEBUG) System.out.println("ERROR: something went wrong");
        //if (isNewVault) azureClient.vaults().deleteById(vault.id());
      }
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static void setVaultPermissionsAll(Azure azureClient, AzureDockerCertVault certVault) throws AzureDockerException {
    if (azureClient == null || certVault.name == null || certVault.resourceGroupName == null ||
        (certVault.servicePrincipalId == null && certVault.userId == null)){
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

      if (certVault.userId != null) {
        vault.update()
            .defineAccessPolicy()
            .forUser(certVault.userId)
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
        (certVault.servicePrincipalId == null && certVault.userId == null)){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name, resourceGroupName and userName/servicePrincipalId cannot be null");
    }

    try {
      Vault vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);

      if (certVault.userId != null) {
        vault.update()
            .defineAccessPolicy()
              .forUser(certVault.userId)
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
        certVault.name == null || certVault.resourceGroupName == null) {
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name and resourceGroupName cannot be null");
    }
    Vault vault;

    try {
      vault = azureClient.vaults().getByGroup(certVault.resourceGroupName, certVault.name);
      certVault.uri = vault.vaultUri();
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return getVault(certVault, keyVaultClient);
  }

  public static AzureDockerCertVault getVault(AzureDockerCertVault certVault, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (certVault == null || keyVaultClient == null || certVault.uri == null){
      throw new AzureDockerException("Unexpected argument values; azureClient, vault name and resourceGroupName cannot be null");
    }

    String vaultUri = certVault.uri;

    try {
      SecretBundle secret = keyVaultClient.getSecret(vaultUri, SECRETENTRY_DOCKERHOSTNAMES);
      if (secret != null) {
        certVault.hostName = secret.value();
      } else {
        certVault.hostName = null;
        return null;
      }
    } catch (Exception e){
      return null;
    }

    //Execute Key Vault Secret read in parallel
    Map<String, String> secretNamesAndValueMap = new HashMap<>();

    Observable.from(DOCKERHOST_SECRETS).flatMap( secretName -> {
          return Observable.create(new Observable.OnSubscribe<Pair<String, String>>() {
            @Override
            public void call(Subscriber<? super Pair<String, String>> subscriber) {
              keyVaultClient.getSecretAsync(vaultUri, secretName,
                  new ServiceCallback<SecretBundle>() {
                    @Override
                    public void failure(Throwable throwable) {
                      // subscriber.onError(throwable);
                      // ignore any errors due to the call trowing an unexpected exception
                      subscriber.onCompleted();
                    }

                    @Override
                    public void success(SecretBundle secretBundle) {
                      if (secretBundle != null) {
                        subscriber.onNext(new Pair<>(secretName, secretBundle.value()));
                      }
                      subscriber.onCompleted();
                    }
                  }
              );
            }
          }).subscribeOn(Schedulers.io());
        }
    , 5).subscribeOn(Schedulers.io())
        .toBlocking()
        .subscribe(new Action1<Pair<String, String>>() {
          @Override
          public void call(Pair<String, String> secretNameAndValue) {
            secretNamesAndValueMap.put(secretNameAndValue.first(), secretNameAndValue.second());
          }
        });

    String currentSecretValue;

    currentSecretValue = secretNamesAndValueMap.get("vmUsername");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.vmUsername = currentSecretValue;
    }

    currentSecretValue = secretNamesAndValueMap.get("vmPwd");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.vmPwd = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("sshKey");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.sshKey = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("sshPubKey");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.sshPubKey = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsCACert");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsCACert = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsCAKey");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsCAKey = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsClientCert");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsClientCert = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsClientKey");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsClientKey = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsServerCert");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsServerCert = currentSecretValue;
    }
    currentSecretValue = secretNamesAndValueMap.get("tlsServerKey");
    if (currentSecretValue != null && !currentSecretValue.isEmpty()) {
      certVault.tlsServerKey = currentSecretValue;
    }

//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "vmUsername");
//      certVault.vmUsername = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "vmPwd");
//      certVault.vmPwd = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "sshKey");
//      certVault.sshKey = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "sshPubKey");
//      certVault.sshPubKey = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsCACert");
//      certVault.tlsCACert = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsCAKey");
//      certVault.tlsCAKey = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsClientCert");
//      certVault.tlsClientCert = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsClientKey");
//      certVault.tlsClientKey = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsServerCert");
//      certVault.tlsServerCert = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}
//
//    try {
//      SecretBundle secret = keyVaultClient.getSecret(vaultUri, "tlsServerKey");
//      certVault.tlsServerKey = (secret != null) ? secret.value() : null;
//    } catch (Exception e){}

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

  public static void cloneVault(Azure azureClientSource, AzureDockerCertVault certVaultSource, Azure azureClientDest, AzureDockerCertVault certVaultDest, KeyVaultClient keyVaultClient) throws AzureDockerException {
    if (azureClientSource == null || certVaultSource == null || certVaultSource.name == null || certVaultSource.resourceGroupName == null ||
        azureClientDest == null || certVaultDest == null || certVaultDest.name == null || certVaultDest.resourceGroupName == null || certVaultDest.region == null ||
        (certVaultDest.servicePrincipalId == null && certVaultDest.userId == null) || keyVaultClient == null ){
        throw new AzureDockerException("Unexpected argument values; azureClient, vault name, hostName, resourceGroupName and destination region and userName/servicePrincipalId cannot be null");
    }

    try {
      AzureDockerCertVault certVaultResult = getVault(azureClientSource, certVaultSource, keyVaultClient);
      certVaultResult.name = certVaultDest.name;
      certVaultResult.resourceGroupName = certVaultDest.resourceGroupName;
      certVaultResult.region = certVaultDest.region;
      certVaultResult.userId = certVaultDest.userId;
      certVaultResult.servicePrincipalId = certVaultDest.servicePrincipalId;

      createOrUpdateVault(azureClientDest, certVaultResult, keyVaultClient);
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault copyVaultLoginCreds(AzureDockerCertVault certVaultDest, AzureDockerCertVault certVaultSource) {
    certVaultDest.vmUsername = certVaultSource.vmUsername;
    certVaultDest.vmPwd = certVaultSource.vmPwd;

    return certVaultDest;
  }

  public static AzureDockerCertVault copyVaultSshKeys(AzureDockerCertVault certVaultDest, AzureDockerCertVault certVaultSource) {
    certVaultDest.sshKey = certVaultSource.sshKey; // see "id_rsa"
    certVaultDest.sshPubKey = certVaultSource.sshPubKey; // see "id_rsa.pub"

    return certVaultDest;
  }

  public static AzureDockerCertVault copyVaultTlsCerts(AzureDockerCertVault certVaultDest, AzureDockerCertVault certVaultSource) {
    certVaultDest.tlsCACert = certVaultSource.tlsCACert; // see "ca.pem";
    certVaultDest.tlsCAKey = certVaultSource.tlsCAKey; // see "ca-key.pem";
    certVaultDest.tlsClientCert = certVaultSource.tlsClientCert; // see "cert.pem";
    certVaultDest.tlsClientKey = certVaultSource.tlsClientKey; // see "key.pem";
    certVaultDest.tlsServerCert = certVaultSource.tlsServerCert; // see "server.pem";
    certVaultDest.tlsServerKey = certVaultSource.tlsServerKey; // see "server-key.pem";

    return certVaultDest;
  }

  public static AzureDockerCertVault getSSHKeysFromLocalFile(String localPath) throws AzureDockerException {
    AzureDockerCertVault certVault = new AzureDockerCertVault();

    try {
      certVault.sshKey         = new String(Files.readAllBytes(Paths.get(localPath, "id_rsa")));
      certVault.sshPubKey      = new String(Files.readAllBytes(Paths.get(localPath, "id_rsa.pub")));
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return certVault;
  }

  public static AzureDockerCertVault getTLSCertsFromLocalFile(String localPath) throws AzureDockerException {
    AzureDockerCertVault certVault = new AzureDockerCertVault();

    try {
      certVault.tlsCACert      = new String(Files.readAllBytes(Paths.get(localPath, "ca.pem")));
      certVault.tlsCAKey       = new String(Files.readAllBytes(Paths.get(localPath, "ca-key.pem")));
      certVault.tlsClientCert  = new String(Files.readAllBytes(Paths.get(localPath, "cert.pem")));
      certVault.tlsClientKey   = new String(Files.readAllBytes(Paths.get(localPath, "key.pem")));
      certVault.tlsServerCert  = new String(Files.readAllBytes(Paths.get(localPath, "server.pem")));
      certVault.tlsServerKey   = new String(Files.readAllBytes(Paths.get(localPath, "server-key.pem")));
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }

    return certVault;
  }

  public static void saveToLocalFiles(String localPath, AzureDockerCertVault certVault) throws AzureDockerException {
    saveSshKeysToLocalFiles(localPath, certVault);
    saveTlsCertsToLocalFiles(localPath, certVault);
  }

  public static void saveSshKeysToLocalFiles(String localPath, AzureDockerCertVault certVault) throws AzureDockerException {
    try {
      String sep = AzureDockerUtils.getPathSeparator();
      if(certVault.sshKey != null)         { FileWriter file = new FileWriter(localPath + sep + "id_rsa");         file.write(certVault.sshKey);         file.close();}
      if(certVault.sshPubKey != null)      { FileWriter file = new FileWriter(localPath + sep + "id_rsa.pub");     file.write(certVault.sshPubKey);      file.close();}
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static void saveTlsCertsToLocalFiles(String localPath, AzureDockerCertVault certVault) throws AzureDockerException {
    try {
      String sep = AzureDockerUtils.getPathSeparator();
      if(certVault.tlsCACert != null)      { FileWriter file = new FileWriter(localPath + sep + "ca.pem");         file.write(certVault.tlsCACert);      file.close();}
      if(certVault.tlsCAKey != null)       { FileWriter file = new FileWriter(localPath + sep + "ca-key.pem");     file.write(certVault.tlsCACert);      file.close();}
      if(certVault.tlsClientCert != null)  { FileWriter file = new FileWriter(localPath + sep + "cert.pem");       file.write(certVault.tlsClientCert);  file.close();}
      if(certVault.tlsClientKey != null)   { FileWriter file = new FileWriter(localPath + sep + "key.pem");        file.write(certVault.tlsClientKey);   file.close();}
      if(certVault.tlsServerCert != null)  { FileWriter file = new FileWriter(localPath + sep + "server.pem");     file.write(certVault.tlsServerCert);  file.close();}
      if(certVault.tlsServerKey != null)   { FileWriter file = new FileWriter(localPath + sep + "server-key.pem"); file.write(certVault.tlsServerKey);   file.close();}
    } catch(Exception e) {
      throw new AzureDockerException(e.getMessage());
    }
  }

  public static AzureDockerCertVault generateSSHKeys(String passPhrase, String comment) throws AzureDockerException{
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
