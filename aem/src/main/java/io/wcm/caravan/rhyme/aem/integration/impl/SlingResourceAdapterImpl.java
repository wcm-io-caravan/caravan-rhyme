package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import io.wcm.caravan.rhyme.aem.integration.ResourceStreams;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
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
  private ResourceSelectorRegistry registry;

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
  public <ModelType> TypedResourceAdapter<ModelType> adaptTo(Class<ModelType> clazz) {

    if (templateGenerationRequired) {
      return new TemplateResourceAdapter<ModelType>(clazz);
    }

    return new TypedResourceAdapterImpl<ModelType>(clazz);
  }


  private final class TypedResourceAdapterImpl<ModelType> implements TypedResourceAdapter<ModelType> {

    private final Class<ModelType> clazz;

    private final CompositeLinkDecorator<ModelType> linkDecorator;

    private TypedResourceAdapterImpl(Class<ModelType> clazz) {
      this.clazz = clazz;
      this.linkDecorator = new CompositeLinkDecorator<ModelType>();
    }

    private TypedResourceAdapterImpl(Class<ModelType> clazz, CompositeLinkDecorator<ModelType> decorator) {
      this.clazz = clazz;
      this.linkDecorator = decorator;
    }

    private TypedResourceAdapterImpl<ModelType> withLinkDecorator(LinkDecorator<ModelType> decorator) {
      return new TypedResourceAdapterImpl<ModelType>(clazz, linkDecorator.withAdditionalDecorator(decorator));
    }

    @Override
    public TypedResourceAdapter<ModelType> withLinkTitle(String title) {

      return withLinkDecorator(new LinkDecorator<ModelType>() {

        @Override
        public String getLinkTitle(Resource resource, ModelType model) {
          return title;
        }

      });
    }

    @Override
    public TypedResourceAdapter<ModelType> withLinkName(String name) {

      return withLinkDecorator(new LinkDecorator<ModelType>() {

        @Override
        public String getLinkName(Resource resource, ModelType model) {
          return name;
        }

      });
    }

    @Override
    public TypedResourceAdapter<ModelType> withQueryParameters(Map<String, Object> parameters) {

      return withLinkDecorator(new LinkDecorator<ModelType>() {

        @Override
        public Map<String, Object> getQueryParameters() {
          return parameters;
        }
      });
    }

    @Override
    public TypedResourceAdapter<ModelType> withQueryParameterTemplate(String... names) {
      throw new HalApiDeveloperException("#withQueryParameterTemplatecan only be called if you selected a null resource path to create a template");
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

      decorateLinks(res, model);

      return model;
    }

    private void decorateLinks(Resource resource, ModelType model) {

      if (!linkDecorator.hasDelegates()) {
        return;
      }

      if (!(model instanceof SlingLinkableResource)) {
        throw new HalApiDeveloperException(
            "Your model class " + model.getClass().getSimpleName() + " does not implement " + SlingLinkableResource.class.getName()
                + " (which is required if you want to override link names and titles via SlingResourceAdapter)");
      }

      SlingLinkableResource linkable = (SlingLinkableResource)model;

      String title = linkDecorator.getLinkTitle(resource, model);
      if (title != null) {
        linkable.setLinkTitle(title);
      }

      String name = linkDecorator.getLinkName(resource, model);
      if (name != null) {
        linkable.setLinkName(name);
      }

      Map<String, Object> parameters = linkDecorator.getQueryParameters();
      if (parameters != null) {
        linkable.setQueryParameters(parameters);
      }
    }


  }

  private interface LinkDecorator<ModelType> {

    default String getLinkTitle(Resource resource, ModelType model) {
      return null;
    }

    default String getLinkName(Resource resource, ModelType model) {
      return null;
    }

    default Map<String, Object> getQueryParameters() {
      return null;
    }
  }

  private class CompositeLinkDecorator<ModelType> implements LinkDecorator<ModelType> {

    private final List<LinkDecorator<ModelType>> delegates = new ArrayList<>();

    CompositeLinkDecorator<ModelType> withAdditionalDecorator(LinkDecorator<ModelType> decorator) {
      CompositeLinkDecorator<ModelType> newInstance = new CompositeLinkDecorator<>();

      newInstance.delegates.addAll(this.delegates);
      newInstance.delegates.add(decorator);
      return newInstance;
    }

    private <T> T findFirstNonNull(Function<LinkDecorator<ModelType>, T> func) {

      return delegates.stream()
          .map(func)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }

    public boolean hasDelegates() {
      return !delegates.isEmpty();
    }

    @Override
    public String getLinkTitle(Resource resource, ModelType model) {

      return findFirstNonNull(dec -> dec.getLinkTitle(resource, model));
    }

    @Override
    public String getLinkName(Resource resource, ModelType model) {

      return findFirstNonNull(dec -> dec.getLinkName(resource, model));
    }

    @Override
    public Map<String, Object> getQueryParameters() {

      return findFirstNonNull(dec -> dec.getQueryParameters());
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


  private final class TemplateResourceAdapter<T> implements TypedResourceAdapter<T> {

    private static final String PATH_PLACEHOLDER = "/letsassumethisisunlikelytoexist";

    private final Class<T> halApiInterface;

    private String linkTitle;
    private String linkName;
    private String[] queryParameters;

    private TemplateResourceAdapter(Class<T> halApiInterface) {
      this.halApiInterface = halApiInterface;
    }

    @Override
    public TypedResourceAdapter<T> withLinkTitle(String title) {
      this.linkTitle = title;
      return this;
    }

    @Override
    public TypedResourceAdapter<T> withLinkName(String name) {
      this.linkName = name;
      return this;
    }

    @Override
    public TypedResourceAdapter<T> withQueryParameterTemplate(String... names) {
      this.queryParameters = names;
      return this;
    }

    @Override
    public TypedResourceAdapter<T> withQueryParameters(Map<String, Object> parameters) {
      throw new HalApiDeveloperException("#withQueryParameters cannot be called if you selected a null resource path to build a template");
    }


    @Override
    public T getInstance() {
      return createResource();
    }

    @Override
    public Optional<T> getOptional() {
      return Optional.of(createResource());
    }

    @Override
    public Stream<T> getStream() {
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
              + "Any instances created with SlingLinkBuilder#buildTemplateTo can only be used to create link templates for these resources");
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
