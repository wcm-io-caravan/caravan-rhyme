<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme
======
[![Build](https://github.com/wcm-io-caravan/caravan-rhyme/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-rhyme/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Code Coverage](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme/branch/develop/graph/badge.svg)](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme)

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

# Introduction

**Rhyme** is a Java framework for providing or consuming hypermedia APIs using the [HAL+JSON media format](http://stateless.co/hal_specification.html). It really shines when you need to do both, e.g. build a distributed system of web services that are connected through several HAL APIs.

**Rhyme** stands for **R**eactive **Hy**per**me**dia, as it fully supports asynchronous generation and retrieval of HAL+JSON resources (using [RxJava 3](https://github.com/ReactiveX/RxJava) internally). Using reactive types however is (almost) entirely optional. In this document, we'll mostly stick to using the simpler blocking code examples, but there is a section that explains how reactive types can be used.

The key concepts and features of **Rhyme** are:
- HAL APIs are represented as type-safe **annotated Java interfaces**.
- These interfaces are shared with the consumers, which can use them as a **highly abstracted client API**.
- The same interfaces are also used to **keep the server-side implementation well structured**, and always in sync with the published API.
- Consistent support for **controlling caching** using the `cache-control: max-age` header
- Simplify **data debugging and performance analysis** (by including embedded metadata in every response)
- **Consistent error handling over service boundaries** using the [vnd.error](https://github.com/blongden/vnd.error) media type
- **Supporting asynchronous, reactive programming** on the client and server side

**Rhyme** is based on several years of experience and best practices from a large production HAL microservice platform (based on OSGi, JAX-RS & RxJava 1). But it has been re-written from scratch (with an unreasonably high unit test coverage) with the goal of being useful within any Java web framework. 

## Modules in this repository

- [api-interfaces](api-interfaces) - contains only annotations, interfaces and dependencies to be used in your API interface definitions
- [core](core) - the core framework that can be integrated within any Java web service
- [osgi-jaxrs](osgi-jaxrs) - additional code for implementing HAL web services using the [OSGi R7 JAX-RS Whiteboard](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html) and related [wcm.io Caravan](https://github.com/wcm-io-caravan) projects
- [examples/osgi-jaxrs-example-service](examples/osgi-jaxrs-example-service) - an example service using reactive types in its API
- [examples/osgi-jaxrs-example-launchpad](examples/osgi-jaxrs-example-launchpad) - a [Sling launchpad](https://sling.apache.org/documentation/the-sling-engine/the-sling-launchpad.html) to start the example service (and run some integration tests)

Another example for usage with Spring Boot can be found at https://github.com/feffef/reactive-hal-spring-example.

# Key concepts explained

## Define a HAL API with annotated interfaces

As an example, here is the HAL+JSON entry point resource of a simple web service that provides access to a database of simple generic items:

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
- [@HalApiInterface](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/HalApiInterface.java) is just a marker annotation that helps the framework identify the relevant interfaces through reflection.
- By extending [LinkableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/LinkableResource.java) we define that this resource is directly accessible through a URL (which will be found in the `self` link).
- The `getItemById` function corresponds to the link template with `item` relation, and the parameter annotated with [@TemplateVariable](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/TemplateVariable.java) indicates that you must provide an `id` parameter. It will be used to expand the link template into the final URL that will be used to retrieve the item resource.
- A HAL API should allow to discover all available data whenever possible. That's why there is also a `getFirstPage` function which allows you to start browsing all available items using the `first` link.

The return type of these functions are again annotated java interfaces. They describe the structure and available relations of the linked resources:

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
- Note that none of these interfaces define anything regarding the URL structure of these resources. This matches the [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) principle that clients should only need to know a single URL (of the entry point). All other URLs should be discoverable through links in the resources.
- Methods annotated with [@Related](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) are used to define all possible relations / links between resources.
- `Stream` is used as return type whenever where there may be multiple links with the same relation. If you don't like Streams you can use `List` instead.
- `Optional` is used when it is not guaranteed that a link will be present (e.g. on the last page, there will be no `next` link).
- The method annotated with [@ResourceState](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) finally returns the actual data structure containing the core properties of an item.

As return type for the @ResourceState method, you could either use a [jackson](https://github.com/FasterXML/jackson) `ObjectNode` or any other type that can be  parsed from and serialized to JSON using the default jackson `ObjectMapper`. Using generic JSON types in your API is preferred if you are forwarding JSON resources from an external source, and those JSON resources' structure is expected to be extended frequently.

If you want to provide a strongly **typed** API to your consumers, you should define simple classes that match the JSON structure of your resources' state. You shouldn't share any **code** with theses classes, so a simple struct-like class like this works well:

```java
  public class Item {

    public String id;
    public String title;
  }
```

In the end, an actual HAL resource that matches the `ItemResource` interface defined above would look like this:

```javascript
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

Hardcore *RESTafarians* may say that everything you have read in this section is a bad idea, as it's all about sharing out-of-band information about your API with your clients. However any consumer needs to have some reasonable expectations about the available links and data structures provided by your API to create reliable client code. The aim of those annotated interfaces is to specify exactly these kind of guarantees given by the API in a very concise way, that can directly be used in client-side code. At the same time, the interfaces don't expose too many implementation details (such as URL structures). 

If you don't like the idea of sharing these interfaces with consumers (as outlined in the next section), then keep in mind that this is entirely optional. Your API will still be using plain HAL+JSON data structures, and nothing forces you to use the **Rhyme** framework and these interfaces on *both* sides.

But especially as long as you (or your team) are the sole consumers of your API anyway, sharing these interfaces between your services will give you many benefits:
- refactoring of your API (e.g. renaming relations) throughout a distributed system is very easy and reliable
- your IDE is able to understand who is actually using a specific API method
- your IDE is able to find the server-side implementation(s) of every API method

## Consuming HAL resources with Rhyme client proxies

Now that you have a set of interfaces that represent your HAL API, you can use the Rhyme framework to automatically create a client implementation of those interfaces. This is similar to the concepts of [Feign](https://github.com/OpenFeign/feign) or [retrofit](https://github.com/square/retrofit), but much better suited to the HAL concepts (as for example no URL patterns are being exposed in the interfaces).

To be able to retrieve HAL+JSON resources through HTTP you must first create an implementation of the [JsonResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/JsonResourceLoader.java) SPI interface. This is intentionally out of scope of the core framework, as the choice of HTTP client library should be entirely up to you.

The interface however just consists of a single method that will load a HAL resource from a given URL, and emit a [HalResponse](core/src/main/java/io/wcm/caravan/rhyme/api/common/HalResponse.java) object when it has been retrieved (or fail with a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java) if this wasn't possible)

```java
Single<HalResponse> loadJsonResource(String uri);
```

Once you have a `JsonResourceLoader` instance, it just requires a few lines of code to create a client implementation of your HAL API's entry point interface: 

```java
  private ApiEntryPoint getApiEntryPoint() {

    // create a Rhyme instance that knows how to load any external JSON resource
    Rhyme rhyme = RhymeBuilder.withResourceLoader(jsonLoader)
        .buildForRequestTo(incomingRequest.getUrl());

    // create a dynamic proxy that knows how to fetch the entry point from the given URL
    return rhyme.getUpstreamEntryPoint("https://hal-api.example.org", ApiEntryPoint.class);
  }
```

Using that proxy instance of your entry point you can easily navigate through all resources of the API by simply calling the methods defined in your interfaces: 
```java
    // obtaining a client proxy will not fetch the entry point resource yet (until you call a method on it)
    ApiEntryPoint api = getApiEntryPoint();
    
    // calling the method will fetch the entry point, then find and expand the URI template.
    ItemResource itemResource = api.getItemById("foo");
    
    // now you have a ItemResource that knows the full URL of the resource (and how to fetch it),
    // but again that resource is only actually fetched when you call a method on the resource
    Item foo = itemResource.getState();

    // You can call another method on the same instance (without any resource being fetched twice),
    // and use stream operations to fetch multiple resources with a simple expression.
    List<Item> relatedToFoo = itemResource.getRelatedItems()
        .map(ItemResource::getState)
        .collect(Collectors.toList());
```
The proxy instance will take care of
- fetching and parsing the resource
- finding the links (or embedded resources) that correspond to the method being called (based on the relations defined in the interface methods' annotations)
- expanding link templates with the parameters from the method invocation
- fetching further linked resources as required
- keeping track of all resources that have been retrieved
 
A local in-memory caching will ensure that the same resources are not fetched more than once (as long as you are using the same Rhyme instance).

## Rendering HAL resources in your web service 

For the server-side implementation of your HAL API, you will have to implement the annotated API interfaces you've defined before. You can then use the [Rhyme](core/src/main/java/io/wcm/caravan/rhyme/api/Rhyme.java) facade to automatically render a HAL+JSON representation based on the annotation found in the interfaces.

What's important to note is that **you should only create a single `Rhyme` instance** in the life-cycle of an incoming request. 

```java
    // create a single Rhyme instance as early as possible in the request-cycle 
    Rhyme rhyme = RhymeBuilder.withoutResourceLoader().buildForRequestTo(incomingRequest.getUrl());
    
    // instantiate your server-side implementation of the requested @HalApiInterface resource
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);
    
    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint);

    // finally convert that response to your framework's representation of a web/JSON response...
```

What `Rhyme#renderResponse` does is to scan your implementation class for methods from the annotated `@HalInterface` and **recursively** call all those methods:

- `#createLink()` is called to generate the `self` link directly
- `#getFirstPage()` is called to create a PageResource instance, and then `PageResource#createLink()` is called to create the link to it
- `#getItemById()` is called (with the `id` parameter being null, as the entry point should only contain a link template and no specific id is known yet), and then again `ItemResource#createLink()` is called on the implementation instance being returned (to actually create the link template)

Here's what happens in the implementation class:

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


Note that the implementation of the `@Related` methods look exactly the same as if you were implementing a normal service interface. This ensures that all consumer code running in the same JVM could use your implementation directly through the same interfaces that external clients are using. That would avoid the overhead of http requests and JSON (de)serialisation. 

Having the same interfaces on the server- and client-side allows the following approach when designing a larger software system:
- You can start with keeping everything in the same JVM, but separate the code into modules that are using `@HalApiInterface`s as internal API from the beginning.
- During development you can easily expose these internal APIs through HTTP using the **Rhyme** framework (even though your other modules are still using the implementation classes). This can be very helpful for inspecting data sources without using a debugger.
- You can still refactor everything easily during development, and continously verify that the API is designed well.
- When there is an actual reason to break up your system into multiple services, you can easily do so. As the interfaces for remote access via HAL+API are exactly the same as for the server-side implementation, you can keep much of the existing code. 

There are a few more **best practices** to keep in mind when implementing your server-side resources. Here is another example that explains the most important things that you need to be aware of:

```java
  class ItemResourceImpl implements ItemResource {

    // all dependencies and request parameters required to render the resource will be provided
    // when the instance is created. We are using constructor injection here, but you can also
    // use whatever IoC injection mechanism is available in the framework of your choice.
    private final ItemDatabase database;

    // be aware that 'id' can be null (e.g. if this resource is created/linked to from the entry point)
    private final String id;

    // your constructors should be as leight-weight as possible, as an instance of your
    // resource is created even if only a link to the resource is rendered
    ItemResourceImpl(ItemDatabase database, String id) {
      this.database = database;
      this.id = id;
    }

    // any I/O should only happen in the methods that are annotated with @Related
    // or @ResourceState which are being called when the resource is actually rendered
    // (and it's guaranteed that the 'id' parameter is set)
    @Override
    public Item getState() {
      return database.getById(id);
    }

    // to generate links to related resources, you'll simply instantiate their resource
    // implementation classes with the right 'id' parameter. This also ensures the method
    // is perfectly usable when called directly by an internal consumer.
    @Override
    public Stream<ItemResource> getRelatedItems() {

      return database.getIdsOfItemsRelatedTo(id).stream()
          .map(relatedId -> new ItemResourceImpl(database, relatedId));
    }

    @Override
    public Link createLink() {

      // this method is the one and only location where all links to this resource are rendered.
      // This includes the generation of link templates (e.g. when 'id' is null)
      UriTemplate uriTemplate = UriTemplate.fromTemplate("https://hal-api.example.org/items/{id}");
      if (id != null) {
        uriTemplate.set("id", id);
      }
      Link link = new Link(uriTemplate.expandPartial());

      if (id != null) {
        // it's good practice to always provide a human readable 'title' attribute for the link,
        // as this will appear in tools such as the HAL browser
        link.setTitle("The item with id '" + id + "'");
        // for machines, you should also always set a 'name' attribute to distinguish
        // multiple links with the same relations
        link.setName(id);
      }
      else {
        // especially link templates should always have a good description in title, as these
        // are likely to appear in the entry point of your resource, and will help to make
        // your API self-explaining
        link.setTitle("A link template to retrieve the item with the specified id from the database");
      }

      return link;
    }
  }

```

One thing you should have noticed is that link generation is quite complex even for this simple example. This is due to the fact that the `#createLink()` method of a resource implementation is responsible to render **all** possible variations of links and link templates to this kind of resource. The benefit of this approach is that the link generation code is not cluttered all over your project. Instead it can all be found in exactly the same class that will be using the parameters encoded in the links.

To keep your resource implementations simple, you are likely to end up with something like a project-specific `LinkBuilder` class to avoid duplication of code and URLs. Since the best way to create links varies a lot depending on the web framework your are using, the core **Rhyme** framework does not try to provide or enforce a solution for this (but you will find some ideas and concepts in the examples).

## Controlling caching

The `cache-control: max-age` header is probably the most useful way of controlling caching in a system of distributed stateless web services. It does not require clients to keep track of last-modified dates or Etags, and there is also good support for it in CDNs, browsers and caching proxies.

Within your server-side implementation, you can call `Rhyme#setResponseMaxAge(Duration)` at any time to set this cache header in the respose. If you call it multiple times, the lowest duration will be used. 

If you are building a service that is also fetching HAL+JSON responses from other services (which is the main use case for **Rhyme**), the `max-age` headers from these upstream responses should also be taken into account: If any of those responses are only to be cached for a short time, the derived response that you are creating must also not be cached any longer than that. Otherwise you'll run into issues that changes to these upstream resources won't become effective for your consumers. This will all happen automatically if you make sure to re-use the same `Rhyme` instance to fetch upstream resources and render your own response.

The **Rhyme** core framework does not implement any client-side caching layer itself. If you need such a cache layer, it can be added to your [JsonResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/JsonResourceLoader.java) implementation. But you should make sure to respect and update the max-age information from the Hal Response being cached.

## Data debugging and performance analysis

There is another benefit of re-using the same `Rhyme` instance while handling an incoming request: Every upstream resource that is fetched, and every annotated method called by the framework will be tracked by the Rhyme instance.

When a HAL response is rendered, it will include a small embedded resource (using the `caravan:metadata` relation) that allows you to inspect what exactly the framework did to generate the response.

This resource will contain the following information:
- a list of `via` links to every HAL resource that was fetched from an upstream service. The `title` and `name` attributes from the original `self` links of those resources will be included as well, giving you a very nice overview which external resources were retrieved to generate your resource. This is super helpful to dive directly into the relevant source data of your upstream services.
- a sorted list of the measured response times for each of those upstream resources. This allows you to identify which upstream service may be slowing down your system
- a sorted list of the `max-age`headers for each of those upstream resources. This allows you to identify the reason why your own response's `max-age` may be lower as expected
- some extensive statistics about the time spent in method calls to your resource implementation classes, or the dynamic client proxies provided by the framework: 

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

While the overhead of using the **Rhyme** framework is usually neglible (especially compared to the latency introduced by external services), this information can be useful to identify hotspots that can be optimized (without firing up a profiler).

## Consistent error handling over service boundaries

Any runtime errors that are thrown by your implementation classes (or any Rhyme framework code) during the execution of `Rhyme#renderResponse` will be caught and handled. Instead of the regular HAL+JSON response with 200 status code, the `renderResponse` method will render a response with an appropriate status code, and a JSON body according to the [vnd.error+json](https://github.com/blongden/vnd.error) media type. This media type is just a very simple convention how error information is represented in a HAL+JSON resource, and will include the exception class and message of the whole exception chain.

The status code will be determined as follows:
- any errors while retrieving an upstream resource should lead to a [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java) from which the original status code can be extracted and by default is also used in your service's response
- this works well in many cases, but you may of course also catch those exception yourself (in your resource implementations), and either return some suitable fallback content or re-throw a [HalApiServerException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiServerException.java) with a different status code
- the Rhyme framework classes may also throw a [HalApiDeveloperException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiDeveloperException.java) that usually indicates that you did something wrong (and hopefully have a clear explanation, otherwise please open an issue). In this case a 500 status code is used.
- when you use the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java) to create your `Rhyme` instance you may also register a [ExceptionStatusAndLoggingStrategy](develop/core/src/main/java/io/wcm/caravan/rhyme/api/spi/ExceptionStatusAndLoggingStrategy.java)) that can extract a suitable status code for any other exception (e.g. an exception used by your web framework)
- any other runtime exceptions will lead to a 500 status code

Some exceptions may also be thrown **before** you are calling `Rhyme#renderResponse`. You should try to catch those as well and call `Rhyme#renderVndErrorResponse(Throwable)` yourself to create a vnd.error response yourself.

The benefit of rendering this vnd.error response body for any error is that any clients using the `Rhyme` framework will parse this body, and include the error information in the [HalApiClientException](core/src/main/java/io/wcm/caravan/rhyme/api/exceptions/HalApiClientException.java). If an upstream service fails, your service will not only render the chain of exceptions that has been caught, but also the error information extracted from the vnd.error response of the upstream service. Even if you have multiple layers of remote services, your vnd.error response will have a neat list of all exception classes and messages, up to the original root cause on the remote server. This may sound trivial, but is very helpful to immediately understand what went wrong, without having to look into the logs of several external services.

The vnd.error resource also always contains a direct link to the resource that failed to load, which can be convenient to reproduce the error.

The embedded `caravan:metadata` resource with `via` links to all upstream resources is also present in the vnd.error resource. This can be useful to investigate the error as often a runtime error is caused by unexpected data in an upstream service.

## Using reactive types in your API

Using asynchronous code to retrieve and render HAL resources can be desired if you have to deal with a lot of long-running requests. It will allow you to execute upstream requests in parallel without blocking your main request handling thread. But don't underestimate the increased complexity that comes with it.

If you want to keep your client and server-side code completely asynchronous and non-blocking, you start with using RxJava reactive types as return values throughout your API interfaces: 

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

- `Single<T>` is used (instead of `T`) whenever it's guaranteed that exactly one value is emitted
- `Observable<T>` is used (instead of `Stream<T>`) whenever multiple values can be emitted
- `Maybe<T>` is used (instead of `Optional<T>`) when a value may be present or not

If you rather want to use [Spring Reactor](https://projectreactor.io/) types (or types from othe RxJava versions), you can add support for that through the [HalApiReturnTypeSupport](core/src/main/java/io/wcm/caravan/rhyme/api/spi/HalApiReturnTypeSupport.java) SPI. You'll just need to implement a couple of functions that convert the additional types to/from RxJava3's `Observable`. You can register your return type extension before you create a `Rhyme` instance with the [RhymeBuilder](core/src/main/java/io/wcm/caravan/rhyme/api/RhymeBuilder.java).

On the client side, you'll have to implement [JsonResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/JsonResourceLoader.java) using a fully asynchronous HTTP client library. 

Then you can use the full range of RxJava operators to construct a chain of API operations that are all executed lazily and asynchronously (and in parallel where possible):

```java
    ReactiveResource resource = rhyme.getUpstreamEntryPoint("https://foo.bar", ReactiveResource.class);

    Observable<Item> parentsOfRelated = resource.getRelatedItems()
        .concatMapEager(ReactiveResource::getRelatedItems)
        .concatMapMaybe(ReactiveResource::getParentItem)
        .concatMapSingle(ReactiveResource::getState)
        .distinct(item -> item.id);
    
    // all Observable provided by rhyme are *cold*, i.e. no HTTP requests would have been executed so far.
```

On the server-side, just use the `renderResponseAsync` function from the `Rhyme` interface to ensure that your code is not blocking the main thread while rendering the HAL representation of your server-side resource implementations:

```java
CompletionStage<HalResponse> response = rhyme.renderResponseAsync(resource);
```

# Related Links

Issues: https://wcm-io.atlassian.net/projects/WCARAV/<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-rhyme/actions<br/>
Commercial support: https://wcm.io/commercial-support.html


# Build from sources

If you want to build wcm.io Caravan Rhyme from sources make sure you have configured all [Maven Repositories](https://caravan.wcm.io/maven.html) in your settings.xml.

See [Maven Settings](https://github.com/wcm-io-caravan/caravan-rhyme/blob/develop/.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
