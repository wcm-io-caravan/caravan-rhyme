package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.api.util.ResourceStreams;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingResourceAdapter.class)
public class SlingResourceAdapterImpl implements SlingResourceAdapter {

  private static final Logger log = getLogger(SlingResourceAdapterImpl.class);

  @Self
  private SlingRhyme slingRhyme;

  @Self
  private UrlHandler urlHandler;

  @Inject
  private RhymeResourceRegistry registry;

  private final Resource fromResource;

  private final boolean templateGenerationRequired;

  private final ResourceSelector resourceSelector;

  private final ResourceFilter resourceFilter;


  public SlingResourceAdapterImpl() {
    fromResource = null;
    templateGenerationRequired = false;
    resourceSelector = new ResourceSelector(null, null);
    resourceFilter = new ResourceFilter(null, null);
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl adapter, ResourceSelector selector, ResourceFilter filter) {
    slingRhyme = adapter.slingRhyme;
    urlHandler = adapter.urlHandler;
    registry = adapter.registry;
    fromResource = adapter.fromResource;
    templateGenerationRequired = adapter.templateGenerationRequired;
    resourceSelector = new ResourceSelector(selector.description, selector.resources);
    resourceFilter = new ResourceFilter(filter.description, filter.predicate);
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl adapter, Resource resource) {
    slingRhyme = adapter.slingRhyme;
    urlHandler = adapter.urlHandler;
    registry = adapter.registry;
    fromResource = resource;
    templateGenerationRequired = resource == null;
    resourceSelector = new ResourceSelector(null, null);
    resourceFilter = new ResourceFilter(null, null);
  }

  private SlingResourceAdapter fromResource(Resource resource) {

    if (resourceSelector.resources != null) {
      throw new HalApiDeveloperException("The SlingResourceAdapterImpl#fromXyz methods must be called *before* any of the selectXyz methods are called");
    }

    return new SlingResourceAdapterImpl(this, resource);
  }

  @Override
  public SlingResourceAdapter fromResourceAt(String path) {

    log.info("fromResourceAt({}) was called with currentResource={}", path, slingRhyme.getCurrentResource());

    Resource resource = slingRhyme.getRequestedResource().getResourceResolver().getResource(path);

    if (resource == null) {
      throw new HalApiDeveloperException("There does not exist a resource at " + path);
    }

    return fromResource(resource);
  }

  @Override
  public SlingResourceAdapter fromCurrentPage() {

    return fromResource(ResourceStreams.getPageResource(slingRhyme.getCurrentResource()));
  }

  @Override
  public SlingResourceAdapter fromParentPage() {

    return fromResource(ResourceStreams.getParentPageResource(slingRhyme.getCurrentResource()));
  }

  @Override
  public SlingResourceAdapter fromGrandParentPage() {

    return fromResource(ResourceStreams.getGrandParentPageResource(slingRhyme.getCurrentResource()));
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
  public SlingResourceAdapter selectContentOfCurrentPage() {

    return resourceSelector.add(ResourceStreams::getContentOfContainingPage, "content of {}");
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
  public <I> TypedResourceAdapter<I, I> adaptTo(Class<I> clazz) {

    if (templateGenerationRequired) {
      return new TemplateResourceAdapter<I, I>(clazz);
    }

    return new TypedResourceAdapterImpl<I, I>(clazz, clazz);
  }

  @Override
  public <I, M extends I> TypedResourceAdapter<I, M> adaptTo(Class<I> halApiInterface, Class<M> slingModelClass) {

    if (templateGenerationRequired) {
      throw new HalApiDeveloperException("You cannot specify a model class if pure template generation was forced by calling #selectResourceAt");
    }

    return new TypedResourceAdapterImpl<I, M>(halApiInterface, slingModelClass);
  }


  private final class TypedResourceAdapterImpl<I, M extends I> implements TypedResourceAdapter<I, M> {

    private final Class<I> interfaze;
    private final Class<M> clazz;
    private final List<Consumer<M>> instanceDecorators;

    private TypedResourceAdapterImpl(Class<I> interfaze, Class<M> clazz) {
      this.interfaze = interfaze;
      this.clazz = clazz;
      this.instanceDecorators = new ArrayList<>();
    }

    private TypedResourceAdapterImpl(Class<I> interfaze, Class<M> clazz, List<Consumer<M>> instanceDecorators) {
      this.interfaze = interfaze;
      this.clazz = clazz;
      this.instanceDecorators = instanceDecorators;
    }

    private TypedResourceAdapterImpl<I, M> withInstanceDecorator(Consumer<M> decorator) {

      List<Consumer<M>> list = new ArrayList<>(instanceDecorators);
      list.add(decorator);

      return new TypedResourceAdapterImpl<>(interfaze, clazz, list);
    }

    private TypedResourceAdapterImpl<I, M> withLinkDecorator(Consumer<SlingLinkableResource> decorator) {

      return withInstanceDecorator(instance -> {
        if (!(instance instanceof SlingLinkableResource)) {
          throw new HalApiDeveloperException(
              "Your model class " + instance.getClass().getSimpleName() + " does not implement " + SlingLinkableResource.class.getName()
                  + " (which is required if you want to override link names and titles via SlingResourceAdapter)");
        }

        decorator.accept((SlingLinkableResource)instance);

      });
    }

    @Override
    public TypedResourceAdapter<I, M> withModifications(Consumer<M> consumer) {
      return withInstanceDecorator(consumer);
    }

    @Override
    public TypedResourceAdapter<I, M> withLinkTitle(String title) {

      return withLinkDecorator(r -> r.setLinkTitle(title));
    }

    @Override
    public TypedResourceAdapter<I, M> withLinkName(String name) {

      return withLinkDecorator(r -> r.setLinkName(name));
    }

    @Override
    public TypedResourceAdapter<I, M> withQueryParameters(Map<String, Object> parameters) {

      return withLinkDecorator(r -> r.setQueryParameters(parameters));
    }

    @Override
    public TypedResourceAdapter<I, M> withPartialLinkTemplate() {

      return withLinkDecorator(r -> r.setExpandAllVariables(false));
    }

    @Override
    public TypedResourceAdapter<I, M> withQueryParameterTemplate(String... names) {
      throw new HalApiDeveloperException("#withQueryParameterTemplatecan can only be called if you selected a null resource path to create a template");
    }

    @Override
    public M getInstance() {

      return getStreamOfModels()
          .findFirst()
          .orElseThrow(() -> new HalApiDeveloperException("No resources were found after selecting " + resourceSelector.description));
    }

    @Override
    public Optional<I> getOptional() {

      return getStream().findFirst();
    }

    @Override
    public Stream<I> getStream() {

      return getResources().map(this::adaptToModelTypeAnDecorateLinks);
    }

    private Stream<M> getStreamOfModels() {

      return getResources().map(this::adaptToModelTypeAnDecorateLinks);
    }

    private Stream<Resource> getResources() {
      Stream<Resource> resources = resourceSelector.resources;

      if (resources == null) {
        throw new HalApiDeveloperException("No resources have been selected with this adapter");
      }

      if (resourceFilter.predicate != null) {
        resources = resources.filter(resourceFilter.predicate);
      }
      return resources;
    }

    private M adaptToModelTypeAnDecorateLinks(Resource res) {

      M model = slingRhyme.adaptResource(res, this.clazz);

      instanceDecorators.forEach(decorator -> decorator.accept(model));

      return model;
    }

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


  private final class TemplateResourceAdapter<I, M extends I> implements TypedResourceAdapter<I, M> {

    private static final String PATH_PLACEHOLDER = "/letsassumethisisunlikelytoexist";

    private final Class<I> halApiInterface;

    private String linkTitle;
    private String linkName;
    private String[] queryParameters;

    private TemplateResourceAdapter(Class<I> halApiInterface) {
      this.halApiInterface = halApiInterface;
    }

    @Override
    public TypedResourceAdapter<I, M> withLinkTitle(String title) {
      this.linkTitle = title;
      return this;
    }

    @Override
    public TypedResourceAdapter<I, M> withLinkName(String name) {
      this.linkName = name;
      return this;
    }

    @Override
    public TypedResourceAdapter<I, M> withQueryParameterTemplate(String... names) {
      this.queryParameters = names;
      return this;
    }

    @Override
    public TypedResourceAdapter<I, M> withQueryParameters(Map<String, Object> parameters) {
      throw new HalApiDeveloperException("#withQueryParameters cannot be called if you selected a null resource path to build a template");
    }

    @Override
    public TypedResourceAdapter<I, M> withPartialLinkTemplate() {
      throw new HalApiDeveloperException("#withPartialLinkTemplate cannot be called if you selected a null resource path to build a template");
    }

    @Override
    public TypedResourceAdapterImpl<I, M> withModifications(Consumer<M> decorator) {
      throw new HalApiDeveloperException("#withModifications cannot be called if you selected a null resource path to build a template");
    }

    @Override
    public M getInstance() {
      return createResource();
    }

    @Override
    public Optional<I> getOptional() {
      return Optional.of(createResource());
    }

    @Override
    public Stream<I> getStream() {
      return Stream.of(createResource());
    }

    private <T extends LinkableResource> T createResource() {

      return (T)Proxy.newProxyInstance(halApiInterface.getClassLoader(), new Class[] { halApiInterface }, new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

          if (method.getName().equals("createLink")) {
            return createLink();
          }

          throw new HalApiDeveloperException("Unsupported call to " + method.getName() + " method on "
              + halApiInterface.getName() + " proxy instance. "
              + "Any instances created with SlingLinkBuilder#selectResourceAt(null can only be used to create link templates for these resources");
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

      return urlHandler.get(PATH_PLACEHOLDER)
          .selectors(selector)
          .extension(HalApiServlet.EXTENSION)
          .buildExternalLinkUrl();
    }
  }
}
