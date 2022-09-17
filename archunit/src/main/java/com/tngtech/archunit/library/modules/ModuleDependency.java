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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

public class ModuleDependency<D extends ArchModule.Descriptor> implements HasDescription {
    private final ArchModule<D> origin;
    private final ArchModule<D> target;
    private final Set<Dependency> classDependencies;

    private ModuleDependency(ArchModule<D> origin, ArchModule<D> target, Set<Dependency> classDependencies) {
        this.origin = origin;
        this.target = target;
        this.classDependencies = classDependencies;
    }

    public ArchModule<D> getOrigin() {
        return origin;
    }

    public ArchModule<D> getTarget() {
        return target;
    }

    public Set<Dependency> toClassDependencies() {
        return classDependencies;
    }

    @Override
    public String getDescription() {
        String classDescriptions = classDependencies.stream()
                .map(HasDescription::getDescription)
                .collect(joining(lineSeparator()));

        return String.format("Module Dependency [%s -> %s]:%n%s", origin.getName(), target.getName(), classDescriptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin.getIdentifier(), target.getIdentifier());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ModuleDependency<?> other = (ModuleDependency<?>) obj;
        return Objects.equals(this.origin.getIdentifier(), other.origin.getIdentifier())
                && Objects.equals(this.target.getIdentifier(), other.target.getIdentifier());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "origin=" + origin +
                ", target=" + target +
                '}';
    }

    static <D extends ArchModule.Descriptor> Optional<ModuleDependency<D>> tryCreate(ArchModule<D> origin, ArchModule<D> target) {
        Set<Dependency> classDependencies = filterDependenciesBetween(origin, target);
        return !classDependencies.isEmpty()
                ? Optional.of(new ModuleDependency<>(origin, target, classDependencies))
                : Optional.empty();
    }

    private static Set<Dependency> filterDependenciesBetween(ArchModule<?> origin, ArchModule<?> target) {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (Dependency dependency : origin.getClassDependencies()) {
            if (target.contains(dependency.getTargetClass())) {
                result.add(dependency);
            }
        }
        return result.build();
    }
}
