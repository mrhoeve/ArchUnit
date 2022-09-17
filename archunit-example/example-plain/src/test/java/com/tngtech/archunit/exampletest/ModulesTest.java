package com.tngtech.archunit.exampletest;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.example.Module;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.library.modules.AnnotationDescriptor;
import com.tngtech.archunit.library.modules.ArchModule;
import com.tngtech.archunit.library.modules.ModuleDependency;
import com.tngtech.archunit.library.modules.ModuleRuleDefinition.GenericDefinition.DescriptorFunction;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.library.modules.ModuleRuleDefinition.modules;

public class ModulesTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("com.tngtech.archunit.example");

    /**
     * This example demonstrates how to easily derive modules from classes annotated with a certain annotation.
     * Within the example those are simply package-info files which denote the root of the modules by
     * being annotated with @Module.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_annotation_API() {
        modules()
                .definedByAnnotation(Module.class)
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    /**
     * This example demonstrates how to use the slightly more generic root class API to define modules.
     * While the result in this example is the same as the above, this API in general can be used to
     * use arbitary classes as roots of modules.
     * For example if there is always a central interface denoted in some way,
     * the modules could be derived from these interfaces.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_root_class_API() {
        modules()
                .definedByRootClasses(new DescribedPredicate<JavaClass>("annotated with @" + Module.class.getSimpleName()) {
                    @Override
                    public boolean apply(JavaClass rootClass) {
                        return rootClass.isAnnotatedWith(Module.class);
                    }
                })
                .derivingModuleFromRootClassBy(
                        new DescribedFunction<JavaClass, AnnotationDescriptor<Module>>("annotation @" + Module.class.getSimpleName()) {
                            @Override
                            public AnnotationDescriptor<Module> apply(JavaClass rootClass) {
                                Module module = rootClass.getAnnotationOfType(Module.class);
                                return new AnnotationDescriptor<>(module.name(), module);
                            }
                        })
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    /**
     * This example demonstrates how to use the generic API to define modules.
     * The result in this example again is the same as the above, however in general the generic API
     * allows to derive modules in a completely customizable way.
     */
    @Test
    public void modules_should_respect_their_declared_dependencies__use_generic_API() {
        modules()
                .definedBy(identifierFromModulesAnnotation())
                .derivingModule(fromModulesAnnotation())
                .should(respectTheirDeclaredDependenciesWithin("..example.."))
                .check(classes);
    }

    private static IdentifierFromAnnotation identifierFromModulesAnnotation() {
        return new IdentifierFromAnnotation();
    }

    private static AnnotationDescriptorFunction fromModulesAnnotation() {
        return new AnnotationDescriptorFunction();
    }

    private DeclaredDependenciesCondition respectTheirDeclaredDependenciesWithin(String applicationRootPackageIdentifier) {
        return new DeclaredDependenciesCondition(applicationRootPackageIdentifier);
    }

    private static class DeclaredDependenciesCondition extends ArchCondition<ArchModule<AnnotationDescriptor<Module>>> {
        private final PackageMatcher applicationRootPackageMatcher;

        DeclaredDependenciesCondition(String applicationRootPackageIdentifier) {
            super("respect their declared dependencies within %s", applicationRootPackageIdentifier);
            this.applicationRootPackageMatcher = PackageMatcher.of(applicationRootPackageIdentifier);
        }

        @Override
        public void check(ArchModule<AnnotationDescriptor<Module>> module, ConditionEvents events) {
            Set<ModuleDependency<AnnotationDescriptor<Module>>> actualDependencies = module.getModuleDependencies();
            Set<String> allowedDependencyTargets = ImmutableSet.copyOf(module.getDescriptor().getAnnotation().allowedDependencies());

            for (ModuleDependency<?> dependency : actualDependencies) {
                if (!allowedDependencyTargets.contains(dependency.getTarget().getName().toString())) {
                    events.add(violated(dependency, dependency.getDescription()));
                }
            }

            for (Dependency undefinedDependency : withinApplicationRootPackage(module.getUndefinedDependencies())) {
                events.add(violated(undefinedDependency, "Dependency not contained in any module: " + undefinedDependency.getDescription()));
            }
        }

        private Set<Dependency> withinApplicationRootPackage(Set<Dependency> dependencies) {
            Set<Dependency> result = new HashSet<>();
            for (Dependency dependency : dependencies) {
                if (applicationRootPackageMatcher.matches(dependency.getTargetClass().getPackageName())) {
                    result.add(dependency);
                }
            }
            return result;
        }
    }

    private static class IdentifierFromAnnotation extends DescribedFunction<JavaClass, ArchModule.Identifier> {
        IdentifierFromAnnotation() {
            super("root classes with annotation @" + Module.class.getSimpleName());
        }

        @Override
        public ArchModule.Identifier apply(JavaClass javaClass) {
            return getIdentifierOfPackage(javaClass.getPackage());
        }

        private ArchModule.Identifier getIdentifierOfPackage(JavaPackage javaPackage) {
            for (JavaClass sibling : javaPackage.getClasses()) {
                if (sibling.isAnnotatedWith(Module.class)) {
                    return ArchModule.Identifier.from(sibling.getAnnotationOfType(Module.class).name());
                }
            }
            return javaPackage.getParent().isPresent()
                    ? getIdentifierOfPackage(javaPackage.getParent().get())
                    : ArchModule.Identifier.ignore();
        }
    }

    private static class AnnotationDescriptorFunction extends DescriptorFunction<AnnotationDescriptor<Module>> {
        AnnotationDescriptorFunction() {
            super("from @%s(name)", Module.class.getSimpleName());
        }

        @Override
        public AnnotationDescriptor<Module> apply(ArchModule.Identifier identifier, Set<JavaClass> containedClasses) {
            Set<JavaClass> rootClasses = new HashSet<>();
            for (JavaClass javaClass : containedClasses) {
                if (javaClass.isAnnotatedWith(Module.class)) {
                    rootClasses.add(javaClass);
                }
            }
            checkState(rootClasses.size() == 1,
                    "Could not find exactly one class annotated with @%s in package hierarchy", Module.class.getSimpleName());

            Module module = getOnlyElement(rootClasses).getAnnotationOfType(Module.class);
            return new AnnotationDescriptor<>(module.name(), module);
        }
    }
}
