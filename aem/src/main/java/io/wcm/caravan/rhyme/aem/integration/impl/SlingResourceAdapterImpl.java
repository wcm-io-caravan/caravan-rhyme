package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@Model(adaptables = SlingRhyme.class, adapters = SlingResourceAdapter.class)
public class SlingResourceAdapterImpl implements SlingResourceAdapter {

  @Self
  private SlingRhyme slingRhyme;

  @Self
  private Resource currentResource;

  private final ResourceSelector resourceSelector;

  private final ResourceFilter resourceFilter;

  public SlingResourceAdapterImpl() {
    resourceSelector = new ResourceSelector(null, null);
    resourceFilter = new ResourceFilter(null, null);
  }

  private SlingResourceAdapterImpl(SlingRhyme rhyme, Resource resource, ResourceSelector selector, ResourceFilter filter) {
    slingRhyme = rhyme;
    currentResource = resource;
    resourceSelector = new ResourceSelector(selector.description, selector.resources);
    resourceFilter = new ResourceFilter(filter.description, filter.predicate);
  }

  @Override
  public SlingResourceAdapter select(Stream<Resource> resources) {

    return resourceSelector.add(resources,
        "custom stream of resourcs");
  }

  @Override
  public SlingResourceAdapter selectCurrentResource() {

    return resourceSelector.add(Stream.of(currentResource),
        "current resource at " + currentResource.getPath());
  }

  @Override
  public SlingResourceAdapter selectParentResource() {

    return resourceSelector.add(Stream.of(currentResource.getParent()).filter(Objects::nonNull),
        "parent of " + currentResource.getPath());
  }

  @Override
  public SlingResourceAdapter selectChildResources() {

    return resourceSelector.add(ResourceUtils.getStreamOfChildren(currentResource),
        "children of " + currentResource.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfCurrentPage() {

    Resource page = getPageResource();

    return resourceSelector.add(getContentResources(Stream.of(page)), "content " + page.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfParentPage() {

    Resource page = getPageResource();

    Stream<Resource> parentPage = Stream.of(page.getParent());

    return resourceSelector.add(getContentResources(parentPage), "content of parent page of " + page.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfGrandParentPage() {

    Resource page = getPageResource();

    Stream<Resource> grandParentPage = Stream.of(page.getParent()).filter(Objects::nonNull).map(Resource::getParent);

    return resourceSelector.add(getContentResources(grandParentPage), "content of grand parent page of " + page.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfChildPages() {

    Resource page = getPageResource();

    Stream<Resource> childPages = ResourceUtils.getStreamOfChildPages(page);

    return resourceSelector.add(getContentResources(childPages), "content of child pages of " + page.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfChildPage(String name) {

    Resource page = getPageResource();

    Stream<Resource> childPage = ResourceUtils.getStreamOfChildPages(page)
        .filter(child -> name.equals(child.getName()));

    return resourceSelector.add(getContentResources(childPage), "content of child page named '" + name + "' of " + page.getPath());
  }

  @Override
  public SlingResourceAdapter selectContentOfGrandChildPages() {

    Resource page = getPageResource();

    Stream<Resource> grandChildPages = ResourceUtils.getStreamOfChildPages(page)
        .flatMap(ResourceUtils::getStreamOfChildPages);

    return resourceSelector.add(getContentResources(grandChildPages), "content of grand child pages of " + page.getPath());
  }

  private Stream<Resource> getContentResources(Stream<Resource> pageResources) {

    return pageResources
        .filter(Objects::nonNull)
        .map(child -> child.getChild(JcrConstants.JCR_CONTENT))
        .filter(Objects::nonNull);
  }


  @Override
  public SlingResourceAdapter selectChildResource(String name) {

    return resourceSelector.add(ResourceUtils.getStreamOfChildren(currentResource).filter(r -> name.equals(r.getName())),
        "child named '" + name + "' of " + currentResource.getPath());
  }

  @Override
  public SlingResourceAdapter selectLinkedResources() {

    return resourceSelector.add(findLinkedResourcesIn(currentResource),
        "resources that are linked from " + currentResource.getPath());
  }

  private Stream<Resource> findLinkedResourcesIn(Resource contentResource) {

    ValueMap properties = contentResource.getValueMap();

    ResourceResolver resolver = currentResource.getResourceResolver();

    Stream<Resource> linkedInThisResource = properties.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof String)
        .map(entry -> (String)entry.getValue())
        .filter(value -> value.startsWith("/"))
        .map(contentRef -> resolver.getResource(contentRef))
        .filter(Objects::nonNull);

    Stream<Resource> linkedInChildResources = StreamSupport.stream(contentResource.getChildren().spliterator(), false)
        .flatMap(this::findLinkedResourcesIn);

    return Stream.concat(linkedInThisResource, linkedInChildResources);
  }


  @Override
  public SlingResourceAdapter selectResourceAt(String path) {

    Resource resource = currentResource.getResourceResolver().getResource(path);
    if (resource == null) {
      return resourceSelector.add(Stream.empty(), "non-existant resource at " + path);
    }

    return resourceSelector.add(Stream.of(resource),
        "different resource at " + resource.getPath());
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
  public SlingResourceAdapter filterWithName(String resourceName) {

    return resourceFilter.add((res) -> res.getName().equals(resourceName),
        "if resource name is " + resourceName);
  }

  @Override
  public <ModelType> TypedResourceAdapter<ModelType> adaptTo(Class<ModelType> clazz) {

    return new TypedResourceAdapterImpl<ModelType>(clazz);
  }

  private final class TypedResourceAdapterImpl<ModelType> implements TypedResourceAdapter<ModelType> {

    private final Class<ModelType> clazz;

    private final LinkDecorator<ModelType> linkDecorator;

    private TypedResourceAdapterImpl(Class<ModelType> clazz) {
      this.clazz = clazz;
      this.linkDecorator = null;
    }

    private TypedResourceAdapterImpl(Class<ModelType> clazz, LinkDecorator<ModelType> decorator) {
      this.clazz = clazz;
      this.linkDecorator = decorator;

      if (AbstractLinkableResource.class.isAssignableFrom(clazz)) {
        throw new HalApiDeveloperException(
            "Your model class must implement " + AbstractLinkableResource.class.getName() + " if you want to decorate your links");
      }
    }

    private TypedResourceAdapterImpl<ModelType> withLinkDecorator(LinkDecorator<ModelType> decorator) {
      return new TypedResourceAdapterImpl<ModelType>(clazz, decorator);
    }

    @Override
    public TypedResourceAdapter<ModelType> withLinkTitle(String title) {

      return withLinkDecorator((r, m) -> title);
    }

    @Override
    public ModelType getInstance() {

      return getOptional().orElseThrow(() -> new HalApiDeveloperException("No elements were found"));
    }

    @Override
    public Optional<ModelType> getOptional() {

      return getStream().findFirst();
    }

    @Override
    public Stream<ModelType> getStream() {

      Stream<Resource> resources = resourceSelector.resources;

      if (resources == null) {
        throw new HalApiDeveloperException("No resources have been selected with this adapter");
      }

      if (resourceFilter.predicate != null) {
        resources = resources.filter(resourceFilter.predicate);
      }

      return resources.map(this::adaptToModelTypeAnDecorateLinks);
    }

    private ModelType adaptToModelTypeAnDecorateLinks(Resource res) {

      ModelType model = slingRhyme.adaptResource(res, this.clazz);

      if (linkDecorator != null && model != null) {
        decorateLinks(res, model);
      }
      return model;
    }

    private void decorateLinks(Resource resource, ModelType model) {

      if (!(model instanceof AbstractLinkableResource)) {
        throw new HalApiDeveloperException(
            "model class " + model.getClass().getSimpleName() + " does not implement " + AbstractLinkableResource.class.getName());
      }

      AbstractLinkableResource linkable = (AbstractLinkableResource)model;

      linkable.setLinkTitle(linkDecorator.getLinkTitle(resource, model));
    }
  }

  private interface LinkDecorator<ModelType> {

    String getLinkTitle(Resource resource, ModelType model);
  }

  private SlingResourceAdapterImpl fromCurrentPage() {
    return new SlingResourceAdapterImpl(slingRhyme, getPageResource(), resourceSelector, resourceFilter);
  }

  private Resource getPageResource() {
    Resource candidate = currentResource;
    while (candidate != null && !ResourceUtils.isPage(candidate)) {
      candidate = candidate.getParent();
    }
    if (candidate == null) {
      throw new HalApiDeveloperException("Failed to find a parent cq:Page node from " + currentResource.getPath());
    }
    return candidate;
  }

  private final class ResourceFilter {

    private final String description;
    private final Predicate<Resource> predicate;

    private ResourceFilter(String description, Predicate<Resource> predicate) {
      this.description = description;
      this.predicate = predicate;
    }

    private SlingResourceAdapterImpl add(Predicate<Resource> newPredicate, String newDescription) {

      ResourceFilter newFilter;
      if (predicate != null) {

        String mergedDescription = this.description + " and " + newDescription;
        Predicate<Resource> mergedPredicate = predicate.and(newPredicate);

        newFilter = new ResourceFilter(mergedDescription, mergedPredicate);
      }
      else {
        newFilter = new ResourceFilter(newDescription, newPredicate);
      }

      SlingResourceAdapterImpl newInstance = new SlingResourceAdapterImpl(slingRhyme, currentResource, resourceSelector, newFilter);

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

    private SlingResourceAdapterImpl add(@NotNull Stream<Resource> newResources, String newDescription) {

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


      SlingResourceAdapterImpl newInstance = new SlingResourceAdapterImpl(slingRhyme, currentResource, newSelector, resourceFilter);

      return newInstance;
    }
  }


}
