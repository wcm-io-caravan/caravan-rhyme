package io.wcm.caravan.rhyme.microbenchmark.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BenchmarkResourceState {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String foo;
  private Integer bar;

  private List<Integer> list;

  private Map<String, BenchmarkResourceState> nestedObjects = new HashMap<>();

  public String getFoo() {
    return foo;
  }

  public BenchmarkResourceState setFoo(String foo) {
    this.foo = foo;
    return this;
  }

  public Integer getBar() {
    return bar;
  }

  public BenchmarkResourceState setBar(Integer bar) {
    this.bar = bar;
    return this;
  }

  public List<Integer> getList() {
    return list;
  }

  public BenchmarkResourceState setList(List<Integer> list) {
    this.list = list;
    return this;
  }

  public Map<String, BenchmarkResourceState> getNestedObjects() {
    return nestedObjects;
  }

  public BenchmarkResourceState setNestedObjects(Map<String, BenchmarkResourceState> value) {
    this.nestedObjects = value;
    return this;
  }

  public static BenchmarkResourceState createTestState() {
    return new BenchmarkResourceState()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9))
        .setNestedObjects(ImmutableMap.of(
            "foo", createNestedTestState(),
            "bar", createNestedTestState()));
  }

  private static BenchmarkResourceState createNestedTestState() {
    return new BenchmarkResourceState()
        .setFoo("123")
        .setBar(456)
        .setList(ImmutableList.of(7, 8, 9));
  }

  public static ObjectNode createMappedJson() {
    return OBJECT_MAPPER.convertValue(createTestState(), ObjectNode.class);
  }
}