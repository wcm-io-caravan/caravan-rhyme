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

public class DynamicResourceImpl implements LinkableBenchmarkResource {

  private final String path;

  public DynamicResourceImpl(String path) {
    this.path = path;
  }

  @Override
  public Single<ObjectNode> getState() {
    return Single.just(StatePojo.createTestJson());
  }

  private static Observable<LinkableBenchmarkResource> createLinked() {
    return Observable.range(0, NUM_LINKED_RESOURCES).map(i -> new DynamicResourceImpl("/" + i));
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked1() {
    return createLinked();
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked2() {
    return createLinked();
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked3() {
    return createLinked();
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked4() {
    return createLinked();
  }

  @Override
  public Observable<LinkableBenchmarkResource> getLinked5() {
    return createLinked();
  }

  @Override
  public Observable<EmbeddableBenchmarkResource> getEmbedded1() {
    return Observable.range(0, NUM_EMBEDDED_RESOURCES).map(i -> new Embedded(i));
  }

  @Override
  public Link createLink() {

    return new Link(path)
        .setName(StringUtils.trimToNull(path.substring(1)))
        .setTitle("A test resource for microbenchmarking")
        .setType(HalResource.CONTENT_TYPE);
  }

  static class Embedded implements EmbeddableBenchmarkResource {

    private final int index;

    Embedded(int index) {
      this.index = index;
    }

    @Override
    public Single<ObjectNode> getState() {
      return Single.just(StatePojo.createTestJson());
    }

    @Override
    public Maybe<LinkableBenchmarkResource> getRelated() {
      return Maybe.just(new DynamicResourceImpl("/" + index));
    }
  }
}
