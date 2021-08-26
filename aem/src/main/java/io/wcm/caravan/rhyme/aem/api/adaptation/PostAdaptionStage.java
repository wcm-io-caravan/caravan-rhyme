/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.aem.api.adaptation;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface PostAdaptionStage<I, M extends I> {

  PostAdaptionStage<I, M> withModifications(Consumer<M> consumer);

  PostAdaptionStage<I, M> withLinkTitle(String title);

  PostAdaptionStage<I, M> withLinkName(String name);

  PostAdaptionStage<I, M> withQueryParameterTemplate(String... names);

  PostAdaptionStage<I, M> withQueryParameters(Map<String, Object> parameters);

  PostAdaptionStage<I, M> withPartialLinkTemplate();

  M getInstance();

  Optional<I> getOptional();

  Stream<I> getStream();
}