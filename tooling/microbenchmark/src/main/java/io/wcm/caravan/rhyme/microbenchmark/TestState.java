package io.wcm.caravan.rhyme.microbenchmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestState {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String foo;
  private Integer bar;

  private List<Integer> list;

  private Map<String, TestState> nestedObjects = new HashMap<String, TestState>();

  public String getFoo() {
    return foo;
  }

  public TestState setFoo(String foo) {
    this.foo = foo;
    return this;
  }

  public Integer getBar() {
    return bar;
  }

  public TestState setBar(Integer bar) {
    this.bar = bar;
    return this;
  }

  public List<Integer> getList() {
    return list;
  }

  public TestState setList(List<Integer> list) {
    this.list = list;
    return this;
  }

  public Map<String, TestState> getNestedObjects() {
    return nestedObjects;
  }

  public TestState setNestedObjects(Map<String, TestState> value) {
    this.nestedObjects = value;
    return this;
  }

  static TestState createTestState() {
    return new TestState()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9))
        .setNestedObjects(ImmutableMap.of(
            "foo", createNestedTestState(),
            "bar", createNestedTestState()));
  }

  private static TestState createNestedTestState() {
    return new TestState()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9));
  }

  static ObjectNode createTestJson() {
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
