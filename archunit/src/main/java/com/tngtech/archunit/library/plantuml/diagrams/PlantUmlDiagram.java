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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class PlantUmlDiagram {
    private final String plantUmlDiagramText;

    PlantUmlDiagram(String plantUmlDiagramText) {
        this.plantUmlDiagramText = plantUmlDiagramText;
    }

    public void writeTo(String diagramPath) {
        writeTo(Paths.get(diagramPath));
    }

    public void writeTo(File diagramFile) {
        writeTo(diagramFile.toPath());
    }

    public void writeTo(Path diagramPath) {
        try {
            Files.write(diagramPath, plantUmlDiagramText.getBytes(UTF_8));
        } catch (IOException e) {
            throw new OutputException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(plantUmlDiagramText);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PlantUmlDiagram other = (PlantUmlDiagram) obj;
        return Objects.equals(this.plantUmlDiagramText, other.plantUmlDiagramText);
    }

    @Override
    public String toString() {
        return plantUmlDiagramText;
    }

    static class OutputException extends RuntimeException {
        private OutputException(IOException e) {
            super(e);
        }
    }

}
