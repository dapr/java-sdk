/*
 * Copyright 2022 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.springboot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class MockControllerNoClazzAnnotation {

  @RequestMapping(value = {"", "page1", "page2"}, method = {RequestMethod.POST, RequestMethod.PUT})
  public void testMethod1() {
    // Do nothing
  }

  @PostMapping(path = {"", "page3", "page4"})
  public void testMethod2() {
    // Do nothing
  }

  @PostMapping("foo")
  public void testMethod3() {
    // Do nothing
  }

  @PostMapping({"foo1", "foo2"})
  public void testMethod4() {
    // Do nothing
  }

  @RequestMapping(path = {"bar", "bar1"}, method = {RequestMethod.GET})
  public void testMethod5() {
    // Do nothing
  }
}