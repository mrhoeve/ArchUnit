package com.tngtech.archunit.library.plantuml.diagrams;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import net.sourceforge.plantuml.UmlDiagramType;
import net.sourceforge.plantuml.syntax.SyntaxResult;
import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static net.sourceforge.plantuml.syntax.SyntaxChecker.checkSyntax;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class PlantUmlDiagramCreatorTest {
    // For whatever reason this might be called "description" diagram :-(
    private static final UmlDiagramType COMPONENT_DIAGRAM_TYPE = UmlDiagramType.DESCRIPTION;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void creates_diagram_of_single_component() {
        JavaClasses classes = new ClassFileImporter().importClasses(getClass());
        DiagramComponents components = DiagramComponents.group(classes)
                .byPackages("(**)")
                .deriveNameFrom("Component[$1]");

        PlantUmlDiagram diagram = createDiagram(components);

        String expectedComponentName = String.format("\"Component[%s]\"", getClass().getPackage().getName());
        assertThat(diagram.toString()).contains(String.format(
                "component %s as Component%s", expectedComponentName,
                getClass().getPackage().getName().replace(".", "")));
    }

    @Test
    public void creates_diagram_of_three_components() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(getClass());
        DiagramComponents components = DiagramComponents.group(classes)
                .byPackages("(**)")
                .deriveNameFrom("Component[$1]");

        PlantUmlDiagram diagram = createDiagram(components);

        assertThat(linesOf(diagram)).has(lineMatching(".*first.*-->.*second.*"));
        assertThat(linesOf(diagram)).has(lineMatching(".*first.*-->.*third.*"));
        assertThat(linesOf(diagram)).has(lineMatching(".*second.*-->.*third.*"));
        assertThat(linesOf(diagram)).has(lineMatching(".*third.*-->.*first.*"));
    }

    @DataProvider
    public static Object[][] diagram_IO_test_cases() {
        return testForEach(
                new DiagramIoTestCase("Write Diagram to File") {
                    @Override
                    public void execute(File temporaryFile) {
                        createSomeDiagram().writeTo(temporaryFile);
                    }
                },
                new DiagramIoTestCase("Write Diagram to Path") {
                    @Override
                    public void execute(File temporaryFile) {
                        createSomeDiagram().writeTo(temporaryFile.toPath());
                    }
                },
                new DiagramIoTestCase("Write Diagram to File name") {
                    @Override
                    public void execute(File temporaryFile) {
                        createSomeDiagram().writeTo(temporaryFile.getAbsolutePath());
                    }
                }
        );
    }

    @Test
    @UseDataProvider("diagram_IO_test_cases")
    public void writes_diagram_to_file(DiagramIoTestCase testCase) throws IOException {
        File file = temporaryFolder.newFile("some.puml");
        testCase.execute(file);
        checkSyntax(new String(readAllBytes(file.toPath()), UTF_8));
    }

    private static PlantUmlDiagram createSomeDiagram() {
        JavaClasses classes = new ClassFileImporter().importClasses(PlantUmlDiagramCreatorTest.class);
        DiagramComponents components = DiagramComponents.group(classes)
                .byPackages("(**)")
                .deriveNameFrom("Component[$1]");

        return createDiagram(components);
    }

    private Condition<List<? extends String>> lineMatching(final String line) {
        final Pattern pattern = Pattern.compile(line);
        return new Condition<List<? extends String>>(String.format("line matching '%s'", line)) {
            @Override
            public boolean matches(List<? extends String> lines) {
                for (String line : lines) {
                    if (pattern.matcher(line).matches()) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private List<String> linesOf(PlantUmlDiagram diagram) {
        return Splitter.on(lineSeparator()).splitToList(diagram.toString());
    }

    private static PlantUmlDiagram createDiagram(DiagramComponents components) {
        PlantUmlDiagram diagram = new PlantUmlDiagramCreator().createDiagramOf(components);
        assertValidSyntax(diagram.toString());
        return diagram;
    }

    private static void assertValidSyntax(String diagramString) {
        SyntaxResult result = checkSyntax(diagramString.replace("\r\n", "\n"));
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getUmlDiagramType()).isEqualTo(COMPONENT_DIAGRAM_TYPE);
    }

    private static abstract class DiagramIoTestCase {
        private final String description;

        DiagramIoTestCase(String description) {
            this.description = description;
        }

        abstract void execute(File temporaryFile);

        @Override
        public String toString() {
            return description;
        }
    }
}