/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client.domain;

import java.time.Instant;

/**
 * Represents a request to schedule a job in Dapr.
 */
public class GetJobResponse {
  private final String name;
  private byte[] data;
  private JobSchedule schedule;
  private Instant dueTime;
  private Integer repeats;
  private Instant ttl;
  private FailurePolicy failurePolicy;

  /**
   * Constructor to create GetJobResponse.
   *
   * @param name of the job.
   * @param schedule job has to run.
   */
  public GetJobResponse(String name, JobSchedule schedule) {
    this.name = name;
    this.schedule = schedule;
  }

  /**
   * Constructor to create GetJobResponse.
   *
   * @param name of the job.
   * @param dueTime An optional time at which the job should be active, or the “one shot” time, if other scheduling
   *                type fields are not provided. Accepts a “point in time” string in the format of RFC3339,
   *                Go duration string (calculated from creation time), or non-repeating ISO8601
   */
  public GetJobResponse(String name, Instant dueTime) {
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
  public GetJobResponse setData(byte[] data) {
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
  public GetJobResponse setSchedule(JobSchedule schedule) {
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
  public GetJobResponse setDueTime(Instant dueTime) {
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
  public GetJobResponse setRepeat(Integer repeats) {
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
  public GetJobResponse setTtl(Instant ttl) {
    this.ttl = ttl;
    return this;
  }

  /**
   * Sets the failure policy for the scheduled job.
   * This defines how the job should behave in case of failure, such as retrying with a delay
   * or dropping the job entirely.
   *
   * @param failurePolicy the {@link FailurePolicy} to apply to the job
   * @return this {@code ScheduleJobRequest} instance for method chaining
   */
  public GetJobResponse setFailurePolicy(FailurePolicy failurePolicy) {
    this.failurePolicy = failurePolicy;
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
  public Instant getDueTime() {
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
  public Instant getTtl() {
    return ttl;
  }

  /**
   * Gets the failure policy.
   *
   * @return FailurePolicy.
   */
  public FailurePolicy getFailurePolicy() {
    return failurePolicy;
  }
}
