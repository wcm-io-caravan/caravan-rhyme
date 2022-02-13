package io.wcm.caravan.rhyme.microbenchmark.resources;

import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_EMBEDDED_RESOURCES;
import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_LINKED_RESOURCES;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class StaticResourceImpl implements LinkableBenchmarkResource {

  private static final Single<ObjectNode> STATE_SINGLE = Single.just(StatePojo.createTestJson());

  private static final Observable<LinkableBenchmarkResource> LINKED = Observable.range(0, NUM_LINKED_RESOURCES)
      .map(i -> (LinkableBenchmarkResource)new StaticResourceImpl("/" + i))
      .cache();

  private static final Observable<EmbeddableBenchmarkResource> EMBEDDED = Observable.range(0, NUM_EMBEDDED_RESOURCES)
      .map(i -> (EmbeddableBenchmarkResource)new Embedded())
      .cache();

  private final Link link;

  public StaticResourceImpl(String path) {
    this.link = new Link(path)
        .setName(StringUtils.trimToNull(path.substring(1)))
        .setTitle("A test resource for microbenchmarking")
        .setType(HalResource.CONTENT_TYPE);
  }

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
    return link;
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
