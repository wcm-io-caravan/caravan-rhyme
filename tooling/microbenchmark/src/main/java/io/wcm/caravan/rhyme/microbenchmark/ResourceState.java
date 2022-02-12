package io.wcm.caravan.rhyme.microbenchmark;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

public class ResourceState {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String foo;
  private Integer bar;

  private List<Integer> list;

  public String getFoo() {
    return foo;
  }

  public ResourceState setFoo(String foo) {
    this.foo = foo;
    return this;
  }

  public Integer getBar() {
    return bar;
  }

  public ResourceState setBar(Integer bar) {
    this.bar = bar;
    return this;
  }

  public List<Integer> getList() {
    return list;
  }

  public ResourceState setList(List<Integer> list) {
    this.list = list;
    return this;
  }

  static ResourceState createTestState() {
    return new ResourceState()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9));
  }

  static ObjectNode createTestJson() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("foo", "123")
        .put("bar", 456)
        .putArray("list").add(7).add(8).add(9);
    return json;
  }

  public static ObjectNode createMappedJson() {
    return OBJECT_MAPPER.convertValue(createTestState(), ObjectNode.class);
  }
}
