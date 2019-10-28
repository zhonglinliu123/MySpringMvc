package com.zlin.mvc.annotation;

import java.lang.annotation.*;

//jdk自带的注解(对自定义的注解进行注释的,注解的注解)  元注解
// @Target 表示自定义的这个注解能用在哪些地方, 用在类/方法/接口/字段/包..上面
// @Retention 表示注解的生命周期(被jvm加载 => 被jvm执行),
/* RetentionPolicy.RUNTIME 表示class文件被jvm加载成实例后还能获取到annotation的注释信息
    (即项目运行起来了能拿到annotation的信息) @Controller("/zlin")  能拿到value "/zlin"
 */
// 自定义注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    /**
     *  表示给Controller注册别名
     */
    String value() default "";
}
