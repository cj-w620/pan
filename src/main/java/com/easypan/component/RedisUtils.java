package com.easypan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component("redisUtils")
public class RedisUtils<V> {
    
    @Resource
    private RedisTemplate<String,V> redisTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    public V get(String key){
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 普通缓存放入
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key,V value){
        try{
            redisTemplate.opsForValue().set(key,value);
            return true;
        }catch (Exception e){
            logger.error("设置redisKey:{},vale:{}失败",key,value);
            return false;
        }
    }
    
    /**
     * 普通缓存放入并设置时间
     * @param key
     * @param value
     * @param time  时间（秒） time要大于0 如果time小于0，时间无限期
     * @return
     */
    public boolean setex(String key,V value,long time){
        try{
            if(time > 0){
                redisTemplate.opsForValue().set(key,value,time, TimeUnit.SECONDS);
            }else {
                set(key,value);
            }
            return true;
        }catch (Exception e){
            logger.error("设置redisKey:{},vale:{}失败",key,value);
            return false;
        }
    }
    
}
