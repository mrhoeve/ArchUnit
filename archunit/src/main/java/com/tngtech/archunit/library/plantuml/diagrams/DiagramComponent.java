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
package com.tngtech.archunit.library.plantuml.diagrams;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

public class DiagramComponent implements HasDescription, Iterable<JavaClass>
{
    private final List<String> groups;
    private final Collection<JavaClass> classes;
    private final String description;
    private final String name;

    DiagramComponent(String namingPattern, List<String> groups, Collection<JavaClass> classes)
    {
        this.groups = groups;
        this.classes = classes;
        description = format(namingPattern, groups);
        name = removeIllegalCharacters(description);
    }

    private String removeIllegalCharacters(String string)
    {
        return string.replaceAll("[^\\w]", "");
    }

    String format(String pattern, List<String> matchingGroups)
    {
        String result = pattern;

        for (int i = 1; i <= matchingGroups.size(); ++i)
        {
            result = result.replace("$" + i, matchingGroups.get(i - 1));
        }

        return result;
    }

    public Set<Dependency> getDependencies()
    {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : classes)
        {
            result.addAll(filterUncontainedTargets(javaClass.getDirectDependenciesFromSelf()));
        }
        return result.build();
    }

    private Iterable<Dependency> filterUncontainedTargets(Set<Dependency> dependencies)
    {
        Set<Dependency> result = new HashSet<>();
        for (Dependency dependency : dependencies)
        {
            if (!classes.contains(dependency.getTargetClass()))
            {
                result.add(dependency);
            }
        }
        return result;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public Iterator<JavaClass> iterator()
    {
        return classes.iterator();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(groups);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        final DiagramComponent other = (DiagramComponent) obj;
        return Objects.equals(this.groups, other.groups);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " '" + description + "'";
    }

    public interface Assignment
    {
        /**
         * FIXME
         * @param javaClass
         * @return
         */
        Optional<Identifier> assign(JavaClass javaClass);
    }

    public static class Identifier
    {
        private final List<String> parts;

        private Identifier(List<String> parts)
        {
            this.parts = parts;
        }

        public List<String> getParts()
        {
            return parts;
        }

        public static Identifier ofParts(String... parts)
        {
            return ofParts(ImmutableList.copyOf(parts));
        }

        public static Identifier ofParts(List<String> parts)
        {
            return new Identifier(parts);
        }
    }
}
