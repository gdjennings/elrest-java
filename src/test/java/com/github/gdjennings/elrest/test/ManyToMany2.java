/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author grantjennings
 */
@Entity
public class ManyToMany2 {
	@Id
	private String id;

	@ManyToMany
	private Set<ManyToMany1> to1 = new HashSet<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<ManyToMany1> getTo1() {
		return to1;
	}

	public void setTo1(Set<ManyToMany1> to1) {
		this.to1 = to1;
	}

}
