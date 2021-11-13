package com.coolspy3.csmodloader.mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod
{

    public String id();

    public String name() default "<unnamed>";

    public String description() default "";

    public String version() default "1.0.0";

    public String[] dependencies() default {};

}
