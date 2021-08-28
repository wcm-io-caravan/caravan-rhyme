package io.wcm.caravan.rhyme.aem.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.impl.docs.RhymeDocsOsgiBundleSupport;
import io.wcm.caravan.rhyme.aem.impl.util.PageUtils;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = SlingHttpServletRequest.class, adapters = { SlingRhyme.class, SlingRhymeImpl.class })
public class SlingRhymeImpl extends SlingAdaptable implements SlingRhyme {

  private final ModelFactory modelFactory;

  private final SlingHttpServletRequest request;
  private final Resource currentResource;
  private final Rhyme rhyme;

  @Inject
  public SlingRhymeImpl(@Self SlingHttpServletRequest request, ModelFactory modelFactory, ResourceLoaderRegistry picker, RhymeDocsOsgiBundleSupport rhymeDocs) {

    this.modelFactory = modelFactory;
    this.request = request;
    this.currentResource = request.getResource();

    this.rhyme = RhymeBuilder.withResourceLoader(picker.getResourceLoader())
        .withRhymeDocsSupport(rhymeDocs)
        .buildForRequestTo(request.getRequestURL().toString());
  }

  private SlingRhymeImpl(SlingRhymeImpl slingRhyme, Resource currentResource) {
    this.modelFactory = slingRhyme.modelFactory;
    this.request = slingRhyme.request;
    this.currentResource = currentResource;
    this.rhyme = slingRhyme.rhyme;
  }

  @Override
  public <T> T adaptResource(Resource resource, Class<T> modelClass) {

    if (resource == null) {
      throw new HalApiDeveloperException("Cannot adapt null resource to " + modelClass.getSimpleName());
    }

    if (resource != currentResource) {
      SlingRhymeImpl slingRhymeWithNextResource = new SlingRhymeImpl(this, resource);
      return slingRhymeWithNextResource.adaptResource(resource, modelClass);
    }

    verifyModelAnnotationIfPresent(modelClass);

    try {
      T slingModel = adaptTo(modelClass);
      if (slingModel == null) {
        throw new HalApiDeveloperException("SlingRhyme#adaptTo(" + modelClass.getName() + ") returned null, see previous log messages for the root cause");
      }

      return slingModel;
    }
    catch (RuntimeException ex) {
      if (ex.getCause() instanceof HalApiServerException) {
        throw (HalApiServerException)ex.getCause();
      }
      throw new HalApiDeveloperException("Failed to adapt " + resource + " to " + modelClass.getSimpleName(), ex);
    }
  }

  private void verifyModelAnnotationIfPresent(Class<?> modelClass) {

    if (modelClass.isAnnotationPresent(Model.class)) {
      if (!isDirectlyAdaptableFromSlingRhyme(modelClass)) {
        throw new HalApiDeveloperException(modelClass + " is not declared to be adaptable from " + SlingRhyme.class);
      }
    }
  }

  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

    try (RequestMetricsStopwatch sw = rhyme.startStopwatch(getClass(), () -> "calls to SlingRhyme#adaptTo(" + type.getSimpleName() + ")")) {

      // immediately return the current resource or request if they are the target of the adaption
      if (Resource.class.isAssignableFrom(type)) {
        return (AdapterType)currentResource;
      }
      if (HttpServletRequest.class.isAssignableFrom(type)) {
        return (AdapterType)request;
      }

      // use the model factory if the target type is a sling model, so that if anything goes wrong,
      // an exception is caught (and thrown) rather than just null being returned
      if (isDirectlyAdaptableFromSlingRhyme(type)) {
        return modelFactory.createModel(this, type);
      }
      if (modelFactory.canCreateFromAdaptable(request, type)) {
        return modelFactory.createModel(request, type);
      }
      if (modelFactory.canCreateFromAdaptable(currentResource, type)) {
        return modelFactory.createModel(currentResource, type);
      }

      // but we also want to adapt to other non-sling model types
      return tryAdapting(type, getRelatedAdaptables())
          .orElseGet(() -> super.adaptTo(type));
    }
  }

  private Collection<Adaptable> getRelatedAdaptables() {

    Builder<Adaptable> builder = ImmutableSet.<Adaptable>builder();
    builder.add(currentResource);
    builder.add(request);

    PageUtils.getOptionalPageResource(currentResource).ifPresent(builder::add);

    return builder.build();
  }

  private <AdapterType> Optional<AdapterType> tryAdapting(Class<AdapterType> type, Collection<Adaptable> adaptables) {

    return adaptables.stream()
        .filter(Objects::nonNull)
        .map(adaptable -> adaptable.adaptTo(type))
        .filter(Objects::nonNull)
        .findFirst();
  }

  private boolean isDirectlyAdaptableFromSlingRhyme(Class<?> adapterType) {

    Model modelAnnotation = adapterType.getAnnotation(Model.class);
    if (modelAnnotation == null) {
      return false;
    }

    return Stream.of(modelAnnotation.adaptables())
        .anyMatch(modelAdaptable -> SlingRhyme.class.isAssignableFrom(modelAdaptable));
  }

  @Override
  public Resource getRequestedResource() {

    return request.getResource();
  }

  @Override
  public Resource getCurrentResource() {

    return currentResource;
  }

  Rhyme getCaravanRhyme() {
    return rhyme;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {
    return rhyme.getRemoteResource(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {
    rhyme.setResponseMaxAge(duration);
  }

  HalResponse renderResource(LinkableResource resourceImpl) {
    return rhyme.renderResponse(resourceImpl).blockingGet();
  }

  HalResponse renderVndErrorResponse(Throwable error) {
    return rhyme.renderVndErrorResponse(error);
  }

}
