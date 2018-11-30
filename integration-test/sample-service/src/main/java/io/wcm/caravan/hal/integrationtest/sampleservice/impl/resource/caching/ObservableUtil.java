/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;


/**
 * Contains static utility methods to simplify working with RX Java streams
 */
public final class ObservableUtil {

  private ObservableUtil() {
    // contains only static methods
  }

  /**
   * @param asyncFilterFunc a function that returns a boolean {@link Observable} to decide whether a given item should
   *          be filtered
   * @return a {@link ObservableTransformer} that can be passed to {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> ObservableTransformer<T, T> filterWith(Function<T, Single<Boolean>> asyncFilterFunc) {
    return obs -> {

      return obs.flatMapSingle(itemToFilter -> {
        Single<Boolean> rxFilterFlags = asyncFilterFunc.apply(itemToFilter);
        return rxFilterFlags.map(flag -> Pair.of(itemToFilter, flag));
      })
          .filter(pair -> pair.getRight())
          .map(pair -> pair.getLeft());

    };
  }
}
