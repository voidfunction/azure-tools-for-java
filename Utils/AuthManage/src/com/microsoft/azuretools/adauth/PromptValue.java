package com.microsoft.azuretools.adauth;

public class PromptValue
{
    public final static String Login = "login";
    public final static String RefreshSession = "refresh_session";

    // The behavior of this value is identical to prompt=none for managed users; However, for federated users, AAD
    // redirects to ADFS as it cannot determine in advance whether ADFS can login user silently (e.g. via WIA) or not.
    public final static String AttemptNone = "attempt_none";        
}
