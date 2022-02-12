package io.wcm.caravan.rhyme.microbenchmark;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;

public class StaticResourceImpl implements LinkableBenchmarkResource {

  private static final Single<ObjectNode> STATE_SINGLE = Single.just(TestState.createTestJson());

  private static final Link LINK = new Link("/foo");

  private static final Observable<LinkableBenchmarkResource> LINKED = Observable.range(0, ResourceParameters.numLinkedResource())
      .map(i -> (LinkableBenchmarkResource)new StaticResourceImpl())
      .cache();

  private static final Observable<EmbeddableBenchmarkResource> EMBEDDED = Observable.range(0, ResourceParameters.numEmbeddedResource())
      .map(i -> (EmbeddableBenchmarkResource)new Embedded())
      .cache();

  @Override
  public Single<ObjectNode> getState() {
    return STATE_SINGLE;
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked1() {
    return LINKED;
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked2() {
    return LINKED;
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked3() {
    return LINKED;
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked4() {
    return LINKED;
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked5() {
    return LINKED;
  }

  @Override
  public Observable<EmbeddableBenchmarkResource> getEmbedded1() {
    return EMBEDDED;
  }

  @Override
  public Link createLink() {
    return LINK;
  }


  static class Embedded implements EmbeddableBenchmarkResource {

    @Override
    public Single<ObjectNode> getState() {
      return STATE_SINGLE;
    }

    @Override
    public Maybe<LinkableBenchmarkResource> getRelated() {
      return LINKED.firstElement();
    }
  }
}
