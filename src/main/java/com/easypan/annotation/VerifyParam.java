package com.easypan.annotation;

import com.easypan.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.FIELD})
public @interface VerifyParam {

    int min() default -1;
    
    int max() default -1;
    
    //默认不是必须的
    boolean required() default false;
    
    //默认不检验
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;
}
