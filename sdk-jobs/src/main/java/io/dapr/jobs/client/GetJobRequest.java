package io.dapr.jobs.client;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class GetJobRequest {
  private final String name;

  private GetJobRequest(Builder builder) {
    this.name = builder.name;
  }

  /**
   * Creates a new builder instance for {@link GetJobRequest}.
   *
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;

    /**
     * Sets the name of the job.
     *
     * @param name The job name.
     * @return This builder instance.
     */
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Builds a {@link GetJobRequest} instance.
     *
     * @return A new {@link GetJobRequest} instance.
     */
    public GetJobRequest build() {
      return new GetJobRequest(this);
    }
  }

  // Getters

  /**
   * Gets the name of the job.
   *
   * @return The job name.
   */
  public String getName() {
    return name;
  }
}
