/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;


import com.github.gdjennings.elrest.test.Instance;
import com.github.gdjennings.elrest.test.User;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author grantjennings
 */
public class ExpressionImplTest {
	@Test
	public void testPropertyEquals() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");

		Instance e2 = new Instance();
		e2.setName("testName2");

		ELFilterImpl el = new ELFilterImpl("name eq \"testName1\"", new HashMap<>());
		List r = (List)el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@Test
	public void testBooleanProperty() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setaBool(true);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setaBool(false);

		ELFilterImpl el = new ELFilterImpl("aBool eq true", new HashMap<>());
		List r = (List)el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}


	@Test
	public void testPropertyIn() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");

		Instance e2 = new Instance();
		e2.setName("testName2");

		Instance e3 = new Instance();
		e3.setName("testName3");

		ELFilterImpl el = new ELFilterImpl("name in \"testName1,testName2\"", new HashMap<>());
		List r = (List)el.filter(Arrays.asList(e1, e2, e3));

		assertNotNull(r);
		assertEquals(2, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@Test
	public void testNotIn() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");
		e1.setaBool(true);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		e2.setaBool(false);

		ELFilterImpl el = new ELFilterImpl("name not in testName1");
		List<Instance> r = (List<Instance>) el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());


		el = new ELFilterImpl("name !in testName1");
		r = (List<Instance>) el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());
	}

	@Test
	public void testNotEq() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");
		e1.setaBool(true);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		e2.setaBool(false);

		ELFilterImpl el = new ELFilterImpl("name !eq testName1");
		List<Instance> r = (List<Instance>) el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());


		el = new ELFilterImpl("name ne testName1");
		r = (List<Instance>) el.filter(Arrays.asList(e1, e2));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());
	}

	@Test
	public void testNotLike() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");

		Instance e3 = new Instance();
		e3.setName("otherName");
		e3.setNumber(3);
		e3.setField("B");

		ELFilterImpl el = new ELFilterImpl("name not like \"testName%\"");
		List<Instance> r = (List<Instance>) el.filter(Arrays.asList(e1, e2, e3));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("otherName", r.get(0).getName());


		el = new ELFilterImpl("name !like \"testName%\"");
		r = (List<Instance>) el.filter(Arrays.asList(e1, e2, e3));

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("otherName", r.get(0).getName());
	}


	@Test
	public void testISO8601() throws Exception {
		User u1 = new User();
		u1.setUsername("u1");
		u1.setCreatedDate(Calendar.getInstance());

		User u2 = new User();
		u2.setUsername("u2");
		Calendar future = Calendar.getInstance();
		future.set(Calendar.YEAR, 3000);
		u2.setCreatedDate(future);

		System.out.println("createdDate lt "+Calendar.getInstance().getTimeInMillis());
		ELFilterImpl el = new ELFilterImpl("createdDate lt "+Calendar.getInstance().getTimeInMillis());
		List<User> r = (List<User>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getUsername());


		System.out.println("createdDate lt \""+ DatatypeConverter.printDateTime(Calendar.getInstance())+"\"");
		el = new ELFilterImpl("createdDate lt \""+ DatatypeConverter.printDateTime(Calendar.getInstance())+"\"");
		r = (List<User>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getUsername());
	}

	@Test
	public void testNotEqualsNull() throws Exception {
		Instance u1 = new Instance();
		u1.setName("u1");
		u1.setaDate(Calendar.getInstance());
		u1.setAnEnum(Instance.InstanceEnum.TYPE1);
		u1.setNumber(1);
		u1.setaLong(1L << 40);
		u1.setField("notNull");

		Instance u2 = new Instance();

		System.out.println("aDate ne null");
		ELFilterImpl el = new ELFilterImpl("aDate ne null");
		List<Instance> r = (List<Instance>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

		System.out.println("field ne null");
		el = new ELFilterImpl("field ne null");
		r = (List<Instance>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

		System.out.println("number ne null");
		el = new ELFilterImpl("number ne null");
		r = (List<Instance>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(2, r.size());

		System.out.println("autoboxedLong ne null");
		el = new ELFilterImpl("aLong ne null");
		r = (List<Instance>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());


		System.out.println("anEnum ne null");
		el = new ELFilterImpl("anEnum ne null");
		r = (List<Instance>) el.filter(Arrays.asList(u1, u2));
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

	}
}
