package com.fmgame.bolt.benchmark.entity;

/**
 * @author luowei
 * @date 2018年4月23日 下午5:43:08
 */
public class PersonFullName {

	private String firstName;

	private String lastName;

	public PersonFullName(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

}
