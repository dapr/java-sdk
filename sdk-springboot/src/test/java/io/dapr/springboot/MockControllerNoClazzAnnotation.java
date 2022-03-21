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