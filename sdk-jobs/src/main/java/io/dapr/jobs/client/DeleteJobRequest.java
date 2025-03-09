package io.dapr.jobs.client;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class DeleteJobRequest {
  private final String name;

  private DeleteJobRequest(Builder builder) {
    this.name = builder.name;
  }

  /**
   * Creates a new builder instance for {@link DeleteJobRequest}.
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
     * Builds a {@link DeleteJobRequest} instance.
     *
     * @return A new {@link DeleteJobRequest} instance.
     */
    public DeleteJobRequest build() {
      return new DeleteJobRequest(this);
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
