/*
 * *
 *  * Copyright (c) Microsoft Corporation
 *  * <p/>
 *  * All rights reserved.
 *  * <p/>
 *  * MIT License
 *  * <p/>
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  * <p/>
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  * <p/>
 *  * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.adauth.UserIdentifierType;

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
