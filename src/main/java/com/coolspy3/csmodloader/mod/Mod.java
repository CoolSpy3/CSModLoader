package com.coolspy3.csmodloader.mod;

public @interface Mod
{

    public String id();

    public String name() default "<unnamed>";

    public String description() default "";

    public String version() default "1.0.0";

    public String[] dependencies() default {};

}
