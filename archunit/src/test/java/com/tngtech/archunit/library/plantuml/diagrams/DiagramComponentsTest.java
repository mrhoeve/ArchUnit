package com.tngtech.archunit.library.plantuml.diagrams;

import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.plantuml.diagrams.testcomponents.first.FirstClassOne;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class DiagramComponentsTest {
    private static JavaClasses classes = new ClassFileImporter().importPackagesOf(DiagramComponentsTest.class);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @DataProvider
    public static Object[][] components() {
        return testForEach(
                DiagramComponents
                        .group(classes)
                        .byPackages("..(testcomponents).(*)..")
                        .deriveNameFrom("Component[$1-$2]"),
                DiagramComponents
                        .group(classes)
                        .by(javaClass -> {
                            String parsedPackage = javaClass.getName().replaceAll(".*testcomponents\\.([^.]+)\\..*", "$1");
                            return Optional.of(DiagramComponent.Identifier.ofParts("testcomponents", parsedPackage));
                        })
                        .deriveNameFrom("Component[$1-$2]")
        );
    }

    @Test
    @UseDataProvider("components")
    public void groups_classes_by_package(DiagramComponents components) {
        DiagramComponent component = components.getComponentOf(classes.get(FirstClassOne.class));

        assertThat(component.getDescription()).isEqualTo("Component[testcomponents-first]");
    }

    @Test
    public void rejects_retrieving_null() {
        DiagramComponents components = anyDiagramComponents();

        thrown.expect(NullPointerException.class);
        thrown.expectMessage(JavaClass.class.getSimpleName());
        thrown.expectMessage("null");
        components.getComponentOf(null);
    }

    @Test
    public void rejects_retrieving_an_uncontained_class() {
        DiagramComponents components = anyDiagramComponents();

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Components don't contain any class " + DiagramComponentsTest.class.getName());
        components.getComponentOf(classes.get(DiagramComponentsTest.class));
    }

    @Test
    public void rejects_non_unique_identifiers() {
        Assert.fail("Add test");
    }

    private DiagramComponents anyDiagramComponents() {
        return DiagramComponents
                .group(classes)
                .byPackages("notthere")
                .deriveNameFrom("irrelevant");
    }
}
