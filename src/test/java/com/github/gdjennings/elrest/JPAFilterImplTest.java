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
import javax.persistence.Tuple;
import org.junit.After;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author grantjennings
 */
public class JPAFilterImplTest  {
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
		el.groupBy(new String[] {"field"}, "sum(number)", new String[] {"field"});
		List r = el.getResultList(Integer.MAX_VALUE, 0);
		
		assertNotNull(r);
		assertEquals(1, r.size());
		assertEquals("A", ((Tuple)r.get(0)).get(0));
		assertEquals(3, ((Tuple)r.get(0)).get(1));
	}

}
