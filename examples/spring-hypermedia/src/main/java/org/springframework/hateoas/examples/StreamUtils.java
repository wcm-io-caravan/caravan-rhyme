package org.springframework.hateoas.examples;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StreamUtils {

	public static <T, U> List<U> transform(Iterable<T> entities, Function<T, U> resourceConstructor) {

		return StreamSupport.stream(entities.spliterator(), false).map(resourceConstructor)
				.collect(Collectors.toList());
	}
}
