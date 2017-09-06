/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest.test;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author grantjennings
 */
@Entity
public class Instance {

	@Id
	@GeneratedValue(generator = "system-uuid")
	private String name;

	private int number;

	private String field;

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


}
