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

import java.util.function.Function;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

abstract class TransformFunction<D extends ArchModule.Descriptor> {
    abstract Iterable<ArchModule<D>> apply(JavaClasses classes);

    static class From {
        static <D extends ArchModule.Descriptor> TransformFunction<D> functions(
                final Function<JavaClass, ArchModule.Identifier> identifierFunction,
                final Function<ArchModules.Definition, D> descriptorFunction) {

            return new TransformFunction<D>() {
                @Override
                Iterable<ArchModule<D>> apply(JavaClasses classes) {
                    return ArchModules.defineBy(identifierFunction).describeBy(descriptorFunction).modularize(classes);
                }
            };
        }
    }
}
