package io.dapr.spring.data.repository.query;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;
import java.util.function.Predicate;

public class DaprPredicate implements Predicate<Object> {

  private final PropertyPath path;
  private final Function<Object, Boolean> check;
  private final Object value;

  public DaprPredicate(PropertyPath path, Object expected) {
    this(path, expected, (valueToCompare) -> ObjectUtils.nullSafeEquals(valueToCompare, expected));
  }


  /**
   * Creates a new {@link DaprPredicate}.
   *
   * @param path  The path to the property to compare.
   * @param value The value to compare.
   * @param check The function to check the value.
   */
  public DaprPredicate(PropertyPath path, Object value, Function<Object, Boolean> check) {
    this.path = path;
    this.check = check;
    this.value = value;
  }

  public PropertyPath getPath() {
    return path;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public boolean test(Object o) {
    Object value = getValueByPath(o, path);
    return check.apply(value);
  }

  private Object getValueByPath(Object root, PropertyPath path) {
    Object currentValue = root;

    for (PropertyPath currentPath : path) {
      currentValue = wrap(currentValue).getPropertyValue(currentPath.getSegment());

      if (currentValue == null) {
        break;
      }
    }

    return currentValue;
  }

  private BeanWrapper wrap(Object o) {
    return new DirectFieldAccessFallbackBeanWrapper(o);
  }
}
