/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.reha.util;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.core.SingleTransformer;


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

  /**
   * Cache the emissions if the observable completes succesfully, but clears the cache on any error (so that you can use
   * {@link Observable#retry()}
   * @param <T> the emission type
   * @return an {@link ObservableTransformer} that can be passed to {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> ObservableTransformer<T, T> cacheIfCompleted() {
    return new CacheOnlyCompletedTransformer<T>();
  }

  /**
   * Cache the emissions if the Single completes succesfully, but clears the cache on any error (so that you can use
   * {@link Single#retry()}
   * @param <T> the emission type
   * @return a {@link SingleTransformer} that can be passed to {@link Single#compose(SingleTransformer)}
   */
  public static <T> SingleTransformer<T, T> cacheSingleIfCompleted() {
    return new CacheOnlyCompletedTransformer<T>();
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

  private static final class CacheOnlyCompletedTransformer<T> implements ObservableTransformer<T, T>, SingleTransformer<T, T> {

    private Observable<T> cachedOrInProgress = null;

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
      return Observable.defer(() -> handleSubscription(upstream));
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
      return Single.defer(() -> {
        return handleSubscription(upstream.toObservable()).firstOrError();
      });
    }

    private synchronized Observable<T> handleSubscription(Observable<T> upstream) {

      Observable<T> source = cachedOrInProgress;

      if (source == null) {
        source = upstream
            .doOnError(this::onError)
            .replay()
            .autoConnect();

        cachedOrInProgress = source;
      }

      return source;
    }

    private void onError(Throwable t) {
      cachedOrInProgress = null;
    }
  }
}
