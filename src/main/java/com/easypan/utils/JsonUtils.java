package com.easypan.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    
    public static String covertObj2Json(Object obj){
        return JSON.toJSONString(obj);
    }
    
    public static <T> T convertJson2Obj(String json,Class<T> classz){
        return JSONObject.parseObject(json,classz);
    }
    
    public static <T> List<T> convertJsonArray2List(String json,Class<T> classz){
        return JSONArray.parseArray(json,classz);
    }
}
