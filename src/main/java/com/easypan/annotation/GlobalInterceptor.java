package com.easypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;
//@Target：这个注解要用在哪里
@Target({ElementType.METHOD})   //在方法上用
//@Retention(RetentionPolicy.RUNTIME)  ：生命周期
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {
    
    /**
     * 校验参数
     * @return
     */
    boolean checkParams() default false;
    
    /**
     * 校验登录
     * @return
     */
    boolean checkLogin() default true;
    
    /**
     * 校验超级管理员
     * @return
     */
    boolean checkAdmin() default false;
}
