package com.microsoft.auth.subsriptions;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name="Subscriptions", namespace = "http://schemas.microsoft.com/windowsazure" )
//@XmlType
public class Subscriptions {
   
   @XmlElement(name="Subscription")
   private List<Subscription> subscriptions = new ArrayList<Subscription>();
   
   
   public List<Subscription> getSubscriptions() {
      return subscriptions;
   }
}
