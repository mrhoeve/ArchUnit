package com.tngtech.archunit.example;

public @interface Module {
    String name();

    String[] allowedDependencies() default {};
}
