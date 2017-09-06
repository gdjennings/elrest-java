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
public class ManyToMany1 {

	@Id
	@GeneratedValue(generator = "system-uuid")
	private String id;

	@ManyToMany(mappedBy = "to1")
	private Set<ManyToMany2> to2 = new HashSet<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<ManyToMany2> getTo2() {
		return to2;
	}

	public void setTo2(Set<ManyToMany2> to2) {
		this.to2 = to2;
	}

}
