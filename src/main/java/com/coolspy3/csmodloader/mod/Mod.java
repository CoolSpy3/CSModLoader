package com.coolspy3.csmodloader.mod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a mod to the loader. When the mod is loaded. It will be ensured that all of its
 * dependencies are met. No two mods may have the same id. All mods must implement
 * {@link Entrypoint}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod
{

    /**
     * @return The id of this mod
     */
    public String id();

    /**
     * @return The user-friendly name of this mod
     */
    public String name() default "<unnamed>";

    /**
     * @return A user-friendly description of this mod
     */
    public String description() default "";

    /**
     * @return This mod's version
     *
     * @see SemanticVersion
     */
    public String version() default "1.0.0";

    /**
     * @return The dependencies of this mod in the form: dependencyName:version
     *
     * @see SemanticVersionRange
     */
    public String[] dependencies() default {};

}
