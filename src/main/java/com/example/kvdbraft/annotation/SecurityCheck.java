package com.example.kvdbraft.annotation;

import com.example.kvdbraft.enums.ESecurityCheckType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SecurityCheck {
    ESecurityCheckType[] types() default {}; // 默认处理类型为1，可以处理多种类型
}
