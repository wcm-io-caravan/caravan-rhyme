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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;

public class ReadMeExamples {

  JsonResourceLoader jsonLoader;

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

    @Related("first")
    PageResource getFirstPage();

    @Related("item")
    ItemResource getItemById(@TemplateVariable("id") String id);
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

  public class Item {

    public String id;
    public String title;
  }

  // client examples


  private Rhyme createRhymeForIncomingRequest() {

    return RhymeBuilder.withResourceLoader(jsonLoader)
        .buildForRequestTo(incomingRequest.getUrl());
  }

  private void convertToFrameworkResponse(HalResponse response) {

    System.out.println(response.getBody().getModel().toString());
  }

  private ApiEntryPoint getApiEntryPoint() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    return rhyme.getUpstreamEntryPoint("https://hal-api.example.org", ApiEntryPoint.class);
  }

  void fetchItems() {

    ApiEntryPoint api = getApiEntryPoint();

    ItemResource itemResource = api.getItemById("foo");

    Item foo = itemResource.getState();

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


  @Test
  void handleEntryPointRequest() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    ApiEntryPoint entryPoint = new ApiEntryPointImpl(database);

    HalResponse response = rhyme.renderResponse(entryPoint);

    convertToFrameworkResponse(response);
  }

  @Test
  void handleItemResourceRequest() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    ItemResource resource = new ItemResourceImpl(database, "2");

    HalResponse response = rhyme.renderResponse(resource);

    convertToFrameworkResponse(response);
  }

  @Test
  void handleFirstPageRequest() {

    Rhyme rhyme = createRhymeForIncomingRequest();

    PageResource resource = new PagingPageResourceImpl(database, 0);

    HalResponse response = rhyme.renderResponse(resource);

    convertToFrameworkResponse(response);
  }

  class ApiEntryPointImpl implements ApiEntryPoint {

    private final ItemDatabase database;

    ApiEntryPointImpl(ItemDatabase database) {
      this.database = database;
    }

    @Override
    public PageResource getFirstPage() {
      return new SimplePageResourceImpl(database);
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

  class SimplePageResourceImpl implements PageResource {

    private final ItemDatabase database;

    public SimplePageResourceImpl(ItemDatabase database) {
      this.database = database;
    }

    @Override
    public Stream<ItemResource> getItemsOnPage() {
      return database.getAllIds().stream()
          .map(id -> new ItemResourceImpl(database, id));
    }

    @Override
    public Optional<PageResource> getNextPage() {
      return Optional.empty();
    }

    @Override
    public Link createLink() {
      return new Link("https://hal-api.example.org/items")
          .setTitle("A pageable list of all available items in the database");
    }

  }

  class PagingPageResourceImpl implements PageResource {

    private final ItemDatabase database;

    private int numItemsPerPage = 10;
    private int startIndex;

    public PagingPageResourceImpl(ItemDatabase database, int startIndex) {
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

      return Optional.of(new PagingPageResourceImpl(database, startIndex + numItemsPerPage));
    }

    @Override
    public Link createLink() {
      return new Link("https://hal-api.example.org/items?startIndex=" + startIndex);
    }

  }

  class ItemResourceImpl implements ItemResource {

    private final ItemDatabase database;
    private final String id;

    public ItemResourceImpl(ItemDatabase database, String id) {
      this.database = database;
      this.id = id;
    }

    @Override
    public Item getState() {

      return database.getById(id);
    }

    @Override
    public Stream<ItemResource> getRelatedItems() {

      return database.getIdsOfItemsRelatedTo(id).stream()
          .map(relatedId -> new ItemResourceImpl(database, relatedId));
    }

    @Override
    public Link createLink() {

      UriTemplate uriTemplate = UriTemplate.fromTemplate("https://hal-api.example.org/items/{id}");

      if (id != null) {
        uriTemplate.set("id", id);
      }

      Link link = new Link(uriTemplate.expandPartial());

      if (id != null) {
        link.setTitle("The item with id '" + id + "'");
        link.setName(id);
      }
      else {
        link.setTitle("Retrieves the item with the specified id");
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

      return Collections.emptyList();
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
