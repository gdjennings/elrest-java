/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author grantjennings
 */
@Entity
public class OneToManyCompositeInstance {

	@Id
	private String name;

	private String aString;

	@JoinColumns({
			@JoinColumn(name = "key1", referencedColumnName = "key1"),
			@JoinColumn(name = "key2", referencedColumnName = "key2")
	})
	@ManyToOne(targetEntity = CompositeKeyInstance.class)
	private CompositeKeyInstance composite;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CompositeKeyInstance getComposite() {
		return composite;
	}

	public void setComposite(CompositeKeyInstance composite) {
		this.composite = composite;
	}

	public String getId() {
		return name;
	}

	public String getaString() {
		return aString;
	}

	public void setaString(String aString) {
		this.aString = aString;
	}
}
