/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;


import com.github.gdjennings.elrest.test.Instance;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.After;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author grantjennings
 */
public class ExpressionImplTest  {
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
		assertEquals(e1.getName(), ((Instance)r.get(0)).getName());
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
		assertEquals(e1.getName(), ((Instance)r.get(0)).getName());
	}

}
