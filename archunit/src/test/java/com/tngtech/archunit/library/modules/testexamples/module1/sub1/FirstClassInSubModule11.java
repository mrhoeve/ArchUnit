package com.tngtech.archunit.library.modules.testexamples.module1.sub1;

import java.util.List;

import com.tngtech.archunit.library.modules.testexamples.module1.sub2.FirstClassInSubModule12;
import com.tngtech.archunit.library.modules.testexamples.module1.sub2.SecondClassInSubModule12;

public class FirstClassInSubModule11 {
    SecondClassInSubModule11 noDependencyToOtherModule;
    FirstClassInSubModule12 firstDependencyOnSubModule12;
    SecondClassInSubModule12 SecondDependencyOnSubModule12;

    String firstUndefinedDependency;
    List<?> secondUndefinedDependency;
}
