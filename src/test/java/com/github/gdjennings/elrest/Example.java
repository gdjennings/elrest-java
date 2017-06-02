/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.gdjennings.elrest;

import com.github.gdjennings.elrest.test.User;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Tuple;

/**
 *
 * @author grantjennings
 */
public class Example {

	EntityManager em;

	Example() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
		em = emf.createEntityManager();
	}
	
	public <T> List<T> filter(Class<T> entityClass, String filter, int limit, int skip) throws ParseException {
		JpaELFilterImpl fx = new JpaELFilterImpl(em, entityClass, entityClass);
		fx.buildExpression(filter);

		return fx.getResultList(limit, skip);
	}

	public List<Tuple> groupBy(Class entityClass, String[] select, String aggregate, String[] groupByFields) {
		JpaELFilterImpl fx = new JpaELFilterImpl(em, entityClass, Tuple.class);
		fx.groupBy(select, aggregate, groupByFields);

		return fx.getResultList(0, 0);
	}


	public static void main(String[] args) throws ParseException {
		// All users with username starting with a
		Example eg = new Example();
		eg.filter(User.class, "username like \"a%\"", 0,0);

		// users created in the last 24 hours
		eg.filter(User.class, "createdDate gt "+(System.currentTimeMillis() - 24*60*60*1000), 0, 0);

		// count the number of users in each postcode
		eg.groupBy(User.class, new String[]{"postCode"}, "count(id)", new String[]{"postCode"});
	}

}
