package com.microsoft.auth;

public class TokenCacheKey {
//		final static Logger log = Logger.getLogger(TokenCache.class.getName());
    TokenCacheKey(String authority, String resource, String clientId, TokenSubjectType tokenSubjectType, UserInfo userInfo) {
    	this(authority, resource, clientId, tokenSubjectType, (userInfo != null) ? userInfo.uniqueId : null, (userInfo != null) ? userInfo.displayableId : null);
	}

	TokenCacheKey(String authority, String resource, String clientId, TokenSubjectType tokenSubjectType, String uniqueId, String displayableId) {
	    this.authority = authority;
	    this.resource = resource;
	    this.clientId = clientId;
	    this.tokenSubjectType = tokenSubjectType;
	    this.uniqueId = uniqueId;
	    this.displayableId = displayableId;
	}
	
	public String authority;
	public String resource;
	public String clientId;
	public String uniqueId;
	public String displayableId;
	public TokenSubjectType tokenSubjectType;

	/// <summary>
	/// Determines whether the specified object is equal to the current object.
	/// </summary>
	/// <returns>
	/// true if the specified object is equal to the current object; otherwise, false.
	/// </returns>
	/// <param name="obj">The object to compare with the current object. </param><filterpriority>2</filterpriority>
	@Override
	public boolean equals(Object obj) {
	    TokenCacheKey other = (TokenCacheKey)obj;
//	    return (other != null) && this.equals(other);
	    return (other != null) && this.hashCode() == other.hashCode();
	}

	/// <summary>
	/// Returns the hash code for this TokenCacheKey.
	/// </summary>
	/// <returns>
	/// A 32-bit signed integer hash code.
	/// </returns>
	@Override
	public int hashCode() {
	    final String Delimiter = ":::";
	    String key = (this.authority + Delimiter 
		        + this.resource.toLowerCase() + Delimiter
		        + this.clientId.toLowerCase() + Delimiter
		        + ((this.uniqueId != null) ? this.uniqueId.toLowerCase() : null) + Delimiter
		        + ((this.displayableId != null) ? this.displayableId.toLowerCase() : null) + Delimiter
		        + this.tokenSubjectType);
	    int hc = key.hashCode();
//	    System.out.println(String.format("==> hashCode: '%x'; key: '%s'; ", hc, key));
	    return hc;
	}
}
