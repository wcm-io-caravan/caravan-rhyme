package io.wcm.caravan.rhyme.microbenchmark;

import static io.wcm.caravan.rhyme.microbenchmark.ResourceParameters.numEmbeddedResource;

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
    return Single.just(TestState.createMappedJson());
  }

  @Override
  public Observable<EmbeddableBenchmarkResource> getEmbedded1() {
    return Observable.range(0, numEmbeddedResource()).map(i -> new Embedded(i));
  }

  static class Embedded implements EmbeddableBenchmarkResource {

    private final int index;

    Embedded(int index) {
      this.index = index;
    }

    @Override
    public Single<ObjectNode> getState() {
      return Single.just(TestState.createMappedJson());
    }

    @Override
    public Maybe<LinkableBenchmarkResource> getRelated() {
      return Maybe.just(new MappingResourceImpl("/" + index));
    }
  }
}
