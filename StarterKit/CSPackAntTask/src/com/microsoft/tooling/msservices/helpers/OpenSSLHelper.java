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
package com.microsoft.tooling.msservices.helpers;

import com.microsoft.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoft.windowsazure.core.utils.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class OpenSSLHelper {
    public static final String PASSWORD = System.getProperty("java.specification.version").compareTo("1.7") >= 0 ? "" : "Java6NeedsPwd";

    public static String processCertificate(String xmlPublishSettings) throws AzureCmdException {
        try {
            Node publishProfileNode = ((NodeList) XmlHelper.getXMLValue(xmlPublishSettings, "/PublishData/PublishProfile", XPathConstants.NODESET)).item(0);
            String version = XmlHelper.getAttributeValue(publishProfileNode, "SchemaVersion");

            boolean isFirstVersion = (version == null || Float.parseFloat(version) < 2);

            NodeList subscriptionList = (NodeList) XmlHelper.getXMLValue(xmlPublishSettings, "//Subscription", XPathConstants.NODESET);

            Document ownerDocument = null;

            for (int i = 0; i != subscriptionList.getLength(); i++) {
                //Gets the pfx info
                Element node = (Element) subscriptionList.item(i);
                ownerDocument = node.getOwnerDocument();
                String pfx = XmlHelper.getAttributeValue(isFirstVersion ? publishProfileNode : node, "ManagementCertificate");
                byte[] decodedBuffer = new BASE64Decoder().decodeBuffer(pfx);

                byte[] buf;
                if (System.getProperty("java.specification.version").compareTo("1.7") >= 0) {
                    KeyStore keyStore = null;
                    try {
                        keyStore = KeyStore.getInstance("PKCS12");

                        keyStore.load(null, PASSWORD.toCharArray());

                        InputStream sslInputStream = new ByteArrayInputStream(Base64.decode(pfx));

                        keyStore.load(sslInputStream, "".toCharArray());

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        keyStore.store(outputStream, PASSWORD.toCharArray());
                        buf = outputStream.toByteArray();
                    } catch (KeyStoreException e) {
                        throw new IllegalArgumentException(
                                "Cannot create keystore from the publish settings file", e);
                    } catch (CertificateException e) {
                        throw new IllegalArgumentException(
                                "Cannot create keystore from the publish settings file", e);
                    } catch (NoSuchAlgorithmException e) {
                        throw new IllegalArgumentException(
                                "Cannot create keystore from the publish settings file", e);
                    }
                } else {
                    throw new AzureCmdException("Need Java 1.7 or later");
                }
                // Not using OpenSSL anymore; assuming that we have java >= 1.7
                /*else {
                    //Create pfxFile
                    File tmpPath = new File(System.getProperty("java.io.tmpdir") + File.separator + "tempAzureCert");
                    tmpPath.mkdirs();
                    tmpPath.setWritable(true);

                    File pfxFile = new File(tmpPath.getPath() + File.separator + "temp.pfx");

                    pfxFile.createNewFile();
                    pfxFile.setWritable(true);

                    FileOutputStream pfxOutputStream = new FileOutputStream(pfxFile);
                    pfxOutputStream.write(decodedBuffer);
                    pfxOutputStream.flush();
                    pfxOutputStream.close();

                    String path = getOpenSSLPath();
                    if (path == null || path.isEmpty()) {
                        throw new Exception("Please configure a valid OpenSSL executable location.");
                    } else if (!path.endsWith(File.separator)) {
                        path = path + File.separator;
                    }

                    //Export to pem with OpenSSL
                    runCommand(new String[]{path + "openssl", "pkcs12", "-in", "temp.pfx", "-out", "temp.pem", "-nodes", "-password", "pass:"}, tmpPath);

                    //Export to pfx again and change password
                    runCommand(new String[]{path + "openssl", "pkcs12", "-export", "-out", "temppwd.pfx", "-in", "temp.pem", "-password", "pass:" + PASSWORD}, tmpPath);

                    //Read file and replace pfx with password protected pfx
                    File pwdPfxFile = new File(tmpPath.getPath() + File.separator + "temppwd.pfx");
                    buf = new byte[(int) pwdPfxFile.length()];
                    FileInputStream pfxInputStream = new FileInputStream(pwdPfxFile);
                    pfxInputStream.read(buf, 0, (int) pwdPfxFile.length());
                    pfxInputStream.close();

                    deleteFile(tmpPath);
                }
*/
                node.setAttribute("ManagementCertificate", new BASE64Encoder().encode(buf).replace("\r", "").replace("\n", ""));

                if (isFirstVersion) {
                    node.setAttribute("ServiceManagementUrl", XmlHelper.getAttributeValue(publishProfileNode, "Url"));
                }
            }

            if (ownerDocument == null) {
                return null;
            }

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            Writer out = new StringWriter();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.transform(new DOMSource(ownerDocument), new StreamResult(out));

            return out.toString();
        } catch (Exception ex) {
            throw new AzureCmdException("Error processing publish settings file.", ex.getMessage());
        }
    }
}