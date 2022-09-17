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

import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.io.CharStreams;
import com.tngtech.archunit.core.domain.Dependency;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * Quick'n Dirty Hack um mit dem aktuellen Release Slices/Module (i.e. gecapturte Packages ala ..okewo.(*)..) als PlantUML Component-Diagramm mit Abhaengigkeiten zu zeichnen.
 */
public class PlantUmlDiagramCreator implements DiagramCreator {
    @Override
    public PlantUmlDiagram createDiagramOf(DiagramComponents components) {
        String diagramTemplate = readDiagramTemplate();
        String body = createBody(components);
        return new PlantUmlDiagram(diagramTemplate.replace("${body}", body));
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private String readDiagramTemplate() {
        try (InputStreamReader is = new InputStreamReader(
                Files.newInputStream(new File(PlantUmlDiagramCreator.class.getResource("diagram.puml.template").toURI()).toPath()), UTF_8)) {
            return CharStreams.toString(is);
        } catch (Exception e) {
            throw new RenderingFailedException(e);
        }
    }

    private static String createBody(DiagramComponents components) {
        List<String> lines = new ArrayList<>();
        for (DiagramComponent component : components) {
            lines.add("component \"" + component.getDescription() + "\" as " + component.getName());
        }
        lines.add("");
        for (Multiset.Entry<String> entry : getFormattedDependenciesWithNumber(components).entrySet()) {
            lines.add(entry.getElement() + ": " + entry.getCount());
        }
        return Joiner.on(lineSeparator()).join(lines);
    }

    private static Multiset<String> getFormattedDependenciesWithNumber(DiagramComponents components) {
        final Map<Dependency, String> result = new HashMap<>();
        for (DiagramComponent component : components) {
            for (Dependency dependency : relevant(components, component.getDependencies())) {
                result.put(dependency, formatDependency(components, dependency));
            }
        }
        return TreeMultiset.create(result.values());
    }

    private static String formatDependency(DiagramComponents components, Dependency dependency) {
        return String.format("[%s] --> [%s]",
                components.getComponentOf(dependency.getOriginClass()).getName(),
                components.getComponentOf(dependency.getTargetClass()).getName());
    }

    private static Iterable<? extends Dependency> relevant(final DiagramComponents components, Set<Dependency> dependencies) {
        return dependencies.stream()
                .filter(dependency -> components.contain(dependency.getTargetClass()))
                .collect(toList());
    }

    private static class RenderingFailedException extends RuntimeException {
        RenderingFailedException(Exception e) {
            super(e);
        }
    }
}
