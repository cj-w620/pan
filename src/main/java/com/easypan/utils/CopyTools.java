package com.easypan.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    
    /**
     * 将S类的集合转化为T类的集合
     * 匹配的属性会拷贝，非公开属性会被忽略
     * 要有get set方法才能成功赋值
     * @param sList
     * @param classz
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T,S> List<T> copyList(List<S> sList,Class<T> classz){
        List<T> list = new ArrayList<T>();
        for(S s : sList){
            T t = null;
            try {
                t = classz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            BeanUtils.copyProperties(s,t);
            list.add(t);
        }
        return list;
    }
    
    /**
     * S类的对象转换为T类的对象
     * @param s
     * @param classz
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T,S> T copy(S s,Class<T> classz){
        T t = null;
        try {
            t = classz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s,t);
        return t;
    }
    
    
    
}
