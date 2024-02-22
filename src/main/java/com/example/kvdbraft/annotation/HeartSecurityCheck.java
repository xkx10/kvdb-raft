package com.example.kvdbraft.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // 设置注解保留期限为运行时
@Target(ElementType.TYPE) // 设置注解适用于方法
public @interface HeartSecurityCheck {
    int type() default 2;
}
