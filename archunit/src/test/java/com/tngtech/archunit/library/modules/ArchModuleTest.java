package com.tngtech.archunit.library.modules;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ArchModuleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void identifier_equals_hashcode_and_toString() {
        ArchModule.Identifier original = ArchModule.Identifier.from("one", "two");
        ArchModule.Identifier equal = ArchModule.Identifier.from("one", "two");
        ArchModule.Identifier different = ArchModule.Identifier.from("one", "other");

        assertThat(equal).isEqualTo(original);
        assertThat(equal.hashCode()).isEqualTo(original.hashCode());
        assertThat(equal).isNotEqualTo(different);
    }

    @Test
    public void identifier_parts() {
        ArchModule.Identifier identifier = ArchModule.Identifier.from("one", "two", "three");

        assertThat(identifier).containsExactly("one", "two", "three");
        assertThat(identifier.getNumberOfParts()).as("number of parts").isEqualTo(3);
        assertThat(identifier.getPart(1)).isEqualTo("one");
        assertThat(identifier.getPart(2)).isEqualTo("two");
        assertThat(identifier.getPart(3)).isEqualTo("three");
    }

    @DataProvider
    public static Object[][] illegal_indizes() {
        return $$(
                $(ArchModule.Identifier.from("one"), 0),
                $(ArchModule.Identifier.from("one"), 2)
        );
    }

    @Test
    @UseDataProvider("illegal_indizes")
    public void rejects_index_out_of_range(ArchModule.Identifier identifier, int illegalIndex) {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(String.valueOf(illegalIndex));
        thrown.expectMessage("out of bounds");

        identifier.getPart(illegalIndex);
    }
}