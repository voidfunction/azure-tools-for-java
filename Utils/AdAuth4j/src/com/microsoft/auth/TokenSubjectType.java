package com.microsoft.auth;

public enum TokenSubjectType {
    /// <summary>
    /// User
    /// </summary>
    User,
    /// <summary>
    /// Client
    /// </summary>
    Client,
    /// <summary>
    /// UserPlusClient: This is for confidential clients used in middle tier.
    /// </summary>
    UserPlusClient
}
