package io.dapr.client.domain;

import com.google.protobuf.Message;

/**
 * A Job to schedule
 *
 * @param <T> The class type of Job data.
 */
public final class Job<T> {

	private final String name;
	
	private String schedule;
	
	private Integer repeats;
	
	private String dueTime;
	
	private String ttl;
	
	private final T data;

	/**
	 * Constructor for Job
	 * 
	 * @param name name of the job to create
	 */
	public Job(String name, T data) {
		super();
		this.name = name;
		this.data = data;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public Integer getRepeats() {
		return repeats;
	}

	public void setRepeats(Integer repeats) {
		this.repeats = repeats;
	}

	public String getDueTime() {
		return dueTime;
	}

	public void setDueTime(String dueTime) {
		this.dueTime = dueTime;
	}

	public String getTtl() {
		return ttl;
	}

	public void setTtl(String ttl) {
		this.ttl = ttl;
	}

	public T getData() {
		return data;
	}

	public String getName() {
		return name;
	}
}
