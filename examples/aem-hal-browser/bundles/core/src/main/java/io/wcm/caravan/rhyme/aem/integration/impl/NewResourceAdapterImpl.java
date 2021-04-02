package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.NewResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@Model(adaptables = SlingRhyme.class, adapters = NewResourceAdapter.class)
public class NewResourceAdapterImpl implements NewResourceAdapter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

  @Self
  private SlingRhyme slingRhyme;

  @Self
  private Resource currentResource;

  private final Supplier<Stream<Resource>> resourceSupplier;

  private final Predicate<Resource> resourcePredicate;

  public NewResourceAdapterImpl() {
    resourceSupplier = () -> Stream.of(currentResource);
    resourcePredicate = (r) -> true;
  }

  private NewResourceAdapterImpl(SlingRhyme rhyme, Resource resource, Supplier<Stream<Resource>> supplier, Predicate<Resource> predicate) {
    slingRhyme = rhyme;
    currentResource = resource;
    resourceSupplier = supplier;
    resourcePredicate = predicate;
  }

  private NewResourceAdapterImpl withPredicate(Predicate<Resource> predicate) {
    return new NewResourceAdapterImpl(slingRhyme, currentResource, resourceSupplier, predicate);
  }

  private NewResourceAdapterImpl withSupplier(Supplier<Stream<Resource>> supplier) {
    return new NewResourceAdapterImpl(slingRhyme, currentResource, supplier, resourcePredicate);
  }

  @Override
  public <T> T getPropertiesAs(Class<T> clazz) {
    ValueMap valueMap = currentResource.getValueMap();

    return OBJECT_MAPPER.convertValue(valueMap, clazz);
  }

  @Override
  public ResourceSelector select() {

    return new ResourceSelectorImpl();
  }

  @Override
  public ResourceFilter filter() {

    return new ResourceFilterImpl();
  }

  @Override
  public <ModelType> ResourceAdapter<ModelType> adaptTo(Class<ModelType> clazz) {

    return new ResourceAdapterImpl<ModelType>(clazz);
  }

  private final class ResourceAdapterImpl<ModelType> implements ResourceAdapter<ModelType> {

    private final Class<ModelType> clazz;

    private LinkDecorator<ModelType> linkDecorator;

    private ResourceAdapterImpl(Class<ModelType> clazz) {
      this.clazz = clazz;
      this.linkDecorator = null;
    }

    private ResourceAdapterImpl(Class<ModelType> clazz, LinkDecorator<ModelType> decorator) {
      this.clazz = clazz;
      this.linkDecorator = decorator;

      if (AbstractLinkableResource.class.isAssignableFrom(clazz)) {
        throw new HalApiDeveloperException(
            "Your model class must implement " + AbstractLinkableResource.class.getName() + " if you want to decorate your links");
      }
    }

    private ResourceAdapterImpl<ModelType> withLinkDecorator(LinkDecorator<ModelType> decorator) {
      return new ResourceAdapterImpl<ModelType>(clazz, decorator);
    }

    @Override
    public ResourceAdapter<ModelType> withLinkTitle(String title) {

      return withLinkDecorator((r, m) -> title);
    }

    @Override
    public ModelType get() {

      return asOptional().orElseThrow(() -> new HalApiDeveloperException("No elements were found"));
    }

    @Override
    public Optional<ModelType> asOptional() {

      return asStream().findFirst();
    }

    @Override
    public Stream<ModelType> asStream() {

      return resourceSupplier.get()
          .filter(resourcePredicate)
          .map(this::adaptToModelType);
    }

    private ModelType adaptToModelType(Resource res) {

      ModelType model = slingRhyme.adaptResource(res, this.clazz);

      if (linkDecorator != null) {
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

  private final class ResourceFilterImpl implements ResourceFilter {

    @Override
    public NewResourceAdapter onlyMatching(Predicate<Resource> predicate) {

      return withPredicate(predicate);
    }

    @Override
    public NewResourceAdapter onlyIfNameIs(String resourceName) {

      return withPredicate((res) -> res.getName().equals(resourceName));
    }

    @Override
    public NewResourceAdapter onlyIfAdaptableTo(Class<?> adapterClazz) {

      return withPredicate((res) -> res.adaptTo(adapterClazz) != null);
    }
  }

  private final class ResourceSelectorImpl implements ResourceSelector {

    @Override
    public NewResourceAdapter parent() {

      return withSupplier(() -> Stream.of(currentResource.getParent()).filter(Objects::nonNull));
    }

    @Override
    public NewResourceAdapter children() {

      return withSupplier(() -> ResourceUtils.getStreamOfChildren(currentResource));
    }

    @Override
    public NewResourceAdapter child(String name) {

      return withSupplier(() -> ResourceUtils.getStreamOfChildren(currentResource).filter(r -> name.equals(r.getName())));
    }

  }

  private interface LinkDecorator<ModelType> {

    String getLinkTitle(Resource resource, ModelType model);
  }


}
