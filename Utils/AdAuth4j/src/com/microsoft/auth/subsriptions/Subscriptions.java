package com.microsoft.auth.subsriptions;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscriptions {

   @JsonProperty("value")
   private List<Subscription> subscriptions;

   public List<Subscription> getSubscriptions() {
      return subscriptions;
   }
}
