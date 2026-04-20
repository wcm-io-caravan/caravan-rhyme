# Designing HAL API Interfaces with Rhyme

This is a reference for modeling HAL+JSON APIs as annotated Java interfaces using the Rhyme framework. It covers every annotation and design pattern available, organized by the decisions you face when designing your API.

For general concepts (client proxies, server-side rendering, caching, error handling), see the [README](../README.md).

## Quick Reference

| Annotation | Purpose |
|---|---|
| `@HalApiInterface` | Marker on every HAL resource interface |
| `@Related` | Declares a link relation to another resource |
| `@ResourceState` | Maps the full JSON state to a single object |
| `@ResourceProperty` | Maps individual JSON properties to methods |
| `@ResourceLink` | Exposes the link/URI to the resource itself |
| `@ResourceRepresentation` | Gives access to the raw HAL+JSON representation |
| `@TemplateVariable` | Binds a method parameter to a URI template variable |
| `@TemplateVariables` | Binds a DTO parameter to multiple URI template variables |

| Base Interface | Purpose |
|---|---|
| `LinkableResource` | Resource accessible via its own URL (has a `self` link) |
| `EmbeddableResource` | Server-side only; controls whether a resource is embedded |

---

## Choosing a Base Interface

Every `@HalApiInterface` must decide whether to extend `LinkableResource`, and this choice is visible to clients.

### Extending `LinkableResource`

Most resource interfaces extend `LinkableResource`. This tells clients that the resource has its own URL and can be fetched independently. Clients can call `createLink()` on any proxy to obtain the `Link` (and its `href`) without fetching the resource itself.

```java
// examples/spring-hypermedia/.../CompanyApi.java
@HalApiInterface
public interface CompanyApi extends LinkableResource {
  // ...
}
```

```java
// examples/aws-movie-search/.../Movie.java
@HalApiInterface
public interface Movie extends LinkableResource {
  // ...
}
```

### Not Extending `LinkableResource`

An interface that does not extend `LinkableResource` can still represent a HAL resource. The difference is that clients have no typed way to obtain the resource's URL. This is used when the interface is consumed purely through navigation (following links from other resources) and the URL is not something clients should care about.

```java
// examples/osgi-jaxrs-example-service/.../ExamplesEntryPointResource.java
@HalApiInterface
public interface ExamplesEntryPointResource {
  // no base interface - clients navigate through relations only
}
```

```java
// examples/osgi-jaxrs-example-service/.../CollectionExamplesResource.java
@HalApiInterface
public interface CollectionExamplesResource {
  // ...
}
```

### Extending `EmbeddableResource`

`EmbeddableResource` is a server-side concern and should **not** appear in your `@HalApiInterface`. Instead, your server-side implementation class implements it to control embedding behavior. However, you can make an interface extend `EmbeddableResource` instead of `LinkableResource` when a resource is designed to *only* ever appear embedded and never be fetched by URL on its own:

```java
// examples/aws-movie-search/.../SearchResult.java
@HalApiInterface
public interface SearchResult extends EmbeddableResource {

  @ResourceProperty
  String getTitle();

  @ResourceProperty
  String getDescription();

  @Related("movie")
  Movie getMovie();
}
```

```java
// examples/aem-hal-browser/.../AemLinkedContent.java
@HalApiInterface
public interface AemLinkedContent extends EmbeddableResource {

  @Related(AemRelations.PAGE)
  Stream<AemPage> getLinkedPages();

  @Related(AemRelations.ASSET)
  Stream<AemAsset> getLinkedAssets();
}
```

---

## Representing Resource State

HAL resources have a JSON state (everything outside `_links` and `_embedded`). Rhyme offers two approaches, and you can combine them in a single interface.

### `@ResourceState` -- Whole State as a Single Object

Use `@ResourceState` when your resource state maps naturally to a Java class (or when you want to pass through arbitrary JSON). There must be at most one `@ResourceState` method per interface.

**With a typed domain class:**

```java
// examples/spring-hypermedia/.../EmployeeResource.java
@HalApiInterface
public interface EmployeeResource extends LinkableResource {

  @ResourceState
  Employee getState();
}
```

On the client side, `Employee` is deserialized from the resource JSON using Jackson's default `ObjectMapper`. On the server side, the returned `Employee` is serialized into the resource's JSON properties.

**With `ObjectNode` for generic/untyped JSON:**

```java
// examples/aem-hal-browser/.../SlingResource.java
@HalApiInterface
public interface SlingResource extends LinkableResource {

  @ResourceState
  ObjectNode getProperties();
}
```

This is useful when the JSON structure is not known in advance or varies across resources.

### `@ResourceProperty` -- Individual JSON Properties as Methods

Use `@ResourceProperty` when your resource has a small number of properties and creating a dedicated Java class feels excessive, or when you want to expose only specific properties from a larger JSON object.

```java
// examples/aws-movie-search/.../Movie.java
@HalApiInterface
public interface Movie extends LinkableResource {

  @ResourceProperty
  String getTitle();

  @ResourceProperty
  int getYear();

  @ResourceProperty
  float getRating();

  @ResourceProperty
  int getRank();

  @ResourceProperty
  String getImdbId();

  @ResourceProperty
  String getThumb();
}
```

By default, the JSON property name is derived from the method name using Java bean conventions (`getTitle()` maps to `"title"`). You can override this:

```java
// examples/spring-hypermedia/.../DetailedEmployeeResource.java
@ResourceProperty("manager")
String getManagerName();
```

### Combining `@ResourceState` and `@ResourceProperty`

You can use both in the same interface. `@ResourceProperty` methods provide fine-grained access to specific properties, while `@ResourceState` gives access to the full state object.

```java
// examples/spring-hypermedia/.../DetailedEmployeeResource.java
@HalApiInterface
public interface DetailedEmployeeResource extends LinkableResource {

  @ResourceState
  Employee getState();

  @ResourceProperty("manager")
  String getManagerName();
}
```

### State DTO Conventions

The class used with `@ResourceState` must be (de)serializable by Jackson's default `ObjectMapper`. Two common styles:

**Public fields (struct-like):**

```java
// tooling/docs-maven-plugin/.../ResourceWithFieldProperties.java
class FieldProperties {
  public String foo;
  public Integer bar;
  public List<Boolean> list;
}
```

**Bean-style with getters/setters:**

```java
// tooling/docs-maven-plugin/.../ResourceWithRxBeanProperties.java
public static class BeanProperties {
  private String foo;
  private Integer bar;
  public String getFoo() { return this.foo; }
  public Integer getBar() { return this.bar; }
  public void setFoo(String foo) { this.foo = foo; }
  public void setBar(Integer bar) { this.bar = bar; }
}
```

---

## Modeling Relationships Between Resources

The `@Related` annotation is the core of API navigation. It maps a method to a HAL link relation, and the method's return type determines how many related resources are expected.

### Cardinality: One, Optional, or Many

**Exactly one** -- the link is always present. Use a direct return type:

```java
// examples/spring-hypermedia/.../CompanyApi.java
@Related("company:employees")
EmployeeCollectionResource getEmployees();
```

**Zero or one** -- the link may be absent. Use `Optional`:

```java
// examples/spring-hypermedia/.../ManagerResource.java
@Related(CANONICAL)
Optional<ManagerResource> getCanonical();
```

**Zero or more** -- multiple links with the same relation. Use `Stream` or `List`:

```java
// examples/spring-hypermedia/.../DetailedEmployeeResource.java
@Related("company:colleague")
Stream<EmployeeResource> getColleagues();
```

```java
// examples/spring-hypermedia/.../EmployeeCollectionResource.java
@Related("company:employee")
List<EmployeeResource> getAll();
```

### Reactive Return Types

If you want your client code to work asynchronously (composing operations with RxJava's `map`, `flatMap`, `concatMap`, etc.), use RxJava 3 types in your interfaces. This is an important design decision: once your interfaces use reactive types, both client and server code work with them.

Choose reactive types when:
- You need parallel fetching of multiple upstream resources
- Your server-side rendering should be non-blocking
- You want to compose complex chains of API operations

Choose blocking types when:
- Simplicity matters more than concurrency
- You're building straightforward request/response services

The reactive equivalents of the blocking return types are:

| Blocking | Reactive | Cardinality |
|---|---|---|
| `T` | `Single<T>` | Exactly one |
| `Optional<T>` | `Maybe<T>` | Zero or one |
| `Stream<T>` / `List<T>` | `Observable<T>` | Zero or more |

```java
// examples/osgi-jaxrs-example-service/.../ExamplesEntryPointResource.java
@HalApiInterface
public interface ExamplesEntryPointResource {

  @Related("examples:collections")
  Single<CollectionExamplesResource> getCollectionExamples();

  @Related("examples:errors")
  Single<ErrorExamplesResource> getErrorExamples();

  @Related("latest-version")
  Maybe<ExamplesEntryPointResource> getLatestVersion();
}
```

```java
// examples/osgi-jaxrs-example-service/.../ItemCollectionResource.java
@HalApiInterface
public interface ItemCollectionResource {

  @ResourceProperty
  Maybe<String> getTitle();

  @Related(StandardRelations.ALTERNATE)
  Maybe<ItemCollectionResource> getAlternate();

  @Related("examples:item")
  Observable<ItemResource> getItems();
}
```

Reactive wrappers apply to `@ResourceState` and `@ResourceProperty` methods as well:

```java
// examples/osgi-jaxrs-example-service/.../ItemResource.java
@HalApiInterface
public interface ItemResource {

  @ResourceState
  Single<ItemState> getProperties();
}
```

```java
// examples/osgi-jaxrs-example-service/.../ErrorResource.java
@HalApiInterface
public interface ErrorResource {

  @ResourceProperty
  Maybe<String> getTitle();
}
```

### Link Relations: Standard vs. Custom

**IANA standard relations** are available as constants in `StandardRelations`:

```java
// examples/aws-movie-search/.../MoviesPage.java
@Related(FIRST)
Optional<MoviesPage> getFirstPage();

@Related(PREV)
Optional<MoviesPage> getPrevPage();

@Related(NEXT)
Optional<MoviesPage> getNextPage();

@Related(LAST)
Optional<MoviesPage> getLastPage();
```

Available constants include: `ABOUT`, `ALTERNATE`, `CANONICAL`, `COLLECTION`, `FIRST`, `INDEX`, `ITEM`, `LAST`, `NEXT`, `PREV`, `RELATED`, `SEARCH`, `SECTION`, `SELF`, `SUBSECTION`, `UP`, `VIA`, and others.

**Custom relations** use a CURI (compact URI) prefix:

```java
// examples/spring-hypermedia/.../CompanyApi.java
@Related("company:employees")
EmployeeCollectionResource getEmployees();

@Related("company:manager")
ManagerResource getManagerById(@TemplateVariable(ID) Long id);
```

The prefix (e.g. `company:`) corresponds to a `curies` link in the HAL response that maps the prefix to a documentation URL. Using CURIs is recommended for all custom relations as it enables integration with HAL browsers and documentation tooling.

### Reactive Return Types (RxJava 3)

For more advanced scenarios, specificially when you need to handle partial failures or execute requests in parallel, you can use **RxJava 3** types:

*   `Single<T>`: When a single resource or value is expected.
*   `Maybe<T>`: When a resource might not exist (similar to `Optional`).
*   `Observable<T>`: For streams of resources (similar to `Stream`).

```java
@HalApiInterface
public interface AsyncResource {

  @Related("item")
  Observable<ItemResource> getItems();

  @ResourceState
  Single<ItemState> getState();
}
```

**Benefits of Reactive Types:**
*   **Resilience**: You can use operators like `retry()`, `timeout()`, or `onErrorReturn()` to gracefully handle upstream failures.
*   **Partial Results**: If fetching a related resource fails, you can return an empty `Observable` or a fallback value instead of failing the entire request.
*   **Parallelism**: Rhyme will automatically execute independent upstream requests in parallel.

### Links to Non-HAL Resources

Methods annotated with `@Related` can return a `Link` directly (instead of another `@HalApiInterface` type) to represent links to non-HAL resources like HTML pages, images, or binary files:

```java
// examples/spring-hypermedia/.../DetailedEmployeeResource.java
@Related("company:htmlProfile")
Link getHtmlProfileLink();
```

```java
// examples/aem-hal-browser/.../AemAsset.java
@Related(AemRelations.BINARY)
Optional<Link> getOriginalRendition();
```

This works with `Optional` for links that may not be present.

### Self-Referential Relations

An interface can have methods that return the same type, which is common for pagination and preference links:

```java
// examples/aws-movie-search/.../MoviesPage.java
@HalApiInterface
public interface MoviesPage extends LinkableResource {

  @Related(NEXT)
  Optional<MoviesPage> getNextPage();

  @Related(PREV)
  Optional<MoviesPage> getPrevPage();
}
```

```java
// examples/spring-hypermedia/.../CompanyApi.java
@Related("company:preferences")
CompanyApi withClientPreferences(
    @TemplateVariable(USE_EMBEDDED_RESOURCES) Boolean useEmbeddedResources,
    @TemplateVariable(USE_FINGERPRINTING) Boolean useFingerprinting,
    @TemplateVariable(EMBED_RHYME_METADATA) Boolean embedRhymeMetadata);
```



### Links for State Modification (POST/PUT/DELETE)

If a relation is used *only* for state modification (and not for fetching state via GET), you can return a `Link` object directly from a `@Related` method. This gives clients typed access to the URL and metadata without implying that the target can be fetched as a HAL resource via Rhyme.

```java
@Related("ex:create-item")
Link getCreateItemLink();
```

For resources that support both GET and other methods, simply use your existing `@Related` method returning a proxy. You can call `createLink()` on the proxy to get the `Link` object (and its URL) without executing a GET request.

> **Tip**: Creating a link proxy and calling `createLink()` does not trigger a network request. It effectively just looks up the link in the current resource's `_links` property. This also gives you access to link attributes (like `title`, `name`, `deprecation`) immediately.



## URI Templates and Parameters

When a link relation is parameterized (e.g. "get item by ID"), the `@Related` method takes parameters annotated with `@TemplateVariable` or `@TemplateVariables`.

### `@TemplateVariable` -- Individual Parameters

Each parameter maps to a single variable in the URI template:

```java
// examples/spring-hypermedia/.../CompanyApi.java
@Related("company:employee")
EmployeeResource getEmployeeById(@TemplateVariable("id") Long id);
```

Multiple parameters for the same template:

```java
// examples/aws-movie-search/.../MoviesDemoApi.java
@Related("movies")
MoviesPage getMoviesPage(
    @TemplateVariable("page") Integer page,
    @TemplateVariable("size") Integer size);
```

```java
// examples/aem-hal-browser/.../AemAsset.java
@Related(AemRelations.RENDITION)
Optional<AemRendition> getRendition(
    @TemplateVariable("width") Integer width,
    @TemplateVariable("height") Integer height);
```

When a `@TemplateVariable` parameter is `null`, the corresponding variable is left unexpanded in the URI template. This is how link templates are rendered on the server side (where the framework calls the method with `null` parameters to generate the template).

### `@TemplateVariables` -- DTO Parameter

When a link template has many variables, you can group them into a single DTO to simplify the method signature:

```java
// examples/osgi-jaxrs-example-service/.../CollectionExamplesResource.java
@Related("delayed:collection")
Single<ItemCollectionResource> getDelayedCollection(
    @TemplateVariables CollectionParameters options);
```

The DTO can be defined as an **interface with getters** (variable names derived via bean conventions):

```java
// examples/osgi-jaxrs-example-service/.../CollectionParameters.java
public interface CollectionParameters {
  int getNumItems();
  boolean getEmbedItems();
  int getDelayMs();
}
```

Or as a **class with public fields** (field names are the variable names directly):

```java
public class PaginationParams {
  public Integer page;
  public Integer size;
}
```

Classes with private fields are **not** supported by `@TemplateVariables` (the framework cannot access them via reflection).

**Constraints**:
- **No Nesting**: The DTO must be flat. Nested objects are not supported and will not be mapped to template variables.
- **Naming**: Variable names are derived exactly from public field names or bean-style getters. Custom annotations (like Jackson's `@JsonProperty`) are **ignored**.
- **Type Conversion**: Rhyme does *not* handle type conversion on the server side. Your DTO values are passed as-is (usually as Strings derived from the URL parameters). You must ensure your server framework handles the conversion from String to your desired types (e.g., `Long`, `Integer`).

> **Note**: Using `@TemplateVariables` ensures that adding new optional fields to the DTO is **not** a breaking change for your API clients.

---

## Accessing Links and Raw Representations

### `@ResourceLink` -- Accessing the Resource's Own Link

`LinkableResource` already declares `createLink()` annotated with `@ResourceLink`, so if your interface extends `LinkableResource`, clients can call `createLink()` to get the `Link` (including `href`, `title`, `name`) without fetching the resource.

You can also declare a method returning `String` instead of `Link` to get just the URI:

```java
// pattern from core/src/test/.../ResourceLinkTest.java
@HalApiInterface
interface ResourceWithUri extends LinkableResource {
  @ResourceLink
  String getUri();
}
```

### `@ResourceRepresentation` -- Raw HAL+JSON Access

If clients need the full HAL+JSON representation (including `_links` and `_embedded`) rather than the abstracted interface methods, declare a `@ResourceRepresentation` method:

```java
// examples/osgi-jaxrs-example-service/.../ExamplesEntryPointResource.java
@ResourceRepresentation
HalResource asHalResource();
```

Supported return types are `HalResource`, `ObjectNode`, `JsonNode`, or `String` (the serialized JSON). These can also be wrapped in reactive types (`Single<HalResource>`, etc.).

This is only needed when you want to process the raw HAL response with another library or perform operations that Rhyme's typed interface doesn't cover.

---

## Custom Content Type

The `@HalApiInterface` annotation has a `contentType` attribute (default: `"application/hal+json"`) that controls the `Content-Type` header when the resource is rendered server-side:

```java
@HalApiInterface(contentType = "application/vnd.example.v2+json")
public interface CustomContentTypeResource extends LinkableResource {
  // ...
}
```

---

## Common Patterns

### Entry Point

An entry point is the root resource of your API. It typically extends `LinkableResource`, has no `@ResourceState`, and provides link templates plus navigation links to discoverable collections:

```java
// examples/aws-movie-search/.../ApiEntryPoint.java
@HalApiInterface
public interface ApiEntryPoint extends LinkableResource {

  @Related("movies:search")
  SearchResultPage getSearchResults(@TemplateVariable("searchTerm") String searchTerm);

  @Related("movies:source")
  MoviesDemoApi getUpstreamEntryPoint();
}
```

### Pagination

Use `Optional` self-references with standard relations (`FIRST`, `PREV`, `NEXT`, `LAST`) for page navigation, and `Stream` or `List` for the page content. A `@ResourceState` with paging metadata completes the pattern:

```java
// examples/aws-movie-search/.../MoviesPage.java
@HalApiInterface
public interface MoviesPage extends LinkableResource {

  @ResourceState
  PagingInfo getPagingInfo();

  @Related("movies")
  Stream<Movie> getPageContent();

  @Related(FIRST)
  Optional<MoviesPage> getFirstPage();

  @Related(PREV)
  Optional<MoviesPage> getPrevPage();

  @Related(NEXT)
  Optional<MoviesPage> getNextPage();

  @Related(LAST)
  Optional<MoviesPage> getLastPage();
}
```

### Collection Resource

A dedicated resource that lists all items of a type, often with cross-links back to the entry point or related collections:

```java
// examples/spring-hypermedia/.../EmployeeCollectionResource.java
@HalApiInterface
public interface EmployeeCollectionResource extends LinkableResource {

  @Related("company:employee")
  List<EmployeeResource> getAll();

  @Related("company:api")
  CompanyApi getApi();

  @Related("company:managers")
  ManagerCollectionResource getManagers();
}
```

### Embedded-Only Resource

A resource that only appears embedded within another resource and has no URL of its own. Extend `EmbeddableResource` instead of `LinkableResource`:

```java
// examples/aws-movie-search/.../SearchResult.java
@HalApiInterface
public interface SearchResult extends EmbeddableResource {

  @ResourceProperty
  String getTitle();

  @ResourceProperty
  String getDescription();

  @Related("movie")
  Movie getMovie();

  @Related("directors")
  Stream<Director> getDirectors();
}
```

### Resource Hierarchy / Cross-References

Resources can reference each other bidirectionally and offer alternative "views" of the same underlying entity:

```java
// examples/aem-hal-browser/.../SlingResource.java
@HalApiInterface
public interface SlingResource extends LinkableResource {

  @ResourceState
  ObjectNode getProperties();

  @Related(AemRelations.PAGE)
  Optional<AemPage> asAemPage();

  @Related(AemRelations.ASSET)
  Optional<AemAsset> asAemAsset();

  @Related(AemRelations.SLING_PARENT)
  Optional<SlingResource> getParent();

  @Related(AemRelations.SLING_CHILD)
  Stream<SlingResource> getChildren();
}
```

### Client Preferences via Link Template

Use a self-referencing method with `@TemplateVariable` parameters to let clients select API behavior (embedding, fingerprinting, metadata):

```java
// examples/spring-hypermedia/.../CompanyApi.java
@Related("company:preferences")
CompanyApi withClientPreferences(
    @TemplateVariable(USE_EMBEDDED_RESOURCES) Boolean useEmbeddedResources,
    @TemplateVariable(USE_FINGERPRINTING) Boolean useFingerprinting,
    @TemplateVariable(EMBED_RHYME_METADATA) Boolean embedRhymeMetadata);
```
