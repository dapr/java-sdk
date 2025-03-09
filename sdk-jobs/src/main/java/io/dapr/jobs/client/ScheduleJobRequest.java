package io.dapr.jobs.client;

import java.time.OffsetDateTime;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class ScheduleJobRequest {
  private final String name;
  private final byte[] data;
  private final JobSchedule schedule;
  private final OffsetDateTime dueTime;
  private final Integer repeats;
  private final OffsetDateTime ttl;

  private ScheduleJobRequest(Builder builder) {
    this.name = builder.name;
    this.data = builder.data;
    this.schedule = builder.schedule;
    this.dueTime = builder.dueTime;
    this.repeats = builder.repeat;
    this.ttl = builder.ttl;
  }

  /**
   * Creates a new builder instance for {@link ScheduleJobRequest}.
   *
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private byte[] data;
    private JobSchedule schedule;
    private OffsetDateTime dueTime;
    private Integer repeat;
    private OffsetDateTime ttl;

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
     * Sets the data payload for the job.
     * This should be a JSON serialized value or object.
     *
     * @param data The job data in byte array format.
     * @return This builder instance.
     */
    public Builder setData(byte[] data) {
      this.data = data;
      return this;
    }

    /**
     * Sets the schedule for the job.
     * The format should follow cron expressions or other supported scheduling formats.
     *
     * @param schedule The job schedule.
     * @return This builder instance.
     */
    public Builder setSchedule(JobSchedule schedule) {
      this.schedule = schedule;
      return this;
    }

    /**
     * Sets the due time when the job should become active or execute once.
     * This can be in RFC3339, Go duration string, or non-repeating ISO8601 format.
     *
     * @param dueTime The due time of the job.
     * @return This builder instance.
     */
    public Builder setDueTime(OffsetDateTime dueTime) {
      this.dueTime = dueTime;
      return this;
    }

    /**
     * Sets the number of times the job should be triggered.
     * If not set, the job runs indefinitely or until expiration.
     *
     * @param repeat The number of times the job should repeat.
     * @return This builder instance.
     */
    public Builder setRepeat(Integer repeat) {
      this.repeat = repeat;
      return this;
    }

    /**
     * Sets the time-to-live (TTL) or expiration for the job.
     * This can be in RFC3339, Go duration string, or non-repeating ISO8601 format.
     *
     * @param ttl The time-to-live for the job.
     * @return This builder instance.
     */
    public Builder setTtl(OffsetDateTime ttl) {
      this.ttl = ttl;
      return this;
    }

    /**
     * Builds a {@link ScheduleJobRequest} instance.
     *
     * @return A new {@link ScheduleJobRequest} instance.
     */
    public ScheduleJobRequest build() {
      return new ScheduleJobRequest(this);
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

  /**
   * Gets the data payload of the job.
   *
   * @return The job data as a byte array.
   */
  public byte[] getData() {
    return data;
  }

  /**
   * Gets the schedule of the job.
   *
   * @return The job schedule.
   */
  public JobSchedule getSchedule() {
    return schedule;
  }

  /**
   * Gets the due time of the job.
   *
   * @return The due time.
   */
  public OffsetDateTime getDueTime() {
    return dueTime;
  }

  /**
   * Gets the number of times the job should repeat.
   *
   * @return The repeat count, or null if not set.
   */
  public Integer getRepeats() {
    return repeats;
  }

  /**
   * Gets the time-to-live (TTL) or expiration of the job.
   *
   * @return The TTL value.
   */
  public OffsetDateTime getTtl() {
    return ttl;
  }
}
