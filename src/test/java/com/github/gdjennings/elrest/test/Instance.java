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
 *
 * @author markcocquio
 */
@Entity
public class Instance {
	
	@Id @GeneratedValue(generator = "system-uuid")
	private String name;
	
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


}
