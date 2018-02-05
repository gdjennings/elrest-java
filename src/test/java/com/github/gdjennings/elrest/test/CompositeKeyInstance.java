package com.github.gdjennings.elrest.test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
@IdClass(CompositePK.class)
public class CompositeKeyInstance {

	@Id
	private String key1;

	@Id
	private String key2;

	private String string1;

	public CompositeKeyInstance() {

	}

	public CompositeKeyInstance(String key1, String key2) {
		this.key1 = key1;
		this.key2 = key2;
	}

	@OneToMany(mappedBy = "composite")
	private Set<OneToManyCompositeInstance> many = new HashSet<>();

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

	public String getString1() {
		return string1;
	}

	public void setString1(String string1) {
		this.string1 = string1;
	}

	public Set<OneToManyCompositeInstance> getMany() {
		return many;
	}

	public void setMany(Set<OneToManyCompositeInstance> many) {
		this.many = many;
	}
}
