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
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author grantjennings
 */
@Entity
public class OneToManyInstance {

	@Id
	@GeneratedValue(generator = "system-uuid")
	private String name;

	@OneToMany(mappedBy = "one")
	private Set<OneToManyInstance> many = new HashSet<>();

	@ManyToOne
	private OneToManyInstance one;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<OneToManyInstance> getMany() {
		return many;
	}

	public void setMany(Set<OneToManyInstance> many) {
		this.many = many;
	}

	public OneToManyInstance getOne() {
		return one;
	}

	public void setOne(OneToManyInstance one) {
		this.one = one;
	}


	public String getId() {
		return name;
	}


}
