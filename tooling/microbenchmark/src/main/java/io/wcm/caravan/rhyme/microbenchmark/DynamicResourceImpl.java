package io.wcm.caravan.rhyme.microbenchmark;

import static io.wcm.caravan.rhyme.microbenchmark.ResourceParameters.numLinkedResource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;

public class DynamicResourceImpl implements Resource {

	@Override
	public Single<ObjectNode> getState() {
		return Single.just(ResourceState.createTestJson());
	}

	private static Observable<Resource> createLinked() {
		return Observable.range(0, numLinkedResource()).map(i -> new DynamicResourceImpl());
	}

	@Override
	public Observable<Resource> getLinked1() {
		return createLinked();
	}

	@Override
	public Observable<Resource> getLinked2() {
		return createLinked();
	}

	@Override
	public Observable<Resource> getLinked3() {
		return createLinked();
	}

	@Override
	public Observable<Resource> getLinked4() {
		return createLinked();
	}

	@Override
	public Observable<Resource> getLinked5() {
		return createLinked();
	}

	@Override
	public Link createLink() {
		return new Link("/foo");
	}
}
