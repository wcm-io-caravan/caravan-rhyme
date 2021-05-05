/*-
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2020 wcm.io Caravan
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

package io.wcm.caravan.rhyme.osgi.sampleservice.impl.util;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;


/**
 * Contains static utility methods to simplify working with RX Java streams
 */
public final class RxJavaTransformers {

  private RxJavaTransformers() {
    // contains only static methods
  }

  /**
   * @param asyncFilterFunc a function that returns a boolean {@link Single} to decide whether a given item should
   *          be filtered
   * @return an {@link ObservableTransformer} that can be passed to {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> ObservableTransformer<T, T> filterWith(Function<T, Single<Boolean>> asyncFilterFunc) {
    return new AsyncFilterTransformer<T>(asyncFilterFunc);
  }

  private static final class AsyncFilterTransformer<T> implements ObservableTransformer<T, T> {

    private final Function<T, Single<Boolean>> asyncFilterFunc;

    private AsyncFilterTransformer(Function<T, Single<Boolean>> asyncFilterFunc) {
      this.asyncFilterFunc = asyncFilterFunc;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {

      return upstream
          .concatMapEager(this::createPairOfItemAndFilterFlag)
          .filter(Pair::getValue)
          .map(Pair::getKey);
    }

    private Observable<Pair<T, Boolean>> createPairOfItemAndFilterFlag(T item) {

      Single<Boolean> filterFlags = asyncFilterFunc.apply(item);

      return filterFlags
          .map(flag -> Pair.of(item, flag))
          // convert the Single to an Observable so that we can use concatMapEager
          .toObservable();
    }
  }
}
