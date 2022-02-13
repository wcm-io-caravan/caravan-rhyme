package io.wcm.caravan.rhyme.microbenchmark.resources;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface LinkableBenchmarkResource extends LinkableResource {

  @ResourceState
  Single<BenchmarkResourceState> getState();

  @Related("linked1")
  Observable<LinkableBenchmarkResource> getLinked1();

  @Related("linked2")
  Observable<LinkableBenchmarkResource> getLinked2();

  @Related("linked3")
  Observable<LinkableBenchmarkResource> getLinked3();

  @Related("linked4")
  Observable<LinkableBenchmarkResource> getLinked4();

  @Related("linked2")
  Observable<LinkableBenchmarkResource> getLinked5();

  @Related("embedded")
  Observable<EmbeddableBenchmarkResource> getEmbedded1();
}
