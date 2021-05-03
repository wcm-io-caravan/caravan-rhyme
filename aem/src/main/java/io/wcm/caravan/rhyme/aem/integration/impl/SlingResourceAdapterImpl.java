package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.aem.integration.ResourceStreams;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
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

    return resourceSelector.add(resources, "custom stream of resourcs");
  }

  @Override
  public SlingResourceAdapter selectCurrentResource() {

    return resourceSelector.add(currentResource, Stream::of, "current resource at {}");
  }

  @Override
  public SlingResourceAdapter selectParentResource() {

    return resourceSelector.add(currentResource, ResourceStreams::getParent, "parent of {}");
  }

  @Override
  public SlingResourceAdapter selectChildResources() {

    return resourceSelector.add(currentResource, ResourceStreams::getChildren, "children of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfCurrentPage() {

    return resourceSelector.add(currentResource, ResourceStreams::getContentOfContainingPage, "content of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfParentPage() {

    return resourceSelector.add(currentResource, ResourceStreams::getContentOfParentPage, "content of parent page of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfGrandParentPage() {

    return resourceSelector.add(currentResource, ResourceStreams::getContentOfGrandParentPage, "content of grand parent page of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfChildPages() {

    return resourceSelector.add(currentResource, ResourceStreams::getContentOfChildPages, "content of child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfChildPage(String name) {

    return resourceSelector.add(currentResource, res -> ResourceStreams.getContentOfNamedChildPage(res, name),
        "content of child page named '" + name + "' of {}");
  }

  @Override
  public SlingResourceAdapter selectContentOfGrandChildPages() {

    return resourceSelector.add(currentResource, ResourceStreams::getContentOfGrandChildPages, "content of grand child pages of {}");
  }

  @Override
  public SlingResourceAdapter selectChildResource(String name) {

    return resourceSelector.add(currentResource, res -> ResourceStreams.getNamedChild(res, name), "child named '" + name + "' of {}");
  }

  @Override
  public SlingResourceAdapter selectResourceAt(String path) {

    Resource resource = currentResource.getResourceResolver().getResource(path);
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

      return getOptional()
          .orElseThrow(() -> new HalApiDeveloperException("No resources were found after selecting " + resourceSelector.description));
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

      if (linkDecorator != null) {
        decorateLinks(res, model);
      }
      return model;
    }

    private void decorateLinks(Resource resource, ModelType model) {

      if (!(model instanceof SlingLinkableResource)) {
        throw new HalApiDeveloperException(
            "Your model class " + model.getClass().getSimpleName() + " does not implement " + SlingLinkableResource.class.getName()
                + " (which is required if you want to override link names and titles via SlingResourceAdapter)");
      }

      SlingLinkableResource linkable = (SlingLinkableResource)model;

      linkable.setLinkTitle(linkDecorator.getLinkTitle(resource, model));
    }
  }

  private interface LinkDecorator<ModelType> {

    String getLinkTitle(Resource resource, ModelType model);
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

    private SlingResourceAdapterImpl add(Resource contextResource, Function<Resource, Stream<Resource>> streamFunc, String newDescription) {

      Stream<Resource> stream = streamFunc.apply(contextResource);

      return add(stream, newDescription.replace("{}", contextResource.getPath()));
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
