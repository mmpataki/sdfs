package com.mmp.sdfs.conf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {
    String[] keys();

    String help();

    boolean required() default false;

    boolean multivalued() default false;

    boolean sensitive() default false;

    String parser() default "defaultParser";

    String defValue() default "";
}