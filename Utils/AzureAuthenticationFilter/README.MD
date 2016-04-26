#AzureAuthenticationFilter
This is a proof of concept to provide a Servlet Filter to enable Azure B2C Authentication in a Java Web Application.

This filter handles both **Authentication** and **Authorization**. **HttpServletRequest.isUserInRole()**, **HttpServletRequest.getRemoteUser()**, and **HttpServletRequest.getUserPrincipal()** all work as would be expected.

It uses Open ID Connect end-points to obtain:
* Policy Metadata from the Well Known Configuration end-point
* The public keys matching the private keys used to sign the tokens from the JWKS end-point specified by the Well Known Configuration end-point
* Tokens (JWT) from the /authorize end-point specified by the Well Known Configuration end-point

The keys obtained from the **JWKS** end-point are validated using the **"Not Before"** attribute, if it exists the keys are marked as invalid if **now** is before the **"Not Before"** time-stamp. (if **"Not Before"** does not exist it is assumed to be valid).

The following validation is performed on tokens returned from the /authorise end-point:
* **"Issued At"** is in the past (if it does not exist it is assumed to be valid)
* **"Not Before"** is in the past (if it does not exist it is assumed to be valid)
* **"Expiration"** is in the future (if it does not exist it is assumed to be valid)
* **"Audience"** matches the Application ID configured for the application
* **"Issuer"** matches the issuer in the Metadata
* **"Signature"** (using a key from the JWKS end-point)

Configuration of this Filter is picked up from system environment variables:
* **AAD_OIDC_POLICY** (The policy to use in Open ID Connect)
* **AAD_REDIRECT_URL** (The URL to redirect to after obtaining a token)
* **AAD_APPLICATION_ID** (The GUID of the application you have registered in Azure AD B2C)
* **AAD_TENANT** (The Azure AD B2C tenant to use)
* **AAD_PRINCIPAL_ID** (The GUID of the principal used to query the graph API)
* **AAD_PRINCIPAL_SECRET** (The password for the principal used to query the graph API)

You will also need to update your web.xml as follows:
```
<filter>
  <filter-name>authenticationFilter</filter-name>
  <filter-class>com.microsoft.azure.oidc.filter.impl.SimpleAuthenticationFilter</filter-class>
  <init-param>
    <param-name>authenticationConfiguration</param-name>
    <param-value>/WEB-INF/configuration/authentication.json</param-value>
  </init-param>
  <init-param>
    <param-name>algorithmConfiguration</param-name>
     <param-value>/WEB-INF/configuration/algorithm.json</param-value>
  </init-param>
  <init-param>
  	<param-name>securityCacheSize</param-name>
  	<param-value>10000</param-value>
  </init-param>
</filter>
<filter-mapping>
  <filter-name>authenticationFilter</filter-name>
  <url-pattern>/*</url-pattern>
  <dispatcher>REQUEST</dispatcher>
  <dispatcher>FORWARD</dispatcher>
</filter-mapping>
```

And add an authentication.json in /WEB-INF/configuration which contains the following:
```
{
  "exclusionUriPatterns": [
    "/",
    "/index.xhtml",
    "/logout",
    "unauthenticated/*"
  ],
  "authorisationUriPatterns": [
    {"uriPattern": "/authorised.xhtml", "roles": [ "JavaEEManagers", "JavaEEPowerUsers" ]},
    {"uriPattern": "/authorised/*", "roles": [ "JavaEEManagers", "JavaEEPowerUsers" ]}
  ]
}
``` 

And add an algorithm.json in /WEB-INF/configuration which contains the following:
```
{
  "algorithms": [
    {"name": "RS256", "javaName": "SHA256withRSA"},
    {"name": "RS384", "javaName": "SHA384withRSA"},
    {"name": "RS512", "javaName": "SHA512withRSA"}
  ],
  "algorithmClasses": [
    {"name": "RS256", "className": "RSA"},
    {"name": "RS384", "className": "RSA"},
    {"name": "RS512", "className": "RSA"}
  ]
}
```