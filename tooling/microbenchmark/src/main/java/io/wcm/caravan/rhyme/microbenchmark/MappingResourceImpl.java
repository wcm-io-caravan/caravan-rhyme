package io.wcm.caravan.rhyme.microbenchmark;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Single;

public class MappingResourceImpl extends DynamicResourceImpl {

	@Override
	public Single<ObjectNode> getState() {
		return Single.just(ResourceState.createMappedJson());
	}

}
