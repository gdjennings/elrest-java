package com.github.gdjennings.elrest.test;

import javax.persistence.Id;
import java.io.Serializable;

public class CompositePK implements Serializable {
	private String key1;

	private String key2;


	public String getKey1() {
		return key1;
	}

	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2(String key2) {
		this.key2 = key2;
	}
}
