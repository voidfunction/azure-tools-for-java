package com.microsoft.azuretools.eclipse.testers;

import org.eclipse.core.expressions.PropertyTester;

import com.microsoft.azuretools.authmanage.AuthMethodManager;

public class AuthPropertyTester extends PropertyTester {
    public static final String PROPERTY_NAMESPACE = "com.microsoft.azuretools.eclipse.testers";
    public static final String PROPERTY_IS_SIGNED_IN = "isSignedIn";
 
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        
        if (PROPERTY_IS_SIGNED_IN.equals(property)) {
            try {
                AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
                boolean isSignIn = authMethodManager.isSignedIn();
                return isSignIn;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return true;
    }
}
