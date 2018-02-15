/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;


import com.github.gdjennings.elrest.test.CompositeKeyInstance;
import com.github.gdjennings.elrest.test.Instance;
import com.github.gdjennings.elrest.test.OneToManyCompositeInstance;
import com.github.gdjennings.elrest.test.OneToManyInstance;
import com.github.gdjennings.elrest.test.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.persistence.EntityManager;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author grantjennings
 */
public class JPAFilterImplTest {

	EntityManager em;

	@BeforeEach
	public void createEntityManager(TestInfo info) {
		em = Persistence.createEntityManagerFactory(info.getDisplayName()).createEntityManager();
		em.getTransaction().begin();
	}

	@AfterEach
	public void rollback() {
		em.getTransaction().rollback();
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testGroupBy(String provider) throws Exception {

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
		List<Tuple> r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("A", ((Tuple) r.get(0)).get(0));
		long sum = Long.valueOf(r.get(0).get(1).toString());
		assertEquals(3L, sum);
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testOneToManyQuery(String provider) throws ParseException {
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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testIn(String provider) throws Exception {

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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testNotIn(String provider) throws Exception {

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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testNotEq(String provider) throws Exception {

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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testNotLike(String provider) throws Exception {

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


	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testISO8601(String provider) throws Exception {
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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testNotEqualsNull(String provider) throws Exception {
		Instance u1 = new Instance();
		u1.setName("u1");
		u1.setaDate(Calendar.getInstance());
		u1.setAnEnum(Instance.InstanceEnum.TYPE1);
		u1.setNumber(1);
		u1.setaLong(1L << 40);
		u1.setField("notNull");
		em.persist(u1);

		Instance u2 = new Instance();
		u2.setName("u2");
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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	@Disabled
	public void hibernatePluralPathTest(String provider) {
		CriteriaBuilder build = em.getCriteriaBuilder();
		CriteriaQuery<OneToManyInstance> critQ = build.createQuery(OneToManyInstance.class);
		Root<OneToManyInstance> resultRoot = critQ.from(OneToManyInstance.class);
		Path pluralPath = resultRoot.get("many");
		Bindable shouldBePluralAttribute = pluralPath.getModel();
		assertNotNull(shouldBePluralAttribute);

		assertTrue(shouldBePluralAttribute instanceof PluralAttribute);
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testPropertyEquals(String provider) throws Exception {

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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testBooleanProperty(String provider) throws Exception {

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


	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testConvertedProperty(String provider) throws Exception {

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

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testPropertyIn(String provider) throws Exception {

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
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(2, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testPropertyGreaterThanLessThan(String provider) throws Exception {

		Instance e1 = new Instance();
		e1.setName("testName1");
		e1.setaLong(1L);
		em.persist(e1);

		Instance e2 = new Instance();
		e2.setName("testName2");
		e2.setaLong(2L);
		em.persist(e2);

		JpaELFilterImpl el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong gte 2");
		List r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e2.getName(), ((Instance) r.get(0)).getName());

		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong ge 2");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e2.getName(), ((Instance) r.get(0)).getName());

		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong gt 1");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e2.getName(), ((Instance) r.get(0)).getName());

		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong lte 1");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());

		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong le 1");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());

		el = new JpaELFilterImpl(em, Instance.class, Instance.class);
		el.buildExpression("aLong lt 2");
		r = el.getResultList(Integer.MAX_VALUE, 0);

		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals(e1.getName(), ((Instance) r.get(0)).getName());
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testSimpleCount(String provider) throws Exception {

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
		long counted = el.count();

		assertEquals(1, counted);
	}



	@ParameterizedTest(name="{0}")
	@ValueSource(strings = { "hibernate", "eclipselink" })
	public void testJoinedCount(String provider) throws ParseException {
		CompositeKeyInstance i1 = new CompositeKeyInstance();
		i1.setKey1("i1k1");
		i1.setKey2("i1k2");
		i1.setString1("parent");
		em.persist(i1);
		em.flush();

		OneToManyCompositeInstance i2 = new OneToManyCompositeInstance();
		i2.setName("i2");
		i2.setaString("child");
		i2.setComposite(i1);
		em.persist(i2);

		OneToManyCompositeInstance i3 = new OneToManyCompositeInstance();
		i3.setName("i3");
		i3.setaString("child");
		i3.setComposite(i1);
		em.persist(i3);

		i1.getMany().add(i2);
		i1.getMany().add(i3);

		CompositeKeyInstance i4 = new CompositeKeyInstance();
		i4.setKey1("i1k1");
		i4.setKey2("i1k4");
		i4.setString1("parent");
		em.persist(i4);

		OneToManyCompositeInstance i42 = new OneToManyCompositeInstance();
		i42.setName("i42");
		i42.setaString("child");
		i42.setComposite(i4);
		em.persist(i42);

		i4.getMany().add(i42);


		CompositeKeyInstance i5 = new CompositeKeyInstance();
		i5.setKey1("i1k5");
		i5.setKey2("i1k2");
		i5.setString1("parent");
		em.persist(i5);

		em.flush();


		JpaELFilterImpl<CompositeKeyInstance, CompositeKeyInstance> el = new JpaELFilterImpl<>(em, CompositeKeyInstance.class, CompositeKeyInstance.class);
		el.buildExpression("many.aString eq \"child\"");
		List<CompositeKeyInstance> results = el.getResultList(100, 0);
		assertNotNull(results);
		assertEquals(2, results.size());

		long r = el.count();
		assertEquals(2, r);
	}


}
