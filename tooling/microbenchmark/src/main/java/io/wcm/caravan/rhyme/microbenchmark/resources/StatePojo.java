package io.wcm.caravan.rhyme.microbenchmark.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class StatePojo {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String foo;
  private Integer bar;

  private List<Integer> list;

  private Map<String, StatePojo> nestedObjects = new HashMap<>();

  public String getFoo() {
    return foo;
  }

  public StatePojo setFoo(String foo) {
    this.foo = foo;
    return this;
  }

  public Integer getBar() {
    return bar;
  }

  public StatePojo setBar(Integer bar) {
    this.bar = bar;
    return this;
  }

  public List<Integer> getList() {
    return list;
  }

  public StatePojo setList(List<Integer> list) {
    this.list = list;
    return this;
  }

  public Map<String, StatePojo> getNestedObjects() {
    return nestedObjects;
  }

  public StatePojo setNestedObjects(Map<String, StatePojo> value) {
    this.nestedObjects = value;
    return this;
  }

  public static StatePojo createTestState() {
    return new StatePojo()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9))
        .setNestedObjects(ImmutableMap.of(
            "foo", createNestedTestState(),
            "bar", createNestedTestState()));
  }

  private static StatePojo createNestedTestState() {
    return new StatePojo()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9));
  }

  public static ObjectNode createTestJson() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("foo", "123")
        .put("bar", 456)
        .putArray("list").add(7).add(8).add(9);

    ObjectNode nested = json.putObject("nestedObjects");
    nested.set("foo", createNestedTestJson());
    nested.set("bar", createNestedTestJson());

    return json;
  }

  static ObjectNode createNestedTestJson() {
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
