package io.wcm.caravan.rhyme.microbenchmark;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface Resource extends LinkableResource {

	@ResourceState
	Single<ObjectNode> getState();

	@Related("linked1")
	Observable<Resource> getLinked1();

	@Related("linked2")
	Observable<Resource> getLinked2();

	@Related("linked3")
	Observable<Resource> getLinked3();

	@Related("linked4")
	Observable<Resource> getLinked4();

	@Related("linked2")
	Observable<Resource> getLinked5();
}
