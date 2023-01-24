package io.dapr.utils;

import org.junit.Assert;
import org.junit.Test;

public class TypeRefTest {

  @Test
  public void testTypeRefIsPrimitive() {
    Assert.assertTrue("expected this to be true as boolean is primitive", TypeRef.isPrimitive(TypeRef.BOOLEAN));
    Assert.assertTrue("expected this to be true as short is primitive", TypeRef.isPrimitive(TypeRef.SHORT));
    Assert.assertTrue("expected this to be true as float is primitive", TypeRef.isPrimitive(TypeRef.FLOAT));
    Assert.assertTrue("expected this to be true as double is primitive", TypeRef.isPrimitive(TypeRef.DOUBLE));
    Assert.assertTrue("expected this to be true as integer is primitive", TypeRef.isPrimitive(TypeRef.INT));

    Assert.assertFalse("expected this to be false as string is not primitive",
        TypeRef.isPrimitive(TypeRef.STRING));
    Assert.assertFalse("expected this to be false as string array is not primitive",
        TypeRef.isPrimitive(TypeRef.STRING_ARRAY));
  }
}
