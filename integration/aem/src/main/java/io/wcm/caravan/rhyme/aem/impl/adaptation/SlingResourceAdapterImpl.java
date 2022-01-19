package io.wcm.caravan.rhyme.aem.impl.adaptation;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.PostAdaptationStage;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.aem.impl.util.PageUtils;
import io.wcm.caravan.rhyme.aem.impl.util.ResourceStreams;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@Model(adaptables = SlingRhyme.class, adapters = SlingResourceAdapter.class)
public class SlingResourceAdapterImpl implements SlingResourceAdapter {

  private final SlingRhyme slingRhyme;
  private final RhymeResourceRegistry registry;

  private final Resource fromResource;
  private final boolean nullResourcePathGiven;

  private final ResourceSelector resourceSelector;
  private final ResourceFilter resourceFilter;

  @Inject
  public SlingResourceAdapterImpl(@Self SlingRhyme slingRhyme, RhymeResourceRegistry registry) {
    this.slingRhyme = slingRhyme;
    this.registry = registry;
    this.fromResource = null;
    this.nullResourcePathGiven = false;
    this.resourceSelector = new ResourceSelector(null, null);
    this.resourceFilter = new ResourceFilter(null, null);
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl adapter, ResourceSelector selector, ResourceFilter filter) {
    this.slingRhyme = adapter.slingRhyme;
    this.registry = adapter.registry;
    this.fromResource = adapter.fromResource;
    this.nullResourcePathGiven = adapter.nullResourcePathGiven;
    this.resourceSelector = new ResourceSelector(selector.description, selector.resources);
    this.resourceFilter = new ResourceFilter(filter.description, filter.predicate);
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl adapter, Resource newFromResource) {
    this.slingRhyme = adapter.slingRhyme;
    this.registry = adapter.registry;
    this.fromResource = newFromResource;
    this.nullResourcePathGiven = newFromResource == null;
    this.resourceSelector = new ResourceSelector(null, null);
    this.resourceFilter = new ResourceFilter(null, null);
  }

  private SlingResourceAdapter fromResource(Resource resource) {

    if (fromResource != null) {
      throw new HalApiDeveloperException(
          "You cannot call the from* methods multiple times, as a single context resource is required for the following selection steps");
    }

    if (resourceSelector.resources != null) {
      throw new HalApiDeveloperException("The SlingResourceAdapterImpl#fromXyz methods must be called *before* any of the selectXyz methods are called");
    }

    return new SlingResourceAdapterImpl(this, resource);
  }

  @Override
  public SlingResourceAdapter fromResourceAt(String path) {

    Resource resource = slingRhyme.getRequestedResource().getResourceResolver().getResource(path);

    if (resource == null) {
      throw new HalApiDeveloperException("There does not exist a resource at " + path);
    }

    return fromResource(resource);
  }

  @Override
  public SlingResourceAdapter fromCurrentPage() {

    return fromResource(PageUtils.getPageResource(slingRhyme.getCurrentResource()));
  }

  @Override
  public SlingResourceAdapter fromParentPage() {

    return fromResource(PageUtils.getParentPageResource(slingRhyme.getCurrentResource()));
  }

  @Override
  public SlingResourceAdapter fromGrandParentPage() {

    return fromResource(PageUtils.getGrandParentPageResource(slingRhyme.getCurrentResource()));
  }

  @Override
  public SlingResourceAdapter select(Stream<Resource> resources) {

    return resourceSelector.add(resources, "custom stream of resourcs");
  }

  @Override
  public SlingResourceAdapter selectCurrentResource() {

    return resourceSelector.add(Stream::of, "current resource at {}");
  }

  @Override
  public SlingResourceAdapter selectContentResource() {

    return resourceSelector.add(ResourceStreams::getContentResource, "content resource of {}");
  }

  @Override
  public SlingResourceAdapter selectParentResource() {

    return resourceSelector.add(ResourceStreams::getParent, "parent of {}");
  }

  @Override
  public SlingResourceAdapter selectChildResources() {

    return resourceSelector.add(ResourceStreams::getChildren, "children of {}");
  }

  @Override
  public SlingResourceAdapter selectSiblingResource(String name) {

    return resourceSelector.add(res -> ResourceStreams.getNamedSibling(res, name), "sibling of {} with name " + name);
  }

  @Override
  public SlingResourceAdapter selectGrandChildResources() {

    return resourceSelector.add(ResourceStreams::getGrandChildren, "grand children of {}");
  }

  @Override
  public SlingResourceAdapter selectContainingPage() {

    return resourceSelector.add((res) -> Stream.of(PageUtils.getPageResource(res)), "page containing {}");
  }

  @Override
  public SlingResourceAdapter selectChildPages() {

    return resourceSelector.add(ResourceStreams::getChildPages, "child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectGrandChildPages() {

    return resourceSelector.add(ResourceStreams::getGrandChildPages, "grand child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfCurrentPage() {

    return resourceSelector.add(ResourceStreams::getContentOfContainingPage, "content of page containing {}");
  }


  @Override
  public SlingResourceAdapter selectContentOfChildPages() {

    return resourceSelector.add(ResourceStreams::getContentOfChildPages, "content of child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfChildPage(String name) {

    return resourceSelector.add(res -> ResourceStreams.getContentOfNamedChildPage(res, name), "content of child page named '" + name + "' of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfGrandChildPages() {

    return resourceSelector.add(ResourceStreams::getContentOfGrandChildPages, "content of grand child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectChildResource(String name) {

    return resourceSelector.add(res -> ResourceStreams.getNamedChild(res, name), "child named '" + name + "' of {}");
  }

  @Override
  public SlingResourceAdapter selectResourceAt(String path) {

    if (path == null) {
      return new SlingResourceAdapterImpl(this, null);
    }

    Resource resource = slingRhyme.getRequestedResource().getResourceResolver().getResource(path);
    if (resource == null) {
      return resourceSelector.add(Stream.empty(), "non-existent resource at " + path);
    }

    return resourceSelector.add(Stream.of(resource), "different resource at " + resource.getPath());
  }


  @Override
  public SlingResourceAdapter filter(Predicate<Resource> predicate) {

    return resourceFilter.add(predicate, "custom predicate");
  }

  @Override
  public SlingResourceAdapter filterAdaptableTo(Class<?> adapterClazz) {

    return resourceFilter.add((res) -> res.adaptTo(adapterClazz) != null,
        "if resource can be adapted to " + adapterClazz.getSimpleName());
  }

  @Override
  public <T> SlingResourceAdapter filterAdaptableTo(Class<T> adapterClazz, Predicate<T> predicate) {

    Predicate<Resource> isAdaptableAndAcceptedByPredicated = (res) -> {
      T adapted = res.adaptTo(adapterClazz);
      if (adapted == null) {
        return false;
      }
      return predicate.test(adapted);
    };

    return resourceFilter.add(isAdaptableAndAcceptedByPredicated,
        "if resource can be adapted to " + adapterClazz.getSimpleName() + " and is accepted by predicate " + predicate);
  }

  @Override
  public SlingResourceAdapter filterWithName(String resourceName) {

    return resourceFilter.add((res) -> res.getName().equals(resourceName),
        "if resource name is " + resourceName);
  }

  @Override
  public <I> PostAdaptationStage<I, I> adaptTo(Class<I> clazz) {

    if (nullResourcePathGiven) {
      return new TemplateProxyPostAdaptationStage<>(this, clazz, registry);
    }

    return new SlingModelPostAdaptationStage<>(this, clazz, clazz);
  }

  @Override
  public <I, M extends I> PostAdaptationStage<I, M> adaptTo(Class<I> halApiInterface, Class<M> slingModelClass) {

    if (nullResourcePathGiven) {
      throw new HalApiDeveloperException("You cannot specify a model class if pure template generation was forced by calling #selectResourceAt");
    }

    return new SlingModelPostAdaptationStage<>(this, halApiInterface, slingModelClass);
  }


  Stream<Resource> getSelectedResources() {
    return resourceSelector.resources;
  }

  String getSelectedResourcesDescription() {
    return resourceSelector.description;
  }

  Predicate<Resource> getResourceFilterPredicate() {
    return resourceFilter.predicate;
  }

  SlingRhyme getSlingRhyme() {
    return slingRhyme;
  }

  private final class ResourceFilter {

    private final String description;
    private final Predicate<Resource> predicate;

    private ResourceFilter(String description, Predicate<Resource> predicate) {
      this.description = description;
      this.predicate = predicate;
    }

    private SlingResourceAdapter add(Predicate<Resource> newPredicate, String newDescription) {

      ResourceFilter newFilter;
      if (predicate != null) {

        String mergedDescription = this.description + " and " + newDescription;
        Predicate<Resource> mergedPredicate = predicate.and(newPredicate);

        newFilter = new ResourceFilter(mergedDescription, mergedPredicate);
      }
      else {
        newFilter = new ResourceFilter(newDescription, newPredicate);
      }

      SlingResourceAdapter newInstance = new SlingResourceAdapterImpl(SlingResourceAdapterImpl.this, resourceSelector, newFilter);

      return newInstance;
    }

  }

  private final class ResourceSelector {

    private final String description;
    private final Stream<Resource> resources;

    private ResourceSelector(String description, Stream<Resource> resources) {
      this.description = description;
      this.resources = resources;
    }

    private SlingResourceAdapter add(Function<Resource, Stream<Resource>> streamFunc, String newDescription) {

      Resource contextResource = fromResource;
      if (contextResource == null) {
        contextResource = slingRhyme.getCurrentResource();
      }

      Stream<Resource> stream = streamFunc.apply(contextResource);

      return add(stream, newDescription.replace("{}", contextResource.getPath()));
    }

    private SlingResourceAdapter add(@NotNull Stream<Resource> newResources, String newDescription) {

      Preconditions.checkNotNull(newResources, "the stream of resources must not be null");

      ResourceSelector newSelector;
      if (resources != null) {

        String mergedDescription = this.description + " and " + newDescription;
        Stream<Resource> mergedStream = Stream.concat(this.resources, newResources);

        newSelector = new ResourceSelector(mergedDescription, mergedStream);
      }
      else {
        newSelector = new ResourceSelector(newDescription, newResources);
      }


      SlingResourceAdapter newInstance = new SlingResourceAdapterImpl(SlingResourceAdapterImpl.this, newSelector, resourceFilter);

      return newInstance;
    }
  }

}
