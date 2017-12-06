/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;


import com.github.gdjennings.elrest.test.Instance;
import com.github.gdjennings.elrest.test.OneToManyInstance;
import com.github.gdjennings.elrest.test.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.PluralAttribute;
import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author grantjennings
 */
public class JPAFilterImplTest {
	EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");


	EntityManager em;

	@Before
	public void createEntityManager() {
		em = emf.createEntityManager();
		em.getTransaction().begin();
	}

	@After
	public void rollback() {
		em.getTransaction().rollback();
	}

	@Test
	public void testGroupBy() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Tuple.class);
		el.groupBy(new String[]{"field"}, "sum(number)", new String[]{"field"});
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("A", ((Tuple) r.get(0)).get(0));
		assertEquals(3, ((Tuple) r.get(0)).get(1));
	}

	@Test
	public void testOneToManyQuery() throws ParseException {
		OneToManyInstance i1 = new OneToManyInstance();
		i1.setName("i1");
		em.persist(i1);

		OneToManyInstance i2 = new OneToManyInstance();
		i2.setName("i2");
		i2.setOne(i1);
		em.persist(i2);

		OneToManyInstance i3 = new OneToManyInstance();
		i3.setName("i3");
		i3.setOne(i1);
		em.persist(i3);

		i1.getMany().add(i2);
		i1.getMany().add(i3);
		em.flush();

		JpaELFilterImpl el = new JpaELFilterImpl(em, OneToManyInstance.class, OneToManyInstance.class);
		el.buildExpression("many.name eq \"i2\"");
		List<OneToManyInstance> r = el.getResultList(Integer.MAX_VALUE, 0);
		assertEquals(1, r.size());
		assertEquals("i1", r.get(0).getName());

	}

	@Test
	public void testIn() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name in testName1");
		List<Instance> r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName1", r.get(0).getName());
	}

	@Test
	public void testNotIn() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setNumber(1);
		e1.setField("A");
		e1.setaBool(true);
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		e2.setaBool(false);
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name not in testName1");
		List<Instance> r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());


		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name !in testName1");
		r = el.getResultList(Integer.MAX_VALUE, 0);

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
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		e2.setaBool(false);
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name !eq testName1");
		List<Instance> r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("testName2", r.get(0).getName());


		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name ne testName1");
		r = el.getResultList(Integer.MAX_VALUE, 0);

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
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setNumber(2);
		e2.setField("A");
		em.persist(e2);

		Instance e3 = new Instance();
		e3.setName("otherName");
		e3.setNumber(3);
		e3.setField("B");
		em.persist(e3);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name not like \"testName%\"");
		List<Instance> r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("otherName", r.get(0).getName());


		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name !like \"testName%\"");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("otherName", r.get(0).getName());
	}


	@Test
	public void testISO8601() throws Exception {
		User u1 = new User();
		u1.setUsername("u1");
		u1.setCreatedDate(Calendar.getInstance());
		em.persist(u1);

		User u2 = new User();
		u2.setUsername("u2");
		Calendar future = Calendar.getInstance();
		future.set(Calendar.YEAR, 3000);
		u2.setCreatedDate(future);
		em.persist(u2);

		Thread.sleep(2);

		System.out.println("createdDate lt "+Calendar.getInstance().getTimeInMillis());
		JpaELFilterImpl el = new JpaELFilterImpl(em, User.class, User.class);
		el.buildExpression("createdDate lt "+Calendar.getInstance().getTimeInMillis());
		List<User> r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getUsername());


		System.out.println("createdDate lt \""+ DatatypeConverter.printDateTime(Calendar.getInstance())+"\"");
		el = new JpaELFilterImpl(em, User.class, User.class);
		el.buildExpression("createdDate lt \""+ DatatypeConverter.printDateTime(Calendar.getInstance())+"\"");
		r = el.getResultList(Integer.MAX_VALUE, 0);
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
		em.persist(u1);

		Instance u2 = new Instance();
		em.persist(u2);

		Thread.sleep(2);

		System.out.println("aDate ne null");
		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aDate ne null");
		List<Instance> r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

		System.out.println("field ne null");
		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("field ne null");
		r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

		System.out.println("number ne null");
		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("number ne null");
		r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(2, r.size());

		System.out.println("autoboxedLong ne null");
		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong ne null");
		r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());


		System.out.println("anEnum ne null");
		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("anEnum ne null");
		r = el.getResultList(Integer.MAX_VALUE, 0);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("u1", r.get(0).getName());

	}

	@Test
	public void hibernatePluralPathTest() {
		CriteriaBuilder build = em.getCriteriaBuilder();
		CriteriaQuery<OneToManyInstance> critQ = build.createQuery(OneToManyInstance.class);
		Root<OneToManyInstance> resultRoot = critQ.from(OneToManyInstance.class);
		Path pluralPath = resultRoot.get("many");
		Bindable shouldBePluralAttribute = pluralPath.getModel();
		assertNotNull(shouldBePluralAttribute);

		assertTrue(shouldBePluralAttribute instanceof PluralAttribute);
	}

	@Test
	public void testPropertyEquals() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name eq \"testName1\"");
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@Test
	public void testBooleanProperty() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setaBool(true);
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setaBool(false);
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aBool eq true");
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}


	@Test
	public void testConvertedProperty() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setConvertedBool(true);
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setConvertedBool(false);
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("convertedBool eq true");
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@Test
	public void testPropertyIn() throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		em.persist(e2);

		Instance e3 = new Instance();
		e3.setName("testName3");
		em.persist(e3);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("name in \"testName1,testName2\"");
		List r = el.getResultList(0, 0);

		assertNotNull(r);
		assertEquals(2, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}
}
