package io.dapr.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeRefTest {

  @Test
  public void testTypeRefIsPrimitive() {
    Assertions.assertTrue(TypeRef.isPrimitive(TypeRef.BOOLEAN), "expected this to be true as boolean is primitive");
    Assertions.assertTrue(TypeRef.isPrimitive(TypeRef.SHORT), "expected this to be true as short is primitive");
    Assertions.assertTrue(TypeRef.isPrimitive(TypeRef.FLOAT), "expected this to be true as float is primitive");
    Assertions.assertTrue(TypeRef.isPrimitive(TypeRef.DOUBLE), "expected this to be true as double is primitive");
    Assertions.assertTrue(TypeRef.isPrimitive(TypeRef.INT), "expected this to be true as integer is primitive");

    Assertions.assertFalse(TypeRef.isPrimitive(TypeRef.STRING),
      "expected this to be false as string is not primitive");
    Assertions.assertFalse(TypeRef.isPrimitive(TypeRef.STRING_ARRAY),
      "expected this to be false as string array is not primitive");
  }
}
