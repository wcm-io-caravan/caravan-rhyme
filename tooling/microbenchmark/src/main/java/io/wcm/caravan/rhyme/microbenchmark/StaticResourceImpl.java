package io.wcm.caravan.rhyme.microbenchmark;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;

public class StaticResourceImpl implements Resource {

	private static final Single<ObjectNode> STATE_SINGLE = Single.just(TestState.createTestJson());

	private static final Link LINK = new Link("/foo");

	private static final Observable<Resource> LINKED = Observable.range(0, ResourceParameters.numLinkedResource())
			.map(i -> new StaticResourceImpl());

	@Override
	public Single<ObjectNode> getState() {
		return STATE_SINGLE;
	}

	@Override
	public Observable<Resource> getLinked1() {
		return LINKED;
	}

	@Override
	public Observable<Resource> getLinked2() {
		return LINKED;
	}

	@Override
	public Observable<Resource> getLinked3() {
		return LINKED;
	}

	@Override
	public Observable<Resource> getLinked4() {
		return LINKED;
	}

	@Override
	public Observable<Resource> getLinked5() {
		return LINKED;
	}

	@Override
	public Link createLink() {
		return LINK;
	}

}
