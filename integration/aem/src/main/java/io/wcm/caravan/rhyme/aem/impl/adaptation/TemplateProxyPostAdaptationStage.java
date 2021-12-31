/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.aem.impl.adaptation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.adaptation.PostAdaptationStage;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

final class TemplateProxyPostAdaptationStage<I, M extends I> implements PostAdaptationStage<I, M> {

  private static final String PATH_PLACEHOLDER = "/letsassumethisisunlikelytoexist";

  private final SlingResourceAdapterImpl adapterImpl;
  private final Class<I> halApiInterface;
  private final RhymeResourceRegistry registry;

  private String linkTitle;
  private String linkName;
  private String[] queryParameters;

  TemplateProxyPostAdaptationStage(SlingResourceAdapterImpl adapterImpl, Class<I> halApiInterface, RhymeResourceRegistry registry) {
    this.adapterImpl = adapterImpl;
    this.halApiInterface = halApiInterface;
    this.registry = registry;
  }

  @Override
  public PostAdaptationStage<I, M> withLinkTitle(String title) {
    this.linkTitle = title;
    return this;
  }

  @Override
  public PostAdaptationStage<I, M> withLinkName(String name) {
    this.linkName = name;
    return this;
  }

  @Override
  public PostAdaptationStage<I, M> withQueryParameterTemplate(String... names) {
    this.queryParameters = names;
    return this;
  }

  @Override
  public PostAdaptationStage<I, M> withPartialLinkTemplate() {
    throw new HalApiDeveloperException("#withPartialLinkTemplate cannot be called if you selected a null resource path to build a template");
  }

  @Override
  public SlingModelPostAdaptationStage<I, M> withModifications(Consumer<M> decorator) {
    throw new HalApiDeveloperException("#withModifications cannot be called if you selected a null resource path to build a template");
  }

  @Override
  public M getInstance() {
    return createResourceProxy();
  }

  @Override
  public Optional<I> getOptional() {
    return Optional.of(createResourceProxy());
  }

  @Override
  public Stream<I> getStream() {
    return Stream.of(createResourceProxy());
  }

  private <T extends LinkableResource> T createResourceProxy() {

    return (T)Proxy.newProxyInstance(halApiInterface.getClassLoader(), new Class[] { halApiInterface }, new InvocationHandler() {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getName().equals("createLink")) {
          return createLink();
        }

        throw new HalApiDeveloperException("Unsupported call to " + method.getName() + " method on "
            + halApiInterface.getName() + " proxy instance. "
            + "Any instances created with SlingLinkBuilder#selectResourceAt(null) can only be used to create link templates for these resources");
      }
    });
  }

  private Link createLink() {

    String baseTemplate = getResourceUrl().replace(PATH_PLACEHOLDER, "{+path}");

    UriTemplateBuilder builder = UriTemplate.buildFromTemplate(baseTemplate);

    if (queryParameters != null) {
      builder.query(queryParameters);
    }
    String uriTemplate = builder.build()
        .getTemplate();

    return new Link(uriTemplate)
        .setTitle(linkTitle)
        .setName(linkName);
  }

  private String getResourceUrl() {

    String selector = registry.getSelectorForHalApiInterface(halApiInterface).orElse(null);

    UrlHandler urlHandler = adapterImpl.getSlingRhyme().adaptTo(UrlHandler.class);

    return urlHandler.get(PATH_PLACEHOLDER)
        .selectors(selector)
        .extension(HalApiServlet.EXTENSION)
        .buildExternalLinkUrl();
  }
}
