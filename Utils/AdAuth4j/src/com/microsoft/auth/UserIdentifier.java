package com.microsoft.auth;

public final class UserIdentifier
{
    private final static String AnyUserId = "AnyUser";
    private static final UserIdentifier anyUserSingleton = new UserIdentifier(AnyUserId, UserIdentifierType.UniqueId);

    /// <summary>
    /// 
    /// </summary>
    /// <param name="id"></param>
    /// <param name="type"></param>
    public UserIdentifier(String id, UserIdentifierType type)
    {
        if (id == null || id.isEmpty())
        {
            throw new IllegalArgumentException("id");
        }

        this.id = id;
        this.type = type;
    }

    /// <summary>
    /// Gets type of the <see cref="UserIdentifier"/>.
    /// </summary>
    UserIdentifierType type;
    
    /// <summary>
    /// Gets Id of the <see cref="UserIdentifier"/>.
    /// </summary>
    String id;

    /// <summary>
    /// Gets an static instance of <see cref="UserIdentifier"/> to represent any user.
    /// </summary>
    public static final UserIdentifier anyUser = anyUserSingleton;

    boolean isAnyUser() {
        return (this.type == anyUser.type && this.id == anyUser.id);
    }

    String uniqueId() {
        return (!this.isAnyUser() && this.type == UserIdentifierType.UniqueId) ? this.id : null;
    }

    String displayableId() {
        return (!this.isAnyUser() && (this.type == UserIdentifierType.OptionalDisplayableId || this.type == UserIdentifierType.RequiredDisplayableId)) ? this.id : null;
    }
}
