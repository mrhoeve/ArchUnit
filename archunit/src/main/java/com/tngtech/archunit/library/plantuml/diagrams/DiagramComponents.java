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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class DiagramComponents implements Iterable<DiagramComponent> {
    private final Set<DiagramComponent> components;
    private final Map<JavaClass, DiagramComponent> classesToComponents = new HashMap<>();

    private DiagramComponents(Set<DiagramComponent> components) {
        this.components = checkNotNull(components);
        for (DiagramComponent component : components) {
            for (JavaClass javaClass : component) {
                classesToComponents.put(javaClass, component);
            }
        }
    }

    @Override
    public Iterator<DiagramComponent> iterator() {
        return components.iterator();
    }

    public boolean contain(JavaClass javaClass) {
        return classesToComponents.containsKey(javaClass);
    }

    public DiagramComponent getComponentOf(JavaClass javaClass) {
        checkNotNull(javaClass, "Passed %s may not be null", JavaClass.class.getSimpleName());
        checkState(contain(javaClass), "Components don't contain any class %s", javaClass.getName());
        return classesToComponents.get(javaClass);
    }

    public static DiagramComponents.Creator group(JavaClasses classes) {
        return new Creator(classes);
    }

    public static class Creator {
        private final JavaClasses classes;

        private Creator(JavaClasses classes) {
            this.classes = checkNotNull(classes);
        }

        /**
         * FIXME
         *
         * @param packageIdentifier Pattern das angibt nach welchen Packages gruppiert werden soll. Beispiel: ..okewo.(*).. gruppiert genau nach dem Package nach 'okewo'. Vergleiche {@link PackageMatcher}
         */
        public FinalStep byPackages(final String packageIdentifier) {
            return new FinalStep(classes, new AssignmentByPackages(packageIdentifier));
        }

        public FinalStep by(DiagramComponent.Assignment componentAssignment) {
            return new FinalStep(classes, componentAssignment);
        }

        public static class FinalStep<T> {
            private final JavaClasses classes;
            private final DiagramComponent.Assignment componentAssignment;

            private FinalStep(JavaClasses classes, DiagramComponent.Assignment componentAssignment) {
                this.classes = checkNotNull(classes);
                this.componentAssignment = checkNotNull(componentAssignment);
            }

            /**
             * FIXME
             *
             * @param namingPattern Pattern mit dem der Name des Moduls festgelegt werden kann, z.B. 'Modul $1' nimmt den ersten '*' und generiert den Modulnamen daraus, ..okewo.zuzug.. -> 'Modul zuzug'
             */
            public DiagramComponents deriveNameFrom(String namingPattern) {
                Builders moduleBuilders = new Builders(namingPattern);
                for (JavaClass clazz : classes) {
                    componentAssignment.assign(clazz).ifPresent(value -> moduleBuilders.add(value, clazz));
                }
                return moduleBuilders.build();
            }
        }
    }

    private static class AssignmentByPackages implements DiagramComponent.Assignment {
        private final String packageIdentifier;

        AssignmentByPackages(String packageIdentifier) {
            this.packageIdentifier = packageIdentifier;
        }

        @Override
        public Optional<DiagramComponent.Identifier> assign(JavaClass javaClass) {
            return PackageMatcher.of(packageIdentifier)
                    .match(javaClass.getPackageName())
                    .map(result -> DiagramComponent.Identifier.ofParts(getGroups(result)));
        }

        // FIXME: Move into Result
        private List<String> getGroups(PackageMatcher.Result result) {
            List<String> groups = new ArrayList<>();
            for (int i = 1; i <= result.getNumberOfGroups(); i++) {
                groups.add(result.getGroup(i));
            }
            return groups;
        }
    }

    private static class Builders {
        private final Multimap<List<String>, JavaClass> rawComponents = HashMultimap.create();
        private final String namingPattern;

        Builders(String namingPattern) {
            this.namingPattern = namingPattern;
        }

        void add(DiagramComponent.Identifier identifier, JavaClass clazz) {
            rawComponents.put(identifier.getParts(), clazz);
        }

        DiagramComponents build() {
            ImmutableSet.Builder<DiagramComponent> result = ImmutableSet.builder();
            for (Map.Entry<List<String>, Collection<JavaClass>> entry : rawComponents.asMap().entrySet()) {
                result.add(new DiagramComponent(namingPattern, entry.getKey(), entry.getValue()));
            }
            return new DiagramComponents(result.build());
        }
    }
}
