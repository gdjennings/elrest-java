/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest.test;


import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Calendar;

/**
 * @author grantjennings
 */
@Entity
public class Instance {

	public enum InstanceEnum {
		TYPE0,
		TYPE1,
		TYPE2
	}

	@Id
	@GeneratedValue(generator = "system-uuid")
	private String name;

	private int number;

	private Long aLong;

	private String field;

	@Enumerated(EnumType.STRING)
	private InstanceEnum anEnum;

	@Temporal(TemporalType.TIMESTAMP)
	private Calendar aDate;

	@ManyToOne
	private Instance circular;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return name;
	}


	public Instance getCircular() {
		return circular;
	}

	public void setCircular(Instance circular) {
		this.circular = circular;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Calendar getaDate() {
		return aDate;
	}

	public void setaDate(Calendar aDate) {
		this.aDate = aDate;
	}

	public InstanceEnum getAnEnum() {
		return anEnum;
	}

	public void setAnEnum(InstanceEnum anEnum) {
		this.anEnum = anEnum;
	}

	public Long getaLong() {
		return aLong;
	}

	public void setaLong(Long aLong) {
		this.aLong = aLong;
	}
}
