package io.dapr.jobs.client;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class GetJobRequest {
  private final String name;

  /**
   * Constructor to create Get Job Request.
   *
   * @param name of the job to fetch..
   */
  public GetJobRequest(String name) {
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
