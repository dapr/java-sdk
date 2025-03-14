package io.dapr.jobs.client;

import java.time.OffsetDateTime;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class ScheduleJobRequest {
  private final String name;
  private byte[] data;
  private JobSchedule schedule;
  private OffsetDateTime dueTime;
  private Integer repeats;
  private OffsetDateTime ttl;

  /**
   * Constructor to create ScheduleJobRequest.
   *
   * @param name of the job.
   * @param schedule job has to run.
   */
  public ScheduleJobRequest(String name, JobSchedule schedule) {
    this.name = name;
    this.schedule = schedule;
  }

  /**
   * Constructor to create ScheduleJobRequest.
   *
   * @param name of the job.
   * @param dueTime An optional time at which the job should be active, or the “one shot” time, if other scheduling
   *                type fields are not provided. Accepts a “point in time” string in the format of RFC3339,
   *                Go duration string (calculated from creation time), or non-repeating ISO8601
   */
  public ScheduleJobRequest(String name, OffsetDateTime dueTime) {
    this.name = name;
    this.dueTime = dueTime;
  }

  /**
   * Sets the data payload for the job.
   * This should be a JSON serialized value or object.
   *
   * @param data The job data in byte array format.
   * @return This builder instance.
   */
  public ScheduleJobRequest setData(byte[] data) {
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
  public ScheduleJobRequest setSchedule(JobSchedule schedule) {
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
  public ScheduleJobRequest setDueTime(OffsetDateTime dueTime) {
    this.dueTime = dueTime;
    return this;
  }

  /**
   * Sets the number of times the job should be triggered.
   * If not set, the job runs indefinitely or until expiration.
   *
   * @param repeats The number of times the job should repeat.
   * @return This builder instance.
   */
  public ScheduleJobRequest setRepeat(Integer repeats) {
    this.repeats = repeats;
    return this;
  }

  /**
   * Sets the time-to-live (TTL) or expiration for the job.
   * This can be in RFC3339, Go duration string, or non-repeating ISO8601 format.
   *
   * @param ttl The time-to-live for the job.
   * @return This builder instance.
   */
  public ScheduleJobRequest setTtl(OffsetDateTime ttl) {
    this.ttl = ttl;
    return this;
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
