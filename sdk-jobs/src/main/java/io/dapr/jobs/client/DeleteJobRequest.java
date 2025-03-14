package io.dapr.jobs.client;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class DeleteJobRequest {
  private final String name;

  /**
   * Constructor to create Delete Job Request.
   *
   * @param name of the job to delete.
   */
  public DeleteJobRequest(String name) {
    this.name = name;
  }

  /**
   * Gets the name of the job.
   *
   * @return The job name.
   */
  public String getName() {
    return name;
  }
}
