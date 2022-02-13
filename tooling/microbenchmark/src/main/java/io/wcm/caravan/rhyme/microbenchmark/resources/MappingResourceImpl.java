package io.wcm.caravan.rhyme.microbenchmark.resources;

import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_EMBEDDED_RESOURCES;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class MappingResourceImpl extends DynamicResourceImpl {

  public MappingResourceImpl(String path) {
    super(path);
  }

  @Override
  public Single<ObjectNode> getState() {
    return Single.just(StatePojo.createMappedJson());
  }

  @Override
  public Observable<EmbeddableBenchmarkResource> getEmbedded1() {
    return Observable.range(0, NUM_EMBEDDED_RESOURCES).map(i -> new Embedded(i));
  }

  static class Embedded implements EmbeddableBenchmarkResource {

    private final int index;

    Embedded(int index) {
      this.index = index;
    }

    @Override
    public Single<ObjectNode> getState() {
      return Single.just(StatePojo.createMappedJson());
    }

    @Override
    public Maybe<LinkableBenchmarkResource> getRelated() {
      return Maybe.just(new MappingResourceImpl("/" + index));
    }
  }
}
