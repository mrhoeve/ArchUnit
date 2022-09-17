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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.ForwardingSet;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;

public class ArchModule<DESCRIPTOR extends ArchModule.Descriptor> extends ForwardingSet<JavaClass> {
    private final Identifier identifier;
    private final DESCRIPTOR descriptor;
    private final Set<JavaClass> classes;
    private final Set<Dependency> classDependencies;
    private Set<ModuleDependency<DESCRIPTOR>> moduleDependencies;
    private Set<Dependency> undefinedDependencies;

    ArchModule(Identifier identifier, DESCRIPTOR descriptor, Set<JavaClass> classes) {
        this.identifier = checkNotNull(identifier);
        this.descriptor = checkNotNull(descriptor);
        this.classes = ImmutableSet.copyOf(classes);
        classDependencies = filterClassDependenciesOutsideOfSelf(classes);
    }

    private ImmutableSet<Dependency> filterClassDependenciesOutsideOfSelf(Set<JavaClass> classes) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : classes) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                if (!classes.contains(dependency.getTargetClass())) {
                    result.add(dependency);
                }
            }
        }
        return result.build();
    }

    void setModuleDependencies(Set<ModuleDependency<DESCRIPTOR>> moduleDependencies) {
        this.moduleDependencies = ImmutableSet.copyOf(moduleDependencies);
        this.undefinedDependencies = ImmutableSet.copyOf(Sets.difference(classDependencies, toClassDependencies(moduleDependencies)));
    }

    private Set<Dependency> toClassDependencies(Set<ModuleDependency<DESCRIPTOR>> moduleDependencies) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (ModuleDependency<DESCRIPTOR> moduleDependency : moduleDependencies) {
            result.addAll(moduleDependency.toClassDependencies());
        }
        return result.build();
    }

    @Override
    protected Set<JavaClass> delegate() {
        return classes;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Name getName() {
        return descriptor.getName();
    }

    public DESCRIPTOR getDescriptor() {
        return descriptor;
    }

    public Set<Dependency> getClassDependencies() {
        return classDependencies;
    }

    public Set<ModuleDependency<DESCRIPTOR>> getModuleDependencies() {
        return moduleDependencies;
    }

    public Set<Dependency> getUndefinedDependencies() {
        return undefinedDependencies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final ArchModule<?> other = (ArchModule<?>) obj;
        return Objects.equals(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier=" + getIdentifier() + ", name=" + getName() + '}';
    }

    public static final class Identifier implements Iterable<String> {
        private final List<String> parts;

        private Identifier(List<String> parts) {
            this.parts = ImmutableList.copyOf(parts);
        }

        public static Identifier from(String... parts) {
            return from(ImmutableList.copyOf(parts));
        }

        public static Identifier from(List<String> parts) {
            return new Identifier(parts);
        }

        public static Identifier ignore() {
            return new Identifier(emptyList());
        }

        /**
         * @return The number of (textual) parts this identifier consists of.
         */
        public int getNumberOfParts() {
            return parts.size();
        }

        /**
         * @param index Index of the requested (textual) part
         * @return Part with the given index; indizes are 1-based (i.e. {@link #getPart(int) getPart(1)}) returns the first part.
         */
        public String getPart(int index) {
            checkArgument(index >= 1 && index <= parts.size(), "Index %d is out of bounds", index);
            return parts.get(index - 1);
        }

        boolean shouldBeConsidered() {
            return !parts.isEmpty();
        }

        @Override
        public Iterator<String> iterator() {
            return parts.iterator();
        }

        @Override
        public int hashCode() {
            return Objects.hash(parts);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Identifier other = (Identifier) obj;
            return Objects.equals(this.parts, other.parts);
        }

        @Override
        public String toString() {
            return parts.toString();
        }
    }

    public static final class Name {
        private final String value;

        private Name(String value) {
            checkArgument(!isNullOrEmpty(value), "Module name must not be null or empty");
            this.value = value;
        }

        public static Name from(String value) {
            return new Name(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Name other = (Name) obj;
            return Objects.equals(this.value, other.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public interface Descriptor {
        Name getName();

        static Descriptor create(final Name name) {
            return () -> name;
        }
    }
}
