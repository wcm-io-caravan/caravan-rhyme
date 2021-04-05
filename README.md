<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Rhyme
======
[![Build](https://github.com/wcm-io-caravan/caravan-rhyme/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-rhyme/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Code Coverage](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme/branch/develop/graph/badge.svg)](https://codecov.io/gh/wcm-io-caravan/caravan-rhyme)

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

# Introduction

**Rhyme** is a Java framework for providing or consuming RESTful APIs using the [HAL+JSON media format](http://stateless.co/hal_specification.html). It really shines when you need to do both, e.g. build a distributed system of microservices that are connected through several HAL APIs.

**Rhyme** stands for **R**eactive **Hy**per**me**dia, as it fully supports asynchronous generation and retrieval of HAL+JSON resources (using [RxJava 3](https://github.com/ReactiveX/RxJava) internally). Other reactive libraries (e.g. [Spring Reactor](https://projectreactor.io/) or RxJava 1 & 2) can be used as well using an extension point. Using reactive types however is (almost) entirely optional, and it's up to you whether you want to use them in your APIs. It will have benefits if you have to deal with a lot of long-running requests that you want to execute in parallel without blocking your main request handling thread. But don't underestimate the increased complexity that comes with it. In the examples in this document, we'll mostly stick to using the simpler blocking code, but there is a section on how reactive types can be used.

The key concepts of Rhyme are
- HAL APIs are represented as type-safe **annotated Java interfaces**
- these interfaces are shared with the consumers, which can use them as a highly abstracted client API
- the same interfaces are also used to keep the server-side implementation well structured, and in sync with the published API

# Key concepts explained

## Define a HAL API with annotated interfaces

As an example, here is the HAL entry point of a simple web service that provides access to a database of simple generic items:

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

Here is how the corresponding Java interface looks like:

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
- by extending [LinkableResource](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/resources/LinkableResource.java) we define that this resource is directly accessible through a URL (which will be found in the `self` link).
- The `getItemById` function corresponds to the link template with `item` relation, and the parameter annotated with [@TemplateVariable](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/TemplateVariable.java) indicates that you must provide an `id` parameter. It will be used to expand the link template into the final URL that will be used to retrieve the item resource.
- A HAL API should allow to discover all available data whenever possible. That's why there is also a `getFirstPage` function which allows you to start browsing all available items using the `first` link.

The return type of these functions are again java interfaces. They describe the structure and available relations of the linked resources:

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
- Note that none of these interfaces define anything regarding the URL structure of these resources. This matches the HAL/HATEOAS principle that client's should only should to know a single URL (of the entry point). All other URLs should be discoverable through links in the resources.
- Again, methods annotated with [@Related](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) are used to model the relations / links between resources.
- `Stream` is used as return type whenever where there may be multiple links with the same relation. If you don't like Streams you can use `List` instead.
- `Optional` is used when it is not guaranteed that a link will be present (e.g. on the last page, there will be no 'next' link).
- The method annotated with [@ResourceState](api-interfaces/src/main/java/io/wcm/caravan/rhyme/api/annotations/ResourceState.java) finally returns the actual data that represents an item.

As a return type for the @ResourceState method, you could either use a [jackson](https://github.com/FasterXML/jackson) `ObjectNode` or any other type that can be  parsed from and serialized to JSON using the default jackson `ObjectMapper`. Using generic JSON types in your API is preferred if you are forwarding JSON resources from an external source, and those JSON resources' structure is expected to be extended frequently.

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

Hardcore RESTafarians may say that everything you have read in this section is a bad idea, as it's all about sharing out-of-band information about your API with your clients. However any consumer needs to have some reasonable expectations about the available links and data structures provided by your API to create reliable client code. The annotated interfaces allow to specify the guarantees given by the API in a very concise way, without exposing too many implementation details (such as URL structures). 

If you don't like the idea of sharing these interfaces with consumers (as outlied in the next section), then keep in mind that this is entirely optional. Your API will still be using plain HAL+JSON data structures, and nothing forces you to use the Rhyme framework and these interfaces on *both* sides.

But especially as long as you (or your team) are the sole consumers of your API anyway, sharing these interfaces will give you many benefits:
- refactoring of your API (e.g. renaming relations) throughout a distributed system is very easy and reliable
- your IDE is able to understand who is actually using a specific API method
- your IDE is able to find the server-side implementation(s) of every API method

## Consuming HAL resources with Rhyme client proxies

Now that you have a set of interfaces that represent your HAL API, you can use the Rhyme framework to automatically create a client implementation of those interfaces. This is similar to the concepts of [Feign](https://github.com/OpenFeign/feign) or [retrofit](https://github.com/square/retrofit), but much better suited to the HAL concepts (as for example no URL patterns are being exposed in the interfaces).

To be able to retrieve HAL+JSON resources through HTTP you must first create an implementation of the [JsonResourceLoader](core/src/main/java/io/wcm/caravan/rhyme/api/spi/JsonResourceLoader.java) SPI interface. This is intentionally out of scope of the core framework, as the choice of HTTP client library should be entirely up to you.

The interface however just consists of a single method that will load a HAL resource from a given URL, and asynchronously emit a [HalResponse](core/src/main/java/io/wcm/caravan/rhyme/api/common/HalResponse.java) object when it has been retrieved.
```java
Single<HalResponse> loadJsonResource(String uri);
```

Once you have a `JsonResourceLoader` instance, it just requires a few lines of code to create a client implementation of your HAL API's entry point interface: 

```java
  private ApiEntryPoint getApiEntryPoint() {

    // create a Rhyme instance that knows how to load any external JSON resource
    Rhyme rhyme = RhymeBuilder.withResourceLoader(jsonLoader)
        .buildForRequestTo(incomingRequest.getUrl());

    // create a dynamic proxy that knows who to fetch the entry point from the given URL
    return rhyme.getUpstreamEntryPoint("https://hal-api.example.org", ApiEntryPoint.class);
  }
```

With that proxy instance you can easily navigate through the resources of the API by simply calling the methods defined in your interfaces: 
```java
    // obtaining a client proxy will not fetch the entry point resource yet (until you call a method on it)
    ApiEntryPoint api = getApiEntryPoint();
    
    // calling the method will fetch the entry point, then find and expand the URI template.
    ItemResource itemResource = api.getItemById("foo");
    
    // now you have a ItemResource that knows the full URL of the resource (and how to fetch it),
    // but again that resource is only actually fetched when you call a method on the resource
    Item foo = itemResource.getState();

    // You can call another method on the same instance without any resource being fetched twice
    List<Item> relatedToFoo = itemResource.getRelatedItems()
        .map(ItemResource::getState)
        .collect(Collectors.toList());
```

## Rendering HAL resources in your web service 

For the server-side implementation of your HAL API, you will have to implement the annotated API interfaces you've defined before. You can then use the [Rhyme](core/src/main/java/io/wcm/caravan/rhyme/api/Rhyme.java) facade to automatically render a HAL+JSON representation based on the annotation found in the interfaces:

```java
    // create a single Rhyme instance as early as possible in the request-cycle 
    Rhyme rhyme = RhymeBuilder.withoutResourceLoader().buildForRequestTo(incomingRequest.getUrl());
    
    // instantiate your server-side implementation of the requested @HalApiInterface resource
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);
    
    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint);

    // finally convert that response to your framework's representation of a web/JSON response...
```

What `Rhyme#renderResponse` does is to scan your implementation class and **recursively** call all the annotated methods from the `@HalApiInterface`:

- `#createLink()` is called to generate the `self` link directly
- `#getFirstPage()` is called to create a PageResource instance, and then `#createLink()` is called on that resource to create the link to it
- `#getItemById()` is called (with the `id` parameter being null, as the entry point should only contain a link template and no specific id is known yet), and then again `#createLink()` is called on the implementation instance being returned (to actually create the link template)

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


Note that the implementation of the `@Related` method looks exactly the same as if you were implementing a normal service interface. This ensures that all consumer code running in the same JVM can use your implementation directly through the same interfaces that external clients are using. This avoids the overhead of http requests and JSON (de)serialisation. 

Having the same interfaces on the server- and client-side allows the following approach when designing a larger software system:
- You can start with keeping everything in the same JVM, but separate the code into modules that are using `@HalApiInterface`s as internal APIs from the beginning
- During development you can easily expose these internal APIs through HTTP using the Rhyme framework (even though your other modules are still using the implementation classes). This can be very helpful for inspecting data sources without using a debugger
- You can still re-factor everything easily during development, and continously verify that the API is designed well
- When there is an actual reason to break up your system into multiple services, you can easily do so. As the interfaces for remote access via HAL+API are exactly the same as for the server-side implementation, you can keep much of the existing code. 


## Related Links

Documentation: https://caravan.wcm.io/rhyme/<br/>
Issues: https://wcm-io.atlassian.net/<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-rhyme/actions<br/>
Commercial support: https://wcm.io/commercial-support.html


## Build from sources

If you want to build wcm.io from sources make sure you have configured all [Maven Repositories](https://caravan.wcm.io/maven.html) in your settings.xml.

See [Maven Settings](https://github.com/wcm-io-caravan/caravan-rhyme/blob/develop/.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
