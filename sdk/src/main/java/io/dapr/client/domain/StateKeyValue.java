package io.dapr.client.domain;

public class StateKeyValue<T> {
  private T value;
  private String key;
  private String etag;
  private StateOptions options;

  public StateKeyValue() {
  }

  public StateKeyValue(T value, String key, String etag) {
    this.value = value;
    this.key = key;
    this.etag = etag;
  }

  public StateKeyValue(T value, String key, String etag, StateOptions options) {
    this.value = value;
    this.key = key;
    this.etag = etag;
    this.options = options;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public StateOptions getOptions() {
    return options;
  }

  public void setOptions(StateOptions options) {
    this.options = options;
  }
}
