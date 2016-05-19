package com.microsoft.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;//import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class TokenCache {
    private final static Logger log = Logger.getLogger(TokenCache.class.getName());
    private final int SchemaVersion = 1;
    private final static String Delimiter = ":::";
    private final Map<TokenCacheKey, AuthenticationResult> tokenCacheDictionary;
    private volatile boolean hasStateChanged = false;
    private final Object lock = new Object();
    private static TokenCache defaultShared = null;

    /// <summary>
    /// Default constructor.
    /// </summary>
    public TokenCache() {
        this.tokenCacheDictionary = new ConcurrentHashMap<TokenCacheKey, AuthenticationResult>();
    }

    /// <summary>
    /// Constructor receiving state of the cache
    /// </summary>        
    public TokenCache(byte[] state) throws Exception {
       this();
        this.deserialize(state);
    }

    /// <summary>
    /// Gets or sets the flag indicating whether cache state has changed. ADAL methods set this flag after any change. Caller application should reset 
    /// the flag after serializing and persisting the state of the cache.
    /// </summary>
    public boolean getHasStateChanged() {
       synchronized (lock) {
          return hasStateChanged;
       }       
    }
    
    public void setHasStateChanged(boolean val) {
       synchronized (lock) {
          hasStateChanged = val;
       }       
    }
    
    /// <summary>
    /// Static token cache shared by all instances of AuthenticationContext which do not explicitly pass a cache instance during construction.
    /// </summary>
    public static TokenCache getDefaultShared() { 
       if (defaultShared == null) {
          defaultShared = new TokenCache();
       }
       return defaultShared;
    }

    /// <summary>
    /// Gets the number of items in the cache.
    /// </summary>
    public int getCount() {
       synchronized (lock) {
          return this.tokenCacheDictionary.size();
        }
    }

    /// <summary>
    /// Serializes current state of the cache as a blob. Caller application can persist the blob and update the state of the cache later by 
    /// passing that blob back in constructor or by calling method Deserialize.
    /// </summary>
    /// <returns>Current state of the cache as a blob</returns>
    public byte[] serialize() throws Exception {
       synchronized (lock) {
          log.info("Serializing...");
          // memory stream
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          
          // stream writer
          DataOutputStream writer = new DataOutputStream(outputStream);

          writer.writeInt(SchemaVersion);
          writer.writeInt(tokenCacheDictionary.size());
          for(TokenCacheKey key : tokenCacheDictionary.keySet()) {
             AuthenticationResult res = tokenCacheDictionary.get(key);
             writer.writeUTF(String.format(
                   "%s%s%s%s%s%s%s", 
                   key.authority, Delimiter, 
                   key.resource, Delimiter, 
                   key.clientId, Delimiter, 
                   key.tokenSubjectType));
             writer.writeUTF(res.serialize());
          }
          log.info(String.format("Serialized %d items to the output stream.", tokenCacheDictionary.size()));
          return outputStream.toByteArray();     
       }
    }

    /// <summary>
    /// Deserializes state of the cache. The state should be the blob received earlier by calling the method Serialize.
    /// </summary>
    /// <param name="state">State of the cache as a blob</param>
    public void deserialize(byte[] state) throws Exception {
       synchronized(lock) {
          log.info("Deserializing...");
            if (state.length == 0) {
                this.tokenCacheDictionary.clear();
                return;
            }
          // memory stream
          ByteArrayInputStream inputStream = new ByteArrayInputStream(state);
          // stream reader
          DataInputStream reader = new DataInputStream(inputStream);
            int schemaVersion = reader.readInt();
            if (schemaVersion != SchemaVersion) {
                log.warn("The version of the persistent state of the cache does not match the current schema, so skipping deserialization.");
                return;
            }
            tokenCacheDictionary.clear();
            int count = reader.readInt();
            for (int n = 0; n < count; n++) {
                String keyString = reader.readUTF();

                String[] kvpElements = keyString.split(Delimiter);
                AuthenticationResult result = AuthenticationResult.deserialize(reader.readUTF());
                TokenCacheKey key = new TokenCacheKey(kvpElements[0], kvpElements[1], kvpElements[2],
                    TokenSubjectType.valueOf(TokenSubjectType.class,kvpElements[3]), result.userInfo);
                
                log.info(String.format("Putting key into the dictionary [hash: '%x']", key.hashCode()));
                tokenCacheDictionary.put(key, result);
            }
            log.info(String.format("Deserialized %d items to token cache.", count));
        }
    }
/*
    /// <summary>
    /// Reads a copy of the list of all items in the cache. 
    /// </summary>
    /// <returns>The items in the cache</returns>
    public List<TokenCacheItem> readItems() throws Exception {
       synchronized(lock) {
            onBeforeAccess();
            List<TokenCacheItem> items = new ArrayList<TokenCacheItem>();
            for (TokenCacheKey key : tokenCacheDictionary.keySet()) {
               AuthenticationResult value = tokenCacheDictionary.get(key);
                items.add(new TokenCacheItem(key, value));
            }
            return items;
        }
    }

    /// <summary>
    /// Deletes an item from the cache.
    /// </summary>
    /// <param name="item">The item to delete from the cache</param>
    public void deleteItem(TokenCacheItem item) throws Exception
    {
        synchronized(lock) {
            if (item == null) {
                throw new IllegalArgumentException("item");
            }
            onBeforeAccess();
            //TokenCacheKey toRemoveKey = this.tokenCacheDictionary.keySet().stream().filter(k -> item.match(k)).findFirst().get(); java8
            TokenCacheKey toRemoveKey = null;
            for( TokenCacheKey key : tokenCacheDictionary.keySet()) {
                if(item.match(key)) {
                    toRemoveKey = key;
                    break;
                }
            }
            if (toRemoveKey != null) {
                this.tokenCacheDictionary.remove(toRemoveKey);
                log.info("One item removed successfully");
            } else {
                log.info("Item not Present in the Cache");
            }
            this.setHasStateChanged(true);
        }
    }
*/
    /// <summary>
    /// Clears the cache by deleting all the items. Note that if the cache is the default shared cache, clearing it would
    /// impact all the instances of <see cref="AuthenticationContext"/> which share that cache.
    /// </summary>
    public void clear() throws Exception {
       synchronized(lock) {
            onBeforeAccess();
            log.info(String.format("Clearing Cache :- %d items to be removed", tokenCacheDictionary.size()));
            this.tokenCacheDictionary.clear();
            log.info("Successfully Cleared Cache");
            this.setHasStateChanged(true);
            onAfterAccess();
        }
    }
    
    void onAfterAccess() throws Exception {
        if(onAfterAccessCallback != null) {
            onAfterAccessCallback.run();
        }
    }
    void onBeforeAccess() throws Exception {
        if (onBeforeAccessCallback != null) {
            onBeforeAccessCallback.run();
        }
    }

    private Runnable  onBeforeAccessCallback = null;
    private Runnable  onAfterAccessCallback = null;

    public void setOnBeforeAccessCallback(Runnable onBeforeAccessCallback) {
        this.onBeforeAccessCallback = onBeforeAccessCallback;
    }

    public void setOnAfterAccessCallback(Runnable onAfterAccessCallback) {
        this.onAfterAccessCallback = onAfterAccessCallback;
    }

    AuthenticationResult loadFromCache(String authority, String resource, String clientId, TokenSubjectType subjectType, String uniqueId, String displayableId) throws Exception {
       synchronized(lock) {
            log.info("Looking up cache for a token...");
            AuthenticationResult result = null;
            Map.Entry<TokenCacheKey, AuthenticationResult> kvp = loadSingleItemFromCache(authority,
                resource, clientId, subjectType, uniqueId, displayableId);
            if (kvp != null) {
                TokenCacheKey cacheKey = kvp.getKey();
                result = kvp.getValue();

                Calendar now = new GregorianCalendar();
                now.setTimeInMillis(System.currentTimeMillis());
                int expirationMarginInMinutes = 5;
                now.add(Calendar.MINUTE, expirationMarginInMinutes);
                Calendar expiresOn = new GregorianCalendar();
                expiresOn.setTimeInMillis(TimeUnit.SECONDS.toMillis(result.expiresOn));
                boolean tokenNearExpiry = expiresOn.before(now);

                if (tokenNearExpiry) {
                    result.accessToken = null;
                    log.info("An expired or near expiry token was found in the cache");
                } else if (!cacheKey.resource.equals(resource)) {
                    log.info(String.format(
                            "Multi resource refresh token for resource '%s' will be used to acquire token for '%s'",
                            cacheKey.resource, resource));
                    AuthenticationResult newResult = new AuthenticationResult(null, null, result.refreshToken, 0);
                    newResult.updateTenantAndUserInfo(result.tenantId, result.idToken, result.userInfo);
                    result = newResult;
                } else {
                   long nowSec = System.currentTimeMillis()/1000;
                    log.info(
                        String.format("%d minutes left until token in cache expires", TimeUnit.SECONDS.toMinutes(result.expiresOn - nowSec)));
                }
                if (result.accessToken == null && result.refreshToken == null) {
                    this.tokenCacheDictionary.remove(cacheKey);
                    log.info( "An old item was removed from the cache");
                    this.setHasStateChanged(true);
                    result = null;
                }
                if (result != null) {
                    log.info(String.format("A matching item (access token or refresh token or both) was found in the cache [hashCode: '%x']", cacheKey.hashCode() ));
                }
            } else {
                log.info( "No matching token was found in the cache");
            }
            return result;
        }
    }

    void storeToCache(AuthenticationResult result, String authority, String resource, String clientId, TokenSubjectType subjectType) {
        synchronized(lock) {
            log.info("Storing token in the cache...");
            TokenCacheKey tokenCacheKey = new TokenCacheKey(authority, resource, clientId, subjectType, result.userInfo);
            log.info(String.format("==> tokenCacheKey:\n \t%s\n\t%s\n\t%s\n\t%s\n\t%s\n\t%s\n", 
                  authority
                  , resource.toLowerCase()
                  , clientId.toLowerCase()
                  , result.userInfo.uniqueId
                  , result.userInfo.displayableId
                  , subjectType));
            tokenCacheDictionary.put(tokenCacheKey, result);
            log.info(String.format("==> hashCode: '%x'", tokenCacheKey.hashCode()));
            
            log.info("An item was stored in the cache");
            updateCachedMrrtRefreshTokens(result, authority, clientId, subjectType);

            this.setHasStateChanged(true);
        }
    }

    private void updateCachedMrrtRefreshTokens(AuthenticationResult result, String authority, String clientId, TokenSubjectType subjectType) {
       synchronized(lock) {
            if (result.userInfo != null && result.isMultipleResourceRefreshToken) {
//                List<Map.Entry<TokenCacheKey, AuthenticationResult>> mrrtItems =
//                    queryCache(authority, clientId, subjectType, result.userInfo.uniqueId, result.userInfo.displayableId)
//                       .stream().filter(p -> p.getValue().isMultipleResourceRefreshToken).collect(Collectors.toList());

                List<Map.Entry<TokenCacheKey, AuthenticationResult>> items =  queryCache(authority, clientId,
                        subjectType, result.userInfo.uniqueId, result.userInfo.displayableId);

                List<Map.Entry<TokenCacheKey, AuthenticationResult>> mrrtItems = new LinkedList<>();
                for (Map.Entry<TokenCacheKey, AuthenticationResult> item : items) {
                    if (item.getValue().isMultipleResourceRefreshToken) {
                        mrrtItems.add(item);
                    }
                }

                for (Map.Entry<TokenCacheKey, AuthenticationResult> mrrtItem : mrrtItems) {
                    AuthenticationResult update = mrrtItem.getValue();
                    update.refreshToken = result.refreshToken;
                    tokenCacheDictionary.put(mrrtItem.getKey(), update);
                }
            }
        }
    }

    private Map.Entry<TokenCacheKey, AuthenticationResult> loadSingleItemFromCache(String authority, String resource, String clientId, 
          TokenSubjectType subjectType, String uniqueId, String displayableId) throws AuthException {
        synchronized(lock) {
            // First identify all potential tokens.
            List<Map.Entry<TokenCacheKey, AuthenticationResult>> items = queryCache(authority, clientId,
                subjectType, uniqueId, displayableId);

            List<Map.Entry<TokenCacheKey, AuthenticationResult>> resourceSpecificItems = new LinkedList<>();
            for (Map.Entry<TokenCacheKey, AuthenticationResult> item : items) {
                if (item.getKey().resource.equals(resource)) {
                    resourceSpecificItems.add(item);
                }
            }

//            List<Map.Entry<TokenCacheKey, AuthenticationResult>> resourceSpecificItems =
//                items.stream().filter(p -> p.getKey().resource.equals(resource)).collect(Collectors.toList());

            int resourceValuesCount = resourceSpecificItems.size();
            Map.Entry<TokenCacheKey, AuthenticationResult> returnValue = null;
            if (resourceValuesCount == 1) {
                log.info( "An item matching the requested resource was found in the cache");
                returnValue = resourceSpecificItems.get(0);
            } else if (resourceValuesCount == 0) {
                // There are no resource specific tokens.  Choose any of the MRRT tokens if there are any.
//                items.stream().filter(p -> p.getValue().isMultipleResourceRefreshToken).collect(Collectors.toList());
                List<Map.Entry<TokenCacheKey, AuthenticationResult>> mrrtItems = new LinkedList<>();
                for (Map.Entry<TokenCacheKey, AuthenticationResult> item : items) {
                    if (item.getValue().isMultipleResourceRefreshToken) {
                        mrrtItems.add(item);
                    }
                }

                if (!mrrtItems.isEmpty()) {
                    returnValue = mrrtItems.get(0);
                    log.info("A Multi Resource Refresh Token for a different resource was found which can be used");
                }
            } else {
                throw new AuthException(AuthError.MultipleTokensMatched);
            }
            return returnValue;
        }
    }

    /// <summary>
    /// Queries all values in the cache that meet the passed in values, plus the 
    /// authority value that this AuthorizationContext was created with.  In every case passing
    /// null results in a wildcard evaluation.
    /// </summary>
    private List<Map.Entry<TokenCacheKey, AuthenticationResult>> queryCache(String authority, String clientId,
        TokenSubjectType subjectType, String uniqueId, String displayableId) {
       synchronized(lock) {
          List<Map.Entry<TokenCacheKey, AuthenticationResult>> res = new LinkedList<>();
          for(Map.Entry<TokenCacheKey, AuthenticationResult> p : tokenCacheDictionary.entrySet()) {
             if(p.getKey().authority.equals(authority)
                        && (StringUtils.isNullOrWhiteSpace(clientId) || p.getKey().clientId.equals(clientId))
                        && (StringUtils.isNullOrWhiteSpace(uniqueId) || p.getKey().uniqueId.equals(uniqueId))
                        && (StringUtils.isNullOrWhiteSpace(displayableId) || p.getKey().displayableId.equals(displayableId))
                        && p.getKey().tokenSubjectType.equals(subjectType)) {
                res.add(p);
             }
          }
          return res;
        }
    }
}
