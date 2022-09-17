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

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenConjunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;

class GivenModulesInternal<D extends ArchModule.Descriptor> implements GivenModules<D>, GivenConjunction<ArchModule<D>> {
    private final ModulesTransformer<D> transformer;

    GivenModulesInternal(TransformFunction<D> transformFunction) {
        this(new ModulesTransformer<>(transformFunction));
    }

    private GivenModulesInternal(ModulesTransformer<D> transformer) {
        this.transformer = checkNotNull(transformer);
    }

    @Override
    public ArchRule should(ArchCondition<? super ArchModule<D>> condition) {
        return all(transformer).should(condition);
    }

    @Override
    public GivenConjunction<ArchModule<D>> and(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new GivenModulesInternal<>(transformer.and(predicate));
    }

    @Override
    public GivenConjunction<ArchModule<D>> or(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new GivenModulesInternal<>(transformer.or(predicate));
    }

    @Override
    public GivenConjunction<ArchModule<D>> that(DescribedPredicate<? super ArchModule<D>> predicate) {
        return new GivenModulesInternal<>(transformer.that(predicate));
    }

    @Override
    public GivenModules<D> as(String description, Object... args) {
        return new GivenModulesInternal<>(transformer.as(String.format(description, args)));
    }
}
