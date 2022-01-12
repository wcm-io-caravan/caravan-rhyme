/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.wcm.caravan.rhyme.microbenchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 2, warmups = 0)
@Warmup(time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(time = 2, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Benchmarks {

	private static HalResourceLoader preBuiltLoader = ResourceLoaders.preBuilt();
	private static HalResourceLoader parsingLoader = ResourceLoaders.parsing();
	private static HalResourceLoader networkLoader = ResourceLoaders.network();

	static Rhyme rhyme() {
		return RhymeBuilder.create().buildForRequestTo("/foo");
	}

	static Rhyme rhyme(HalResourceLoader halResourceLoader) {
		return RhymeBuilder.withResourceLoader(halResourceLoader).buildForRequestTo("/foo");
	}

	private static Rhyme rhymeWithMetrics() {
		return RhymeBuilder.create().withMetadataConfiguration(new RhymeMetadataConfiguration() {

			@Override
			public boolean isMetadataGenerationEnabled() {
				return true;
			}

		}).buildForRequestTo("/foo");
	}

	private static Rhyme rhymeWithMetrics(HalResourceLoader halResourceLoader) {
		return RhymeBuilder.withResourceLoader(halResourceLoader)
				.withMetadataConfiguration(new RhymeMetadataConfiguration() {

					@Override
					public boolean isMetadataGenerationEnabled() {
						return true;
					}

				}).buildForRequestTo("/foo");
	}

	// @Benchmark
	public Rhyme createRhyme() {

		return rhyme();
	}

	@Setup(Level.Iteration)
	public void init() {
		ResourceLoaders.init();
	}

	@TearDown(Level.Iteration)
	public void tearDown() {
		ResourceLoaders.tearDown();
	}

	@Benchmark
	public Rhyme createRhymeWithMetrics() {

		return rhymeWithMetrics();
	}

	@Benchmark
	public HalResponse renderStatic() {

		return rhyme().renderResponse(new StaticResourceImpl()).blockingGet();
	}

	@Benchmark
	public HalResponse renderStaticWithMetrics() {

		return rhymeWithMetrics().renderResponse(new StaticResourceImpl()).blockingGet();
	}

	@Benchmark
	public HalResponse renderDynamic() {

		return rhyme().renderResponse(new DynamicResourceImpl()).blockingGet();
	}

	@Benchmark
	public HalResponse renderDynamicWithMetrics() {

		return rhymeWithMetrics().renderResponse(new DynamicResourceImpl()).blockingGet();
	}

	@Benchmark
	public HalResponse renderMapping() {

		return rhyme().renderResponse(new MappingResourceImpl()).blockingGet();
	}

	@Benchmark
	public HalResponse renderMappingWithMetrics() {

		return rhymeWithMetrics().renderResponse(new MappingResourceImpl()).blockingGet();
	}

	private ImmutableList<Object> callClientMethods(Rhyme rhyme) {

		Resource clientProxy = rhyme.getRemoteResource("/foo", Resource.class);

		return ImmutableList.of(clientProxy.getState().blockingGet(), clientProxy.createLink(),
				clientProxy.getLinked1().map(l -> l.getState()).toList().blockingGet(),
				clientProxy.getLinked1().map(l -> l.createLink().getHref()).toList().blockingGet(),
				clientProxy.getLinked2().map(l -> l.createLink().getHref()).toList().blockingGet(),
				clientProxy.getLinked3().map(l -> l.createLink().getHref()).toList().blockingGet(),
				clientProxy.getLinked4().map(l -> l.createLink().getHref()).toList().blockingGet(),
				clientProxy.getLinked5().map(l -> l.createLink().getHref()).toList().blockingGet());
	}

	@Benchmark
	public Object useClientProxy() {
		return callClientMethods(rhyme(preBuiltLoader));
	}

	@Benchmark
	public Object useClientProxyWithMetrics() {
		return callClientMethods(rhymeWithMetrics(preBuiltLoader));
	}

	@Benchmark
	public Object useParsingClientProxy() {
		return callClientMethods(rhyme(parsingLoader));
	}

	@Benchmark
	public Object useParsingClientProxyWithMetrics() {
		return callClientMethods(rhymeWithMetrics(parsingLoader));
	}

	@Benchmark
	public Object useNetworkClientProxy() {
		return callClientMethods(rhyme(networkLoader));
	}

	@Benchmark
	public Object useNetworkClientProxyWithMetrics() {
		return callClientMethods(rhymeWithMetrics(networkLoader));
	}
}
