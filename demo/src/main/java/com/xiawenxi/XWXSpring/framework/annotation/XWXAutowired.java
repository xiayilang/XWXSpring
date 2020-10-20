package com.xiawenxi.XWXSpring.framework.annotation;


import java.lang.annotation.*;

/**
 * @author xf107134
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XWXAutowired {

    String value() default "";

}
