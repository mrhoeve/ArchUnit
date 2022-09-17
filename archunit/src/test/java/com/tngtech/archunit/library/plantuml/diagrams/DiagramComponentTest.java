package com.tngtech.archunit.library.plantuml.diagrams;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class DiagramComponentTest {
    @Test
    public void derives_description_from_namingPattern() {
        DiagramComponent component =
                new DiagramComponent("Something [$2] [$1]", ImmutableList.of("first", "second"), Collections.<JavaClass>emptyList());

        assertThat(component.getDescription()).isEqualTo("Something [second] [first]");

    }

    @Test
    public void derives_name_without_illegal_characters() {
        DiagramComponent component = new DiagramComponent("Illegal[$1]", singletonList("irrelevant"), Collections.<JavaClass>emptyList());

        assertThat(component.getName()).matches("\\w+");
    }
}