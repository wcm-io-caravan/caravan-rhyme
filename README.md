<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme
======
[![Build](https://github.com/wcm-io-caravan/caravan-rhyme/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-rhyme/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=wcm-io-caravan_caravan-rhyme&metric=alert_status)](https://sonarcloud.io/summary/overall?id=wcm-io-caravan_caravan-rhyme)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wcm-io-caravan_caravan-rhyme&metric=coverage)](https://sonarcloud.io/component_measures?id=wcm-io-caravan_caravan-rhyme&metric=coverage)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=wcm-io-caravan_caravan-rhyme&metric=sqale_index)](https://sonarcloud.io/project/issues?id=wcm-io-caravan_caravan-rhyme&resolved=false)


![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

# Introduction

**Rhyme** is a Java framework for providing or consuming hypermedia APIs using the [HAL+JSON media format](http://stateless.co/hal_specification.html). Its main use case is building a distributed system of web services connected through several HAL APIs.

**Rhyme** stands for **R**eactive **Hy**per**me**dia, as it fully supports asynchronous generation and retrieval of HAL+JSON resources (using [RxJava 3](https://github.com/ReactiveX/RxJava) internally). However, using reactive types is mostly optional (with very few exceptions). This document primarily uses simpler, blocking code examples, but a dedicated section explains how to use reactive types.

The key concepts and features of **Rhyme** are:
- HAL APIs are represented as **annotated Java interfaces** that define the resource state's structure and available related resources.
- These interfaces can be used by consumers as a **highly abstracted client API**, for which Rhyme will create a client implementation at runtime.
- The same interfaces are also used to **keep the server-side implementation well-structured** and always in sync with the API interfaces.
- Writing extensive **integration tests** is easy without wasting time constructing and verifying URL details.
- Simple and transparent support for **embedded resources**.
- Generation and integration of **HTML API documentation** from the annotated interfaces.
- A **simple and effective caching model** based on URL fingerprinting and the `cache-control: max-age` header.
- Simplifying **data debugging and performance analysis** by including embedded metadata in every response.
- **Forwarding error information across service boundaries** using the [vnd.error](https://github.com/blongden/vnd.error) media type.
- **Supporting asynchronous, reactive programming** on both the client and server sides.

Each of these concepts and features is explained in more detail below.

**Rhyme** is based on several years of experience and best practices from a large-scale production HAL microservice platform (utilizing OSGi, JAX-RS, RxJava & wcm.io Caravan code). When we transitioned to Spring Boot for new services, we seized the opportunity to rewrite the HAL client and rendering code to be usable within any Java project and accessible to everyone.

## Limitations

An important point to note is that the **Rhyme** framework currently lacks support for [HAL-FORMS](https://rwcbook.github.io/hal-forms/). Additionally, the annotations and client implementations are designed to support only **GET** requests to API resources. This is because our primary use case was aggregating and caching read-only resources from many different data sources.

You can still use **Rhyme**'s client interfaces to follow links to a resource and then extract the URL to POST or PUT data using any other HTTP client library.

If you believe that more sophisticated support for other HTTP methods should be added, please open an issue to discuss the best way to extend **Rhyme**!

## Modules in this repository

- [api-interfaces](api-interfaces) - Contains only annotations, interfaces, and dependencies for your API interface definitions.
- [core](core) - The core framework, integrable into any Java project.
- **Integration** modules for using Rhyme with specific web service frameworks:
  - [spring](integration/spring) - For implementing HAL web services as a Spring (Boot) application.
  - [osgi-jaxrs](integration/osgi-jaxrs) - For implementing HAL web services using the [OSGi R7 JAX-RS Whiteboard](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html) and related [wcm.io Caravan](https://github.com/wcm-io-caravan) projects.
  - [aem](integration/aem) - Code and APIs for use in Adobe Experience Manager (work in progress and not yet released).
- **Examples** demonstrating how to use Rhyme in these frameworks:
  - [spring-hypermedia](examples/spring-hypermedia) - A well-documented Spring Boot application with examples for most core framework concepts.
  - [spring-hello-world](examples/spring-hello-world) - A very simple Spring Boot application with minimal dependencies.
  - [aws-movie-search](examples/aws-movie-search) - An AWS Lambda example that consumes another HAL+JSON API and builds a simple search on top of it.
  - [osgi-jaxrs-example-service](examples/osgi-jaxrs-example-service) - An example service using reactive types in its API.
  - [osgi-jaxrs-example-launchpad](examples/osgi-jaxrs-example-launchpad) - A [Sling launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html) to start the OSGi/JAX-RS example service (and run some integration tests).
  - [aem-hal-browser](examples/aem-hal-browser) - An example project for AEM showing how HAL resources can be implemented as Sling models (work in progress).
- [testing](testing) - Additional integration testing support classes (for test scope only).
- Maven **Tooling**
  - [coverage](tooling/coverage) - Maven module for generating aggregated code coverage reports.
  - [docs-maven-plugin](tooling/docs-maven-plugin) - Maven Plugin for generating and embedding HTML API docs from annotated interfaces.
  - [parent](tooling/parent) - Common parent POM used by most other modules.

Another example of usage with Spring Boot can be found at https://github.com/feffef/reactive-hal-spring-example.

## Build from sources

If you want to build Rhyme from the sources, ensure you have configured the `OSS Sonatype Snapshots` repository in your `~/.m2/settings.xml` file. See the CI build's [Maven Settings](.maven-settings.xml) for an example of a full configuration.

Using **JDK 11, 17, or 21** and **Apache Maven 3.6.3** (or higher), you should be able to build all modules (and run the integration tests) from the root directory:

```
mvn -s .maven-settings.xml clean install
```

If this build fails on your machine, please open an issue on GitHub or [Jira](https://wcm-io.atlassian.net/projects/WCARAV/) and include the full stack trace after running the Maven build again with the `-e` switch.

# Key concepts explained

## Define a HAL API with annotated interfaces

As an example, here is the HAL+JSON entry point resource of a simple web service that provides access to a database of generic items:

```json
{
  "_links":{
    "self":{
      "href":"https://hal-api.example.org/",
      "title":"The entry point for this example HAL API"
    },
    "item":{
      "href":"https://hal-api.example.org/items/{id}",
      "templated":true,
      "title":"A link template to retrieve the item with the specified id from the database"
    },
    "first":{
      "href":"https://hal-api.example.org/items",
      "title":"A pageable list of all available items in the database"
    }
  }
}
```

And this is how the corresponding Java interface looks like:

```java
  @HalApiInterface
  public interface ApiEntryPoint extends LinkableResource {

    @Related("item")
    ItemResource getItemById(@TemplateVariable("id") String id);

    @Related("first")
    PageResource getFirstPage();
  }
```
- [@HalApiInterface](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/HalApiInterface.java) is a marker annotation that helps the framework identify the relevant interfaces through reflection.
- Extending [LinkableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/LinkableResource.java) defines that this resource is directly accessible via a URL (found in the `self` link).
- The `getItemById` function corresponds to the link template with the `item` relation. The parameter annotated with [@TemplateVariable](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/TemplateVariable.java) indicates that you must provide an `id` parameter. This parameter will expand the link template into the final URL used to retrieve the item resource.
- A HAL API should allow discovery of all available data whenever possible. That's why there is also a `getFirstPage` function, allowing you to start browsing all available items using the `first` link.

The return types of these functions are also annotated Java interfaces. They describe the structure and available relations of the linked resources:

```java
  @HalApiInterface
  public interface PageResource extends LinkableResource {

    @Related("item")
    Stream<ItemResource> getItemsOnPage();

    @Related("next")
    Optional<PageResource> getNextPage();
  }
```

```java
  @HalApiInterface
  public interface ItemResource extends LinkableResource {

    @ResourceState
    Item getState();

    @Related("related")
    Stream<ItemResource> getRelatedItems();
  }
```
- Note that none of these interfaces define anything regarding the URL structure of these resources. This aligns with the [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) principle that clients should only need to know a single URL (the entry point). All other URLs should be discoverable through links in the resources.
- Methods annotated with [@Related](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) define all possible relations/links between resources.
- `Stream` is used as a return type whenever there might be multiple links with the same relation. If you prefer, you can use `List` instead.
- `Optional` is used when a link's presence is not guaranteed (e.g., the last page will not have a `next` link).
- The method annotated with [@ResourceState](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) ultimately returns the data structure containing the item's core properties.

### Representing JSON resource state

As the return type for the `@ResourceState` method, you could use either a [Jackson](https://github.com/FasterXML/jackson) `ObjectNode` or any other type that can be parsed from and serialized to JSON using the default Jackson `ObjectMapper`. Using generic JSON types in your API is preferable if you are forwarding JSON resources from an external source and expect the structure of those JSON resources to be extended frequently.

If you want to provide a strongly **typed** API to your consumers, you should define simple classes that match the JSON structure of your resource's state. You shouldn't share any **code** with these classes, so a simple, struct-like class such as this works well:

```java
  public class Item {

    public String id;
    public String title;
  }
```
If you don't like this style with public mutable fields, you can define the class with private fields and accessor methods or even use an interface. However, be aware that instances of this class or interface will need to be deserialized with Jackson on the client side. Therefore, you may need to use annotations (e.g., `@JsonCreator`) to allow your resource state instances to be created from the parsed JSON.

If you do not yet have such (de)serializable domain classes in your project that you could use with `@ResourceState`, you can also consider adding multiple methods (one for each JSON property) annotated with [@ResourceProperty](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceProperty.java) directly in your `@HalApiInterface`. This is also useful if a resource has very few JSON properties and you want to avoid creating a Java class to represent that JSON structure.

Regardless of which Java representation you prefer, an actual HAL+JSON resource matching the `ItemResource` interface defined above would look like this:

```json
{
  "id":"2",
  "title":"Item #2",
  "_links":{
    "self":{
      "href":"https://hal-api.example.org/items/2",
      "title":"The item with id '2'",
      "name":"2"
    },
    "related":[
      {
        "href":"https://hal-api.example.org/items/12",
        "title":"The item with id '12'",
        "name":"12"
      }
    ]
  }
}
```

### Sharing API interfaces between services

Some may argue that providing interfaces that define the API is a bad idea, as it encourages sharing out-of-band information about your API with clients. However, any consumer needs reasonable expectations about the available links, parameter types, and data structures provided by your API to create reliable client code. The aim of these annotated interfaces is to specify exactly these expectations in a concise, machine-readable format. Exposing implementation details (such as URL structures or logic) is avoided.

If you dislike the idea of sharing the same interfaces in client and server code (as outlined in the next sections), keep in mind that this is entirely optional. Your API will still use plain HAL+JSON data structures, and nothing forces you to use the **Rhyme** framework and these interfaces on **both** sides. If you are concerned about introducing binary dependency issues, you can copy the Java sources for these interfaces between projects. There is absolutely no requirement that the client and server use the same interfaces to represent the API. You can also consume a HAL+JSON API written with a completely different technology; you'll just have to create your own interfaces with annotated methods for the API's link relations that you are using.

However, especially if your team is the sole consumer of your API, sharing these interfaces between your services offers many benefits:
- Your IDE can understand where a specific API method is actually used.
- Your IDE can find the server-side implementation(s) of every API method.
- Refactoring your API before publication (e.g., renaming relations or parameter names) is very easy and reliable.

## Consuming HAL resources with Rhyme client proxies

When you have a set of interfaces representing a HAL API, you can use the Rhyme framework to automatically create a client implementation of those interfaces. This is similar to concepts like [Feign](https://github.com/OpenFeign/feign) or [Retrofit](https://github.com/square/retrofit) but is much better suited to HAL concepts (e.g., methods are mapped to **relations** rather than endpoints, and no URL patterns are exposed in the interfaces).

It requires only two lines of code to create a client implementation of your HAL API's entry point interface:

```java
  // create a HalApiClient that uses a default HTTP implementation
  HalApiClient client = HalApiClient.create();

  // create a dynamic proxy that knows how to fetch the entry point from the given URL.
  ApiEntryPoint api = client.getRemoteResource("https://hal-api.example.org", ApiEntryPoint.class);
```

Note: If you are also using Rhyme to **render** your resources, you shouldn't use `HalApiClient` directly. Instead, call the `Rhyme#getRemoteResource` method, which has the exact same signature and behavior. This ensures that the same `HalApiClient` instance is used throughout your incoming request, enabling caching and collection of performance metrics as explained in a later section.

Using the proxy instance of your entry point, you can easily navigate through all API resources by simply calling the methods defined in your interfaces:
```java
    // calling a method on the proxy will fetch the entry point, and then find and expand the URI template.
    ItemResource itemResource = api.getItemById("foo");
    
    // now you have an ItemResource that knows the full URL of the resource (and how to fetch it),
    // but again, the resource is only actually fetched when you call a method on the resource proxy
    Item foo = itemResource.getState();

    // You can call another method on the same resource instance (without fetching any resource twice),
    // and use Stream operations to fetch multiple related resources with a simple expression:
    List<Item> relatedToFoo = itemResource.getRelatedItems()
        .map(ItemResource::getState)
        .collect(Collectors.toList());
```
The Rhyme client proxy instances will take care of:
- Fetching and parsing the resource.
- Finding the links (or embedded resources) corresponding to the called method (based on the relations defined in the interface methods' annotations).
- Expanding link templates with parameters from the method invocation.
- Automatically fetching further linked resources as required (as soon as any method on a related resource instance is called).
- Converting the resource JSON state to the corresponding Java type.
- Keeping track of all retrieved resources.
 
A local in-memory cache ensures that each resource is fetched no more than once. Repeated calls to the same method (with the same parameters) return a cached value immediately (as long as you use the same `Rhyme` or `HalApiClient` instance).

Check out the [AWS Lambda Example](examples/aws-movie-search) to see this in action.

### Using a custom HTTP client implementation

By default, HTTP requests are executed using the JDK's `HttpURLConnection` class with default configurations. In many cases, you will need more control over executing the HTTP request (e.g., adding authentication) and may want to use a more sophisticated HTTP client library already used in your project or framework.

The execution of any HTTP requests by the **Rhyme** framework is fully customizable through the [HalResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalResourceLoader.java) SPI interface.

The interface consists of a single method that loads a HAL resource from a given URL and returns an RxJava `Single`. This `Single` emits a [HalResponse](core/src/main/java/io/wcm/caravan/rhyme/api/common/HalResponse.java) object when the response has been retrieved (or fails with a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java) if retrieval was not possible).

```java
Single<HalResponse> getHalResource(String uri);
```

You can implement this interface entirely yourself, but this will also require you to implement JSON parsing and exception handling according to the framework's expectations.

A simpler way is to implement the callback-style [HttpClientSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HttpClientSupport.java) interface and then use the [HalResourceLoaderBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/client/HalResourceLoaderBuilder.java).

In both cases, you should extend the [AbstractHalResourceLoaderTest](testing/src/main/java/io/wcm/caravan/rhyme/testing/client/AbstractHalResourceLoaderTest.java) (from the `testing` module) to test your implementation against a WireMock server. These unit tests ensure that all expectations regarding response and error handling are met.

The `HalResourceLoaderBuilder` also has further methods to enable persistent caching of responses, which are explained in a later section.


## Rendering HAL resources in your web service 

For the server-side implementation of your HAL API, you will need to implement the annotated API interfaces you defined earlier. You can then use the [Rhyme](core/src/main/java/io/wcm/caravan/rhyme/api/Rhyme.java) facade to automatically render a HAL+JSON representation based on the annotations found in the interfaces.

It's important to note that **you should create a single `Rhyme` instance** for each incoming request: 

```java
    // create a single Rhyme instance as early as possible in the request cycle 
    Rhyme rhyme = RhymeBuilder.create().buildForRequestTo(incomingRequest.getUrl());
    
    // instantiate your server-side implementation of the requested @HalApiInterface resource
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);
    
    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint).blockingGet();

    // finally convert that response to your framework's representation of a web/JSON response...
```

What `Rhyme#renderResponse` does is scan your implementation class for methods from the annotated `@HalApiInterface` and **recursively** call all those methods:

- `#createLink()` is called to generate the `self` link directly
- `#getFirstPage()` is called to create a PageResource instance, and then `PageResource#createLink()` is called to create the link to it
- `#getItemById()` is called (with the `id` parameter being null, as the entry point should only contain a link template and no specific id is known yet), and then again `ItemResource#createLink()` is called on the implementation instance being returned (to actually create the link template)

### Implementing HAL API interfaces on the server-side

Here's how the server-side implementation of the `ApiEntryPoint` interface could look like:

```java
  class ApiEntryPointImpl implements ApiEntryPoint {

    private final ItemDatabase database;

    ApiEntryPointImpl(ItemDatabase database) {
      this.database = database;
    }

    @Override
    public PageResource getFirstPage() {
      return new PageResourceImpl(database, 0);
    }

    @Override
    public ItemResource getItemById(String id) {
      return new ItemResourceImpl(database, id);
    }

    @Override
    public Link createLink() {

      return new Link("https://hal-api.example.org/")
          .setTitle("The entry point for this example HAL API");
    }
  }
```

Note that the implementation of the `@Related` methods looks exactly as if you were implementing a normal Java service interface. This ensures that all consumer code running in the same JVM can use your implementation directly through the same interfaces that external clients use. This avoids the overhead of HTTP requests and JSON (de)serialization for internal consumers.

Having the same interfaces on the server and client sides allows the following approach when designing a larger software system:
- You can start by keeping everything in the same JVM but separating the code into modules that use `@HalApiInterface`s as their internal API from the beginning.
- This encourages your modules to share only data structures and common IDs, not service implementations or other dependencies.
- During development, you can easily expose these internal APIs via HTTP using the **Rhyme** framework (even if your other modules are still using the services directly). This can be helpful, for example, for inspecting data sources in detail without using a debugger.
- You can still refactor everything with full IDE support during development and continuously verify that the API is well-designed.
- When there is an actual reason to break up your system into multiple services, you can do so easily. Since the interfaces for remote access via HAL+API are identical to those for internal consumers, you can keep much of the existing code. 

The [Spring Hypermedia Example](/examples/spring-hypermedia) contains integration tests demonstrating that the example API's behavior is identical for internal and external clients.

### Additional best practices

There are a few more concepts and best practices to follow when implementing your server-side resources. Here is another example to explain what you need to be aware of:

```java
  class ItemResourceImpl implements ItemResource {

    // all dependencies and request parameters required to render the resource will be provided
    // when the instance is created. We are using constructor injection here, but you can also
    // use whatever IoC injection mechanism is available in the framework of your choice.
    private final ItemDatabase database;

    // be aware that 'id' can be null (e.g. if this resource is created/linked to from the entry point)
    private final String id;

    // your constructors should be as lightweight as possible, as an instance of your
    // resource is created even if only a link to the resource is rendered
    ItemResourceImpl(ItemDatabase database, String id) {
      this.database = database;
      this.id = id;
    }

    // any I/O should only happen in the methods that are annotated with @Related
    // or @ResourceState, which are called when the resource is actually rendered
    // (and it's guaranteed that the 'id' parameter is set)
    @Override
    public Item getState() {
      return database.getById(id);
    }

    // to generate links to related resources, you'll simply instantiate their resource
    // implementation classes with the correct 'id' parameter. This also ensures the method
    // is perfectly usable when called directly by an internal consumer.
    @Override
    public Stream<ItemResource> getRelatedItems() {

      return database.getIdsOfItemsRelatedTo(id).stream()
          .map(relatedId -> new ItemResourceImpl(database, relatedId));
    }

    @Override
    public Link createLink() {

      // this method is the sole location where all links to this resource are rendered.
      // This includes generating link templates (e.g., when 'id' is null).
      UriTemplate uriTemplate = UriTemplate.fromTemplate("https://hal-api.example.org/items/{id}");
      if (id != null) {
        uriTemplate.set("id", id);
      }
      Link link = new Link(uriTemplate.expandPartial());

      if (id != null) {
        // it's good practice to always provide a human-readable 'title' attribute for the link,
        // as this will appear in tools such as the HAL browser.
        link.setTitle("The item with id '" + id + "'");
        // For machines, you should also always set a 'name' attribute to distinguish
        // multiple links with the same relations.
        link.setName(id);
      }
      else {
        // Link templates, especially, should always have a good description in the title, as these
        // are likely to appear in your resource's entry point and will help make
        // your API self-explanatory.
        link.setTitle("A link template to retrieve the item with the specified id from the database");
      }

      return link;
    }
  }

```

As you can see, link generation is quite complex even for this simple example. This is because the `#createLink()` method of a resource implementation is responsible for rendering **all** possible variations of links and link templates to this kind of resource. The benefit of this approach is that the link generation code is not scattered throughout your project. Instead, it can all be found in the same class that will use the parameters encoded in the links.

To keep your individual resource implementations simple, you will likely end up with something like a service-specific `LinkBuilder` class to avoid duplicating code and URLs. Since the best way to create links varies significantly depending on the web framework you are using, the core **Rhyme** framework does not attempt to provide or enforce a solution for this.

The [Spring integration module](/integration/spring), however, does have additional classes to simplify link building, including support for URL fingerprinting, as explained below.

## Writing Integration Tests

One main goal of the `Rhyme` framework is to enable writing extensive integration tests for your application that are easy to read and are written entirely from an external client's perspective.

Since clients shouldn't make assumptions about the full URL structures of an API's resources, your integration tests shouldn't either. So, instead of manually creating URLs to your resources, fetching them, and verifying that related link URLs are constructed correctly, the tests should check if **following** the links (and expanding link templates where necessary) according to HAL specifications actually leads to the expected resource. This ensures that changes to the URL structure (which you can make at any time without breaking API compatibility) don't require adjustments to your tests.

The [integration test](/examples/spring-hello-world/src/test/java/io/wcm/caravan/rhyme/examples/spring/helloworld/HelloWorldTest.java) for the [Spring Hello World example](/examples/spring-hello-world) shows how only a single URL is constructed manually (the entry point URL running on a random port). All other assertions are based on following links by calling methods on the dynamic proxies created by the `HalApiClient` instance.

Using `Rhyme` for integration tests of an existing API implementation is probably the best way to get comfortable with Rhyme's concepts. You can start by modeling your API as `HalApiInterface`s (beginning from the entry point) and then add additional tests and resource interfaces as required. It's a good way to test if your API works well with a client library that has certain expectations, which may differ from how you are using the API.

If you notice behavior where you think the `HalApiClient` is not acting according to the specification or common conventions, please open an issue (or, of course, a PR if you've narrowed down the problem yourself). The same applies if there are gaps that prevent you from fully representing your API as annotated `HalApiInterface`s.


## Embedded Resources

If you have many small resources in your API, retrieving each individually will incur additional overhead. To avoid this, the HAL+JSON format specifies a way to embed resources (in addition to linking them) within a context resource.

Embedding resources with **Rhyme** is straightforward: Simply make your server-side resource implementation classes also implement the [EmbeddableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/EmbeddableResource.java) interface.

If you don't override any of the interface's default methods, the `Rhyme#renderResponse` method will automatically embed a complete representation of your resource wherever it is linked (with a fully resolved URI, not for templated links). You can also override `EmbeddableResource#isEmbedded` to have more control over the process and decide, based on context or configuration, whether embedding a resource is beneficial to performance.

Proxy client implementations created with the **Rhyme** framework will also always check for embedded resources when a method annotated with `@Related` is called. If they find embedded resources for that relation, those will be used instead of following the corresponding links.

You can also create a nested hierarchy of embedded resources. This may be useful, for example, if you have a deep hierarchy tree of objects where the leaf objects require links to other resources. In such cases, it may not always be reasonable to have a unique URI to access each resource in the tree individually. To achieve this, simply make your server-side resources implement **only** `EmbeddedResource` (but not `LinkableResource`).

## Generation and Integration of HTML API documentation

Since the interfaces annotated with `@HalApiInterface` define the structure and relations of all resources in the API, they can also be used to generate user-friendly, context-dependent documentation for your API. Using `curies` links, this documentation can be automatically integrated into tools such as the [HAL Browser](https://github.com/mikekelly/hal-browser) or [HAL Explorer](https://github.com/toedter/hal-explorer).

All you need to do is configure the [Rhyme Maven Documentation Plugin](/tooling/docs-maven-plugin) in your project and add Javadoc comments to your annotated interfaces and methods. The generated documentation will be deployed (and served) with your application and is always guaranteed to be up-to-date with the current implementation in each environment.

## Cache-Control Header and URL Fingerprinting

The `cache-control: max-age` header is arguably the most useful way to control caching in a system of distributed, stateless web services. It doesn't require clients to keep track of last-modified dates or ETags, and it has good support in CDNs, browsers, and caching proxies.

### Controlling the 'max-age' directive on the server

Within your server-side implementation, you can simply call `Rhyme#setResponseMaxAge(Duration)` at any time to set this cache header in the response. If you call it multiple times, the lowest duration will be used.

If you are building a service that also fetches HAL+JSON responses from other services (which is the main use case for **Rhyme**), the `max-age` headers from these upstream responses should also be considered: If any of those responses are only to be cached for a short time, the derived response you are creating must also not be cached any longer. Otherwise, you'll encounter issues where changes to these upstream resources won't become effective for your consumers. This will all happen automatically if you ensure you reuse the same `Rhyme` instance to fetch upstream resources and render your own response.

### Enabling client-side caching

To enable caching for your upstream requests executed with `Rhyme` or `HalApiClient`, you need to use the [HalResourceLoaderBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/client/HalResourceLoaderBuilder.java) and call either `#withMemoryCache` or `#withCustomCache` to create a single instance of a caching `HalResourceLoader` implementation. This instance should then be shared and reused across your application by passing it to the `RhymeBuilder#withResourceLoader` or `HalApiClient#create` methods.

Any response retrieved by the **Rhyme** framework will then be stored in this cache, and subsequent requests to the same URL will use the cached response instance. Again, the `max-age` cache directive will be taken into account:

- If the response in the cache is older than the `max-age` value, it is considered stale and will not be used. A fresh copy will be loaded (and again stored in the cache).
- The `max-age` value of responses taken from the cache will be updated automatically: if an upstream response required by your resource had specified a max-age of 60 seconds but was already requested and cached 55 seconds ago, calling `HalResponse#getMaxAge` on the cached instance will return 5 seconds.
- If you are using this cached response in a service that also renders HAL+JSON resources with Rhyme, loading this cached response with the adjusted max-age will automatically reduce your own response's max-age: Since your response depends on data that is about to go stale in 5 seconds, your consumers shouldn't cache your response for longer than that either.

By default, only responses with a status code of 200 will be cached, and a default max-age of 60 seconds will be used if no such directive is found in the upstream response headers. You can override these defaults by providing your own implementation of [CachingConfiguration](core/src/main/java/io/wcm/caravan/rhyme/api/client/CachingConfiguration.java) to `HalResourceLoaderBuilder#withCachingConfiguration`.

### Using immutable resources and URL fingerprinting

For all this to work best, you should build your API with the following pattern:
- For the entry point resource to your HAL API, set a short max-age value via `Rhyme#setResponseMaxAge(Duration)`.
- Ensure that the entry point resource renders quickly, as it will be requested often (since any interaction with your API starts with requesting the entry point).
- Any links pointing to resources that are expensive to generate should contain some kind of fingerprint (e.g., a hash or timestamp) in the URL that changes whenever the data changes.
- When a resource with such a fingerprint in its URL is rendered, you can set the max-age to a very high value, as they are now essentially immutable (because if data changes, clients will fetch them with a different URL instead).
- Consumers will now automatically "poll" the entry point repeatedly. But as long as the data (and therefore the URLs) don't change, they will continue to use the same fingerprinted URLs to fetch the more expensive resources (and there is a high chance that these can be found in the cache).

Your consumers will not have to do anything to benefit from these immutable resources, as the additional fingerprinting in your URLs is not exposed anywhere in your API. It's entirely up to the server-side implementation to decide for which links these fingerprints are added, and clients will just pick them up by following the links.

The [SpringRhyme](integration/spring/src/main/java/io/wcm/caravan/rhyme/spring/api/SpringRhyme.java) integration has built-in support for this via the [UrlFingerprinting](integration/spring/src/main/java/io/wcm/caravan/rhyme/spring/api/UrlFingerprinting.java) interface. To see it in action, check out the [examples/spring-hypermedia](examples/spring-hypermedia) module.

## Data debugging and performance analysis

There is another benefit to reusing the same `Rhyme` instance while handling an incoming request: Every upstream resource fetched and every annotated method called by the framework will be tracked by the `Rhyme` instance.

When a HAL response is rendered, it can include a small embedded resource (using the `rhyme:metadata` relation) that allows you to inspect what exactly the framework did to generate the response. This is disabled by default, but for existing integration modules (e.g., Spring, OSGi/JAX-RS), you can add an `embedRhymeMetadata` query parameter to any request to include this embedded resource.

This resource will contain the following information:
- A list of `via` links to every HAL resource fetched from an upstream service. The `title` and `name` attributes from the original `self` links of those resources will be included, giving you a clear overview of which external resources were retrieved to generate your resource. This is extremely helpful for diving directly into the relevant source data of your upstream services.
- A sorted list of the measured response times for each of those upstream resources. This allows you to identify which upstream service might be slowing down your system.
- A sorted list of the `max-age` headers for each of those upstream resources. This allows you to identify why your own response's `max-age` might be lower than expected.
- Extensive statistics about the time spent in method calls to your resource implementation classes or the dynamic client proxies provided by the framework: 

```json
{
  "measurements": [
    "6.774 ms - sum of 50x calling #createLink of DelayableItemResourceImpl",
    "0.954 ms - sum of 2x calling #createLink of DelayableCollectionResourceImpl",
    "0.307 ms - 1x calling #renderLinkedOrEmbeddedResource with DelayableCollectionResourceImpl",
    "0.026 ms - 1x calling #getItems of DelayableCollectionResourceImpl",
    "0.024 ms - 1x calling #getState of DelayableCollectionResourceImpl",
    "0.018 ms - 1x calling #getAlternate of DelayableCollectionResourceImpl"
  ],
  "title": "A breakdown of time spent in blocking method calls by AsyncHalResourceRenderer"
}
```

While the overhead of using the **Rhyme** framework is usually negligible (especially compared to the latency introduced by external services), this information can be useful for identifying hotspots that can be optimized (without using a profiler).

If you have a section of your own code that you suspect is a performance hotspot, you can easily add your own metrics to the response metadata:

```
  class YourClass {

    private void doExpensiveStuffWith(Object param) {

      try (RequestMetricsStopwatch sw = rhyme.startStopwatch(YourClass.class, () -> "calls to #doExpensiveStuffWith(" + param + ")")) {
        // ... actually do expensive stuff with param
      }
    }
  }
```

This will make the **Rhyme** instance count the number of calls to your method and sum up the overall execution time, so you can check if it's worth optimizing this bit of code (or avoiding repeated calls).

The advantage over using an external profiler is that execution times for the same section of code with different parameters can be distinguished, and only execution times from the current request are considered.

Check out the [AWS Lambda Example](examples/aws-movie-search) where this metadata, showing details about the upstream requests, is included by default.

## Forwarding error information across service boundaries

Any runtime exceptions thrown by your implementation classes (or any **Rhyme** framework code) during the execution of `Rhyme#renderResponse` will be caught and handled. Instead of the regular HAL+JSON response with a 200 status code, the `renderResponse` method will render a response with an appropriate status code and a JSON body according to the [vnd.error+json](https://github.com/blongden/vnd.error) media type. This media type is a very simple convention for representing error information in a HAL+JSON compatible format and will include the exception classes and messages of the entire exception chain.

The **status code** will be determined as follows:
- Any errors while retrieving an upstream resource should lead to a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java). The original status code can be extracted from this exception and is, by default, also used in your service's response.
- This works well in many cases, but you can also catch those exceptions yourself (in your resource implementations) and either return suitable fallback content or re-throw a [HalApiServerException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiServerException.java) with a different status code.
- The Rhyme framework classes may also throw a [HalApiDeveloperException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiDeveloperException.java), which usually indicates a developer error (and hopefully has a clear explanation; otherwise, please open an issue). In this case, a 500 status code is used.
- When you use the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java) to create your `Rhyme` instance, you may also register an [ExceptionStatusAndLoggingStrategy](core/src/main/java/io/wcm/caravan/rhyme/api/spi/ExceptionStatusAndLoggingStrategy.java)) that can extract a suitable status code for any other exception (e.g., an exception used by your web framework).
- Any other runtime exceptions will lead to a 500 status code.

Some exceptions may also be thrown **before** you call `Rhyme#renderResponse`. You should try to catch those as well and call `Rhyme#renderVndErrorResponse(Throwable)` yourself to create a vnd.error response.

The benefit of rendering this vnd.error response body for any error is that clients using the `Rhyme` framework can parse this body and include the error information in the [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java). If an upstream service fails, your service will render not only the information from the caught exception but also all information extracted from the vnd.error response of the upstream service. Even if you have multiple layers of remote services, your vnd.error response will have a neat list of all exception classes and messages, up to the original root cause on the remote server. This may sound trivial but is very helpful for immediately understanding what went wrong without having to look into the logs of several external services.

The embedded `rhyme:metadata` resource, with `via` links to all upstream resources loaded until the error occurred, is also present in the vnd.error resource. This can be useful for investigating the root cause, as runtime exceptions are often caused by unexpected data in an upstream service.

## Using reactive types in your API

Using asynchronous code to retrieve and render HAL resources can be desirable if you have to deal with many long-running requests. It will allow you to execute upstream requests in parallel without blocking your main request handling thread. However, don't underestimate the increased complexity that comes with it.

If you want to keep your client and server-side code completely asynchronous and non-blocking, you can start by using RxJava 3 reactive types as return values throughout your API interfaces: 

```java
  @HalApiInterface
  public interface ReactiveResource extends LinkableResource {

    @ResourceState
    Single<Item> getState();

    @Related("related")
    Observable<ReactiveResource> getRelatedItems();

    @Related("parent")
    Maybe<ReactiveResource> getParentItem();
  }
```
- `Single<T>` is used (instead of `T`) whenever it's guaranteed that exactly one value is emitted.
- `Observable<T>` is used (instead of `Stream<T>`) whenever multiple values can be emitted.
- `Maybe<T>` is used (instead of `Optional<T>`) when a value may or may not be present.

If you would rather use [Spring Reactor](https://projectreactor.io/) types (or types from other RxJava versions), you can add support for that through the [HalApiReturnTypeSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalApiReturnTypeSupport.java) SPI. You'll just need to implement a couple of functions that convert the additional types to and from RxJava 3's `Observable`. You can register your return type extension before you create a `Rhyme` instance with the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java).

On the client side, you'll have to implement [HalResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalResourceLoader.java) using a fully asynchronous HTTP client library.

Then you can use the full range of RxJava operators to construct a chain of API operations that are all executed lazily and asynchronously (and in parallel where possible):

```java
    ReactiveResource resource = rhyme.getRemoteResource("https://foo.bar", ReactiveResource.class);

    Single<List<Item>> parentsOfRelated = resource.getRelatedItems()
        .concatMapMaybe(ReactiveResource::getParentItem)
        .concatMapSingle(ReactiveResource::getState)
        .distinct(item -> item.id)
        .toList();

    // All Observables provided by Rhyme are *cold*, i.e. no HTTP requests would have been executed so far.
    // But they all will be executed (in parallel where possible) when you subscribe to the Single:

    List<Item> parentItems = parentsOfRelated.blockingGet();
```

On the server-side, just use the `renderResponse` function from the `Rhyme` interface, without calling `Single#blockingGet()`, to ensure that your code is not blocking the main thread while rendering the HAL representation of your server-side resource implementations:

```java
Single<HalResponse> response = rhyme.renderResponse(resource);
```

All implementation methods of your resource will be called and should return a `Maybe`, `Single`, or `Observable` immediately.
This is the "assembly" phase of the rendering where the main thread is blocked.

As soon as you subscribe to the response `Single`, all other reactive instances returned by the resource implementation will be subscribed to and start their actual workload (e.g., requesting upstream resources). Then, when all of these objects have emitted their results, the `HalResponse` will finally be constructed and emitted by the `Single`. 

# Related Links

Issues: https://github.com/wcm-io-caravan/caravan-rhyme/issues<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-rhyme/actions<br/>
Commercial support: https://wcm.io/commercial-support.html
