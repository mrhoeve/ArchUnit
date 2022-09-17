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

import java.lang.annotation.Annotation;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

@PublicAPI(usage = ACCESS)
public final class AnnotationDescriptor<A extends Annotation> implements ArchModule.Descriptor {
    private final ArchModule.Name name;
    private final A annotation;

    public AnnotationDescriptor(String moduleName, A annotation) {
        name = ArchModule.Name.from(moduleName);
        this.annotation = checkNotNull(annotation);
    }

    @Override
    public ArchModule.Name getName() {
        return name;
    }

    public A getAnnotation() {
        return annotation;
    }
}
