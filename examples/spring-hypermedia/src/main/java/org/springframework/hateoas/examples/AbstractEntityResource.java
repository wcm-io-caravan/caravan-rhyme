package org.springframework.hateoas.examples;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.data.util.Lazy;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

abstract class AbstractEntityResource<T> implements LinkableResource, EmbeddableResource {

	protected final Long id;

	private final Lazy<T> state;

	private final boolean embedded;

	AbstractEntityResource(Long id, Supplier<Optional<T>> supplier) {
		this.id = id;
		this.state = Lazy.of(() -> supplier.get()
				.orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
		this.embedded = false;
	}

	AbstractEntityResource(Long id, T entity) {
		this.id = id;
		this.state = Lazy.of(entity);
		this.embedded = true;
	}

	public T getState() {
		return state.get();
	}

	@Override
	public boolean isEmbedded() {
		return embedded;
	}

	@Override
	public boolean isLinkedWhenEmbedded() {
		return false;
	}

}