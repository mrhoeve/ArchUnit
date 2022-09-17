/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.modules;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.base.DescribedFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

public class ModuleRuleDefinition {
    public static Creator modules() {
        return new Creator();
    }

    public static class Creator {
        private Creator() {
        }

        public GenericDefinition definedBy(final DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            return new GenericDefinition(identifierFunction);
        }

        public RootClassesDefinition definedByRootClasses(DescribedPredicate<JavaClass> predicate) {
            return new RootClassesDefinition(predicate);
        }

        public <A extends Annotation> GivenObjects<ArchModule<AnnotationDescriptor<A>>> definedByAnnotation(
                final Class<A> annotationType) {

            return definedByAnnotation(annotationType, input -> {
                try {
                    return (String) input.annotationType().getMethod("name").invoke(input);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new ReflectionException(
                            String.format("Could not invoke @%s.name() -> Supplied annotation must provide a method 'String name()'. "
                                            + "Otherwise use modules().definedByAnnotation(annotationType, nameFunction).",
                                    input.annotationType().getSimpleName()),
                            e);
                }
            });
        }

        public <A extends Annotation> GivenObjects<ArchModule<AnnotationDescriptor<A>>> definedByAnnotation(
                final Class<A> annotationType, final Function<A, String> nameFunction) {

            return definedByRootClasses(
                    new DescribedPredicate<JavaClass>("from annotation @" + annotationType.getSimpleName()) {
                        @Override
                        public boolean test(JavaClass input) {
                            return input.isAnnotatedWith(annotationType);
                        }
                    }
            ).derivingModuleFromRootClassBy(
                    new DescribedFunction<JavaClass, AnnotationDescriptor<A>>("annotation @" + annotationType.getSimpleName()) {
                        @Override
                        public AnnotationDescriptor<A> apply(JavaClass javaClass) {
                            return new AnnotationDescriptor<>(
                                    nameFunction.apply(javaClass.getAnnotationOfType(annotationType)),
                                    javaClass.getAnnotationOfType(annotationType));
                        }
                    }
            ).as("modules defined by annotation @%s", annotationType.getSimpleName());
        }
    }

    public static class GenericDefinition {
        private final DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction;

        private GenericDefinition(DescribedFunction<JavaClass, ArchModule.Identifier> identifierFunction) {
            this.identifierFunction = identifierFunction;
        }

        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModule(final DescriptorFunction<D> descriptorFunction) {
            return new GivenModulesInternal<>(new TransformFunction<D>() {
                @Override
                Iterable<ArchModule<D>> apply(JavaClasses classes) {
                    return ArchModules
                            .defineBy(identifierFunction)
                            .describeBy(definition -> descriptorFunction.apply(definition.getIdentifier(), definition.getContainedClasses()))
                            .modularize(classes);
                }
            }).as("modules defined by %s deriving module %s", identifierFunction.getDescription(), descriptorFunction.getDescription());
        }

        @PublicAPI(usage = INHERITANCE)
        public abstract static class DescriptorFunction<D extends ArchModule.Descriptor> implements HasDescription {
            private final String description;

            protected DescriptorFunction(String description, Object... args) {
                this.description = String.format(description, args);
            }

            public abstract D apply(ArchModule.Identifier identifier, Set<JavaClass> containedClasses);

            @Override
            public String getDescription() {
                return description;
            }
        }
    }

    public static class RootClassesDefinition {
        private final DescribedPredicate<JavaClass> rootClassPredicate;
        private final String descriptionStart;

        private RootClassesDefinition(DescribedPredicate<JavaClass> rootClassPredicate) {
            this.rootClassPredicate = rootClassPredicate;
            descriptionStart = "modules defined by root classes " + rootClassPredicate.getDescription();
        }

        public <D extends ArchModule.Descriptor> GivenModules<D> derivingModuleFromRootClassBy(
                final DescribedFunction<JavaClass, D> descriptorFunction) {

            String description = descriptionStart + " deriving module from root class by " + descriptorFunction.getDescription();

            return new GivenModulesInternal<>(new TransformFunction<D>() {
                @Override
                Iterable<ArchModule<D>> apply(JavaClasses classes) {
                    final RawModules<D> rawModules = new RawModules<>();
                    for (JavaClass rootClass : classes.that(rootClassPredicate)) {
                        D descriptor = descriptorFunction.apply(rootClass);
                        rawModules.add(rootClass.getPackageName(), descriptor);
                    }

                    return ArchModules.defineBy(rawModules::getIdentifierFor)
                            .describeBy(definition -> rawModules.getDescriptorFor(definition.getIdentifier()))
                            .modularize(classes);
                }
            }).as(description);
        }
    }

    private static class RawModules<D extends ArchModule.Descriptor> {
        private final Map<ArchModule.Identifier, D> identifierToDescriptor = new HashMap<>();
        private final Map<ArchModule.Identifier, String> identifiersToPackageNames = new HashMap<>();

        ArchModule.Identifier getIdentifierFor(JavaClass javaClass) {
            for (Map.Entry<ArchModule.Identifier, String> identifierToPackageName : identifiersToPackageNames.entrySet()) {
                if (containedInPackage(javaClass, identifierToPackageName.getValue())) {
                    return identifierToPackageName.getKey();
                }
            }
            return ArchModule.Identifier.ignore();
        }

        private boolean containedInPackage(JavaClass javaClass, String packageName) {
            return javaClass.getPackageName().equals(packageName) || javaClass.getPackageName().startsWith(packageName + ".");
        }

        D getDescriptorFor(ArchModule.Identifier identifier) {
            return identifierToDescriptor.get(identifier);
        }

        public void add(String packageName, D descriptor) {
            ArchModule.Identifier identifier = ArchModule.Identifier.from(descriptor.getName().toString());
            checkState(identifierToDescriptor.put(identifier, descriptor) == null,
                    "Module name must be unique, but %s is not", descriptor.getName());
            identifiersToPackageNames.put(identifier, packageName);
        }
    }
}
