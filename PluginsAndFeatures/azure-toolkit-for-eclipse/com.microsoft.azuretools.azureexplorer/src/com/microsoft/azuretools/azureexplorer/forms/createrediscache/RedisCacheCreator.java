/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.azureexplorer.forms.createrediscache;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.management.redis.RedisCaches;

public final class RedisCacheCreator {
    private Map<String, ProcessingStrategy> creatorMap = new HashMap<String, ProcessingStrategy>();
    public RedisCacheCreator(RedisCaches redisCaches, String dnsName, String regionName, String groupName)
    {
        creatorMap.put("C0 Basic 250MB noSslPort", new BasicProcessorWithNewResourceGroupNonSslPort(redisCaches, dnsName, regionName, groupName, 0));
        // TODO: C1 - C5 Basic, C0 - C5 Std, P1 - P4 Premium Processors
    }
    
    public Map<String, ProcessingStrategy> CreatorMap() {
        if(creatorMap.isEmpty()) {
            throw new IllegalStateException("Redis cache creator map not initialized properly");
        }
        return creatorMap;
    }
}