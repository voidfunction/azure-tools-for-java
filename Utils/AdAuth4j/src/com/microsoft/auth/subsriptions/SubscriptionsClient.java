package com.microsoft.auth.subsriptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SubscriptionsClient {
    
    private static final String AZURE_API_VERSION = "2014-06-01";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String X_MS_VERSION_HEADER = "x-ms-version";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String USER_AGENT = "auth4j";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String managementUrl = "https://management.core.windows.net/subscriptions";
    
    
    public static List<Subscription> getByToken(String accessToken) throws Exception {
        URL url = new URL(managementUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.addRequestProperty(USER_AGENT_HEADER, USER_AGENT);
        conn.addRequestProperty(X_MS_VERSION_HEADER, AZURE_API_VERSION);
        conn.addRequestProperty(ACCEPT_HEADER, "");
        conn.addRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);
        conn.addRequestProperty(AUTHORIZATION_HEADER, "Bearer " + accessToken);
        
        conn.setRequestMethod("GET");
        conn.setDoOutput(false);
        
        int statusCode = conn.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = null;
            InputStreamReader errorReader = null;
            StringBuilder err = new StringBuilder();
            try {
                errorStream = conn.getErrorStream();
                errorReader = new InputStreamReader(errorStream);
                
                int data;
                while((data = errorReader.read()) != -1) {
                    err.append((char)data);
                }
            }
            finally {
                if(errorStream != null) {
                    errorStream.close();
                }
                if(errorReader != null) {
                    errorReader.close();
                }
            }

            throw new IOException("Azure returned HTTP status code " +
                    Integer.toString(statusCode) + ". Error info: " + err.toString());
        }
        
        InputStream resposeBodyStream = null;
//        BufferedReader reader = null;

//        Subscriptions subscriptions;

        List<Subscription> subscriptions = new LinkedList<>();
        try {
             resposeBodyStream = conn.getInputStream();

            //for some reason there is a jaxb impl conflict between java8 and azure-sdk-for-java dependencies
            // so jaxb mapping doesn't work - have to use another approach

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(resposeBodyStream);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("Subscription");
            System.out.println("list size: " + list.getLength());
            for (int temp = 0; temp < list.getLength(); temp++) {
                Node node = list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String subscriptionId = element.getElementsByTagName("SubscriptionID").item(0).getTextContent();
                    String subscriptionName = element.getElementsByTagName("SubscriptionName").item(0).getTextContent();
                    String subscriptionStatus = element.getElementsByTagName("SubscriptionStatus").item(0).getTextContent();
                    String tenantId = element.getElementsByTagName("AADTenantID").item(0).getTextContent();
                    subscriptions.add(new Subscription(subscriptionId, subscriptionName, subscriptionStatus,tenantId ));
                }
            }


//             java.io.BufferedReader br = new java.io.BufferedReader(new InputStreamReader(resposeBodyStream));
//             StringBuilder resposeBody = new StringBuilder();
//             String line;
//             while ((line = br.readLine()) != null) {
//                 resposeBody.append(line);
//             }
//             System.out.println("Response: " + resposeBody.toString());

/*

            JAXBContext jaxbContext = JAXBContext.newInstance(Subscriptions.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            subscriptions = (Subscriptions)jaxbUnmarshaller.unmarshal(resposeBodyStream);
*/
//             subscriptions = (Subscriptions)jaxbUnmarshaller.unmarshal(new java.io.StringReader(resposeBody.toString()));     
//             System.out.println("Success!");
            
        }   finally {
            resposeBodyStream.close();
//            br.close();
        }
        
        //return subscriptions.getSubscriptions();
        return subscriptions;
    }

}
