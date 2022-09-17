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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.library.modules.ArchModule.Identifier;
import com.tngtech.archunit.library.modules.ArchModule.Name;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Multimaps.asMap;
import static com.tngtech.archunit.core.domain.PackageMatcher.TO_GROUPS;
import static com.tngtech.archunit.library.modules.ArchModule.Identifier.ignore;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ArchModules<D extends ArchModule.Descriptor> implements Iterable<ArchModule<D>> {
    private final Map<Identifier, ArchModule<D>> modulesByIdentifier;
    private final Map<Name, ArchModule<D>> modulesByName;

    private ArchModules(Set<ArchModule<D>> modules) {
        this.modulesByIdentifier = modules.stream().collect(toMap(ArchModule::getIdentifier, identity()));
        this.modulesByName = modules.stream().collect(toMap(ArchModule::getName, identity()));
        for (ArchModule<D> origin : modules) {
            origin.setModuleDependencies(createModuleDependencies(origin, modules));
        }
    }

    private ImmutableSet<ModuleDependency<D>> createModuleDependencies(ArchModule<D> origin, Set<ArchModule<D>> modules) {
        ImmutableSet.Builder<ModuleDependency<D>> moduleDependencies = ImmutableSet.builder();
        for (ArchModule<D> target : Sets.difference(modules, origin)) {
            ModuleDependency.tryCreate(origin, target).ifPresent(moduleDependencies::add);
        }
        return moduleDependencies.build();
    }

    @Override
    public Iterator<ArchModule<D>> iterator() {
        return modulesByIdentifier.values().iterator();
    }

    public static Creator defineByPackages(String packageIdentifier) {
        return new Creator(identifierByPackage(packageIdentifier));
    }

    public static Creator defineBy(Function<JavaClass, Identifier> identifierFunction) {
        return new Creator(checkNotNull(identifierFunction));
    }

    public ArchModule<D> getByIdentifier(String... identifier) {
        Identifier moduleIdentifier = Identifier.from(identifier);
        checkArgument(modulesByIdentifier.containsKey(moduleIdentifier), "There is no %s with identifier %s", ArchModule.class.getSimpleName(),
                moduleIdentifier);
        return modulesByIdentifier.get(moduleIdentifier);
    }

    public ArchModule<D> getByName(String name) {
        Name moduleName = Name.from(name);
        checkArgument(modulesByName.containsKey(moduleName), "There is no %s with name %s", ArchModule.class.getSimpleName(), moduleName);
        return modulesByName.get(moduleName);
    }

    public Set<String> getNames() {
        return modulesByName.keySet().stream().map(toStringFunction()).collect(toImmutableSet());
    }

    private static Function<JavaClass, Identifier> identifierByPackage(final String packageIdentifier) {
        return javaClass -> {
            PackageMatcher packageMatcher = PackageMatcher.of(packageIdentifier);
            Optional<PackageMatcher.Result> result = packageMatcher.match(javaClass.getPackageName());
            return result.map(TO_GROUPS).map(Identifier::from).orElse(ignore());
        };
    }

    public static class Creator {
        private final Function<JavaClass, Identifier> identifierFunction;
        private final Function<Identifier, String> deriveName;

        private Creator(Function<JavaClass, Identifier> identifierFunction) {
            this(identifierFunction, DEFAULT_NAMING_STRATEGY);
        }

        private Creator(Function<JavaClass, Identifier> identifierFunction, Function<Identifier, String> deriveName) {
            this.identifierFunction = identifierFunction;
            this.deriveName = deriveName;
        }

        private static final Function<Identifier, String> DEFAULT_NAMING_STRATEGY =
                identifier -> String.format("Module [%s]", Joiner.on(":").join(identifier));

        public ArchModules<?> modularize(JavaClasses classes) {
            return defineBy(identifierFunction)
                    .describeBy(definition -> ArchModule.Descriptor.create(Name.from(deriveName.apply(definition.getIdentifier()))))
                    .modularize(classes);
        }

        public <D extends ArchModule.Descriptor> WithGenericDescriptor<D> describeBy(Function<Definition, D> descriptorFunction) {
            return new WithGenericDescriptor<>(identifierFunction, descriptorFunction);
        }

        public Creator deriveNameFrom(final String namingPattern) {
            return new Creator(identifierFunction, identifier -> {
                String result = namingPattern;
                for (int i = 1; i <= identifier.getNumberOfParts(); i++) {
                    result = result
                            .replace("$" + i, identifier.getPart(i))
                            .replace("${" + i + "}", identifier.getPart(i));
                }
                return result;
            });
        }

        public static class WithGenericDescriptor<D extends ArchModule.Descriptor> {
            private final Function<JavaClass, Identifier> identifierFunction;
            private final Function<Definition, D> descriptorFunction;

            private WithGenericDescriptor(Function<JavaClass, Identifier> identifierFunction, Function<Definition, D> descriptorFunction) {
                this.identifierFunction = checkNotNull(identifierFunction);
                this.descriptorFunction = checkNotNull(descriptorFunction);
            }

            public ArchModules<D> modularize(JavaClasses classes) {
                SetMultimap<Identifier, JavaClass> classesByIdentifier = groupClassesByIdentifier(classes);

                Set<ArchModule<D>> modules = new HashSet<>();
                for (Map.Entry<Identifier, Set<JavaClass>> entry : asMap(classesByIdentifier).entrySet()) {
                    Identifier identifier = entry.getKey();
                    Set<JavaClass> containedClasses = entry.getValue();
                    D descriptor = descriptorFunction.apply(new Definition(identifier, containedClasses));
                    modules.add(new ArchModule<>(identifier, descriptor, containedClasses));
                }
                return new ArchModules<>(modules);
            }

            private SetMultimap<Identifier, JavaClass> groupClassesByIdentifier(JavaClasses classes) {
                SetMultimap<Identifier, JavaClass> classesByIdentifier = HashMultimap.create();
                for (JavaClass javaClass : classes) {
                    Identifier identifier = identifierFunction.apply(javaClass);
                    if (identifier.shouldBeConsidered()) {
                        classesByIdentifier.put(identifier, javaClass);
                    }
                }
                return classesByIdentifier;
            }
        }
    }

    public static class Definition {
        private final Identifier identifier;
        private final Set<JavaClass> containedClasses;

        private Definition(Identifier identifier, Set<JavaClass> containedClasses) {
            this.identifier = identifier;
            this.containedClasses = containedClasses;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public Set<JavaClass> getContainedClasses() {
            return containedClasses;
        }
    }
}
