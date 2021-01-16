/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.rhyme.util;

import static io.wcm.caravan.rhyme.util.RxJavaTransformers.filterWith;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;


public class RxJavaTransformersTest {

  @Test
  public void filterWith_should_filter_by_async_expression() throws Exception {

    String filtered = Observable.just("cat", "dog", "monkey", "owl")
        .compose(filterWith(string -> Single.just(string.length() > 3)))
        .singleOrError().blockingGet();

    assertThat(filtered).isEqualTo("monkey");
  }

}
