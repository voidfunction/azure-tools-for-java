package com.microsoft.auth;

import java.io.InputStream;
//import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

public class JsonHelper {
//	private static final Logger log = Logger.getLogger(JsonHelper.class.getName());
	
    public static <T> T deserialize(Class<T> cls, String json) throws Exception {
//    	log.info("structure: " + cls.getName());
//    	log.info("json string: " + json);
        if(json == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, cls);
    }
    
    public static <T> T deserialize(Class<T> cls,InputStream is) throws Exception {
        if(is == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(is, cls);
    }
    
    public static <T> String serialize(T jsonObject) throws Exception {
    	if(jsonObject == null) return null;
    	ObjectMapper mapper = new ObjectMapper();
    	return mapper.writeValueAsString(jsonObject);
    }
}
