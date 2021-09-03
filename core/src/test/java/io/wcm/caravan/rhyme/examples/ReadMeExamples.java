/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

public class ReadMeExamples {

  HalResourceLoader resourceLoader;

  Request incomingRequest = new Request() {

    @Override
    public String getUrl() {
      return "/";
    }
  };

  ItemDatabase database = new ItemDatabaseImpl();

  // interface definitions

  @HalApiInterface
  public interface ApiEntryPoint extends LinkableResource {

    @Related("item")
    ItemResource getItemById(@TemplateVariable("id") String id);

    @Related("first")
    PageResource getFirstPage();
  }

  @HalApiInterface
  public interface PageResource extends LinkableResource {

    @Related("item")
    Stream<ItemResource> getItemsOnPage();

    @Related("next")
    Optional<PageResource> getNextPage();
  }

  @HalApiInterface
  public interface ItemResource extends LinkableResource {

    @ResourceState
    Item getState();

    @Related("related")
    Stream<ItemResource> getRelatedItems();
  }

  @HalApiInterface
  public interface ReactiveResource extends LinkableResource {

    @ResourceState
    Single<Item> getState();

    @Related("related")
    Observable<ReactiveResource> getRelatedItems();

    @Related("parent")
    Maybe<ReactiveResource> getParentItem();
  }

  public class Item {

    public String id;
    public String title;
  }

  // client examples


  private Rhyme createRhymeForIncomingRequest() {

    return RhymeBuilder.withResourceLoader(resourceLoader)
        .buildForRequestTo(incomingRequest.getUrl());
  }

  private void convertToFrameworkResponse(HalResponse response) {

    response.getBody().removeEmbedded("rhyme:metadata");
    if (response.getBody().getModel().path("_embedded").size() == 0) {
      response.getBody().getModel().remove("_embedded");
    }
    System.out.println(response.getBody().getModel().toString());
  }

  private ApiEntryPoint getApiEntryPoint() {

    // create a Rhyme instance that knows how to load any external JSON resource
    Rhyme rhyme = RhymeBuilder.withResourceLoader(resourceLoader)
        .buildForRequestTo(incomingRequest.getUrl());

    // create a dynamic proxy that knows how to fetch the entry point from the given URL
    return rhyme.getRemoteResource("https://hal-api.example.org", ApiEntryPoint.class);
  }

  void fetchItems() {
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

    assertThat(foo.id).isEqualTo("foo");
    assertThat(relatedToFoo).isNotNull();
  }

  Stream<ItemResource> collectItems(PageResource currentPage) {

    Stream<ItemResource> onThisPage = currentPage.getItemsOnPage();

    Stream<ItemResource> onFollowingPages = currentPage.getNextPage()
        .map(this::collectItems)
        .orElse(Stream.empty());

    return Stream.concat(onThisPage, onFollowingPages);
  }

  void fetchAllItems() {

    ApiEntryPoint api = getApiEntryPoint();

    List<Item> allItems = collectItems(api.getFirstPage())
        .map(ItemResource::getState)
        .collect(Collectors.toList());

    assertThat(allItems).isNotEmpty();
  }

  // server side implementation


  //@Test (not really a test but junit was used to execute this method to provide the example JSON)
  void handleEntryPointRequest() {

    // create a single Rhyme instance as early as possible in the request-cycle
    Rhyme rhyme = RhymeBuilder.create().buildForRequestTo(incomingRequest.getUrl());

    // instantiate your server-side implementation of the requested @HalApiInterface resource
    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);

    // create the HAL+JSON representation (and response headers) for this resource
    HalResponse response = rhyme.renderResponse(entryPoint).blockingGet();

    // finally convert that response to your framework's representation of a web/JSON response...
    convertToFrameworkResponse(response);
  }

  //@Test (not really a test but junit was used to execute this method to provide the example JSON)
  void handleItemResourceRequest() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    ItemResource resource = new ItemResourceImpl(database, "2");

    HalResponse response = rhyme.renderResponse(resource).blockingGet();

    convertToFrameworkResponse(response);
  }

  //@Test (not really a test but junit was used to execute this method to provide the example JSON)
  void handleFirstPageRequest() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    PageResource resource = new PageResourceImpl(database, 0);

    HalResponse response = rhyme.renderResponse(resource).blockingGet();

    convertToFrameworkResponse(response);
  }

  void reactiveExample() {

    Rhyme rhyme = RhymeBuilder.withResourceLoader(resourceLoader)
        .buildForRequestTo(incomingRequest.getUrl());

    ReactiveResource resource = rhyme.getRemoteResource("https://foo.bar", ReactiveResource.class);

    Observable<Item> parentsOfRelated = resource.getRelatedItems()
        .concatMapEager(ReactiveResource::getRelatedItems)
        .concatMapMaybe(ReactiveResource::getParentItem)
        .concatMapSingle(ReactiveResource::getState)
        .distinct(item -> item.id);

    // all Observable provided by rhyme are *cold*, i.e. no HTTP requests would have been executed so far.

    Single<HalResponse> response = rhyme.renderResponse(resource);
  }

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


  class PageResourceImpl implements PageResource {

    private final ItemDatabase database;

    private int numItemsPerPage = 10;
    private int startIndex;

    PageResourceImpl(ItemDatabase database, int startIndex) {
      this.database = database;
      this.startIndex = startIndex;
    }

    @Override
    public Stream<ItemResource> getItemsOnPage() {

      List<String> idsOnPage = database.getIds(startIndex, numItemsPerPage);

      return idsOnPage.stream().map(id -> new ItemResourceImpl(database, id));
    }

    @Override
    public Optional<PageResource> getNextPage() {

      boolean hasNextPage = database.getItemCount() > startIndex + numItemsPerPage;

      if (!hasNextPage) {
        return Optional.empty();
      }

      return Optional.of(new PageResourceImpl(database, startIndex + numItemsPerPage));
    }

    @Override
    public Link createLink() {
      return new Link("https://hal-api.example.org/items?startIndex=" + startIndex);
    }

  }

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
        // for machines, you should set always set a 'name' attribute to distinguish
        // multiple links with the same relations
        link.setName(id);
      }
      else {
        // especially link templates should always have a good description in title, as these
        // are likely to appear in the entry point of your resource, and will help to make
        // your API self-explainable
        link.setTitle("A link template to retrieve the item with the specified id from the database");
      }

      return link;
    }
  }

  public interface Request {

    String getUrl();
  }

  public interface ItemDatabase {

    Item getById(String id);

    List<String> getIdsOfItemsRelatedTo(String id);

    List<String> getAllIds();

    List<String> getIds(int startIndex, int numIds);

    int getItemCount();
  }

  public class ItemDatabaseImpl implements ItemDatabase {

    private final Map<String, Item> allItems;

    ItemDatabaseImpl() {
      allItems = IntStream.range(0, 15)
          .mapToObj(this::createItem)
          .collect(Collectors.toMap(item -> item.id, item -> item, (i1, i2) -> i1, () -> new LinkedHashMap<>()));
    }

    private Item createItem(int index) {
      Item item = new Item();
      item.id = Integer.toString(index);
      item.title = "Item #" + index;
      return item;
    }

    @Override
    public Item getById(String id) {

      return allItems.get(id);
    }

    @Override
    public List<String> getIdsOfItemsRelatedTo(String id) {

      return getAllIds().stream()
          .filter(otherId -> !otherId.equals(id))
          .filter(otherId -> otherId.contains(id))
          .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllIds() {

      return ImmutableList.copyOf(allItems.keySet());
    }

    @Override
    public List<String> getIds(int startIndex, int numIds) {

      return getAllIds().subList(startIndex, startIndex + numIds);
    }

    @Override
    public int getItemCount() {

      return allItems.size();
    }

  }
}
