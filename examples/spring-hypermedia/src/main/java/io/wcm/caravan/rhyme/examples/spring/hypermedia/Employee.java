/*
 * Copyright 2017 the original author or authors.
 *
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
 */
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Greg Turnquist
 */
@Entity
@EntityListeners(RepositoryModificationListener.class)
public class Employee {

  /** a generated ID (to be used in all the link templates) */
  @Id
  @GeneratedValue
  private Long id;


  /** the first name */
  private String name;

  /** the role within the company */
  private String role;

  /**
   * To break the recursive, bidirectional relationship, don't serialize {@literal manager}.
   */
  @JsonIgnore
  @ManyToOne
  private Manager manager;

  Employee(String name, String role, Manager manager) {

    this.name = name;
    this.role = role;
    this.manager = manager;
  }

  public Employee() {
    // required for deserialization
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return this.role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
