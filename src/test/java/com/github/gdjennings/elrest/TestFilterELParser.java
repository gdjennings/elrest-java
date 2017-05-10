/*
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author grantjennings
 */
public class TestFilterELParser {
	
	public TestFilterELParser() {
	}

	@Test
	public void testUQLLogical() throws ParseException {
		FilterELParser p = new FilterELParser("(a eq b) and (c ne d)");
		FilterExpression fe = p.parse();
		assertNotNull(fe);
		assertTrue(fe.getClause() instanceof FilterExpression.CompoundClause);
		
		FilterExpression.CompoundClause clause1 = (FilterExpression.CompoundClause)fe.getClause();
		assertTrue(clause1.left instanceof FilterExpression.SimpleClause);
		assertTrue(clause1.right instanceof FilterExpression.SimpleClause);
		assertEquals(FilterExpression.LogicalOperator.AND , clause1.operator);

		assertEquals("a", ((FilterExpression.SimpleClause)clause1.left).identifier);
		assertEquals("b", ((FilterExpression.SimpleClause)clause1.left).value);
		assertEquals(FilterExpression.ComparisonOperator.EQ, ((FilterExpression.SimpleClause)clause1.left).operator);

		assertEquals("c", ((FilterExpression.SimpleClause)clause1.right).identifier);
		assertEquals("d", ((FilterExpression.SimpleClause)clause1.right).value);
		assertEquals(FilterExpression.ComparisonOperator.NE, ((FilterExpression.SimpleClause)clause1.right).operator);
	}
	
	
	@Test
	public void testUQLComplexLogical() throws ParseException {
		FilterELParser p = new FilterELParser("(a eq b) and ((c ne d) or (e gt f))");
		FilterExpression fe = p.parse();
		assertNotNull(fe);
		assertTrue(fe.getClause() instanceof FilterExpression.CompoundClause);
		
		FilterExpression.CompoundClause clause1 = (FilterExpression.CompoundClause)fe.getClause();
		assertTrue(clause1.left instanceof FilterExpression.SimpleClause);
		assertTrue(clause1.right instanceof FilterExpression.CompoundClause);
		assertEquals(FilterExpression.LogicalOperator.AND , clause1.operator);

		assertEquals("a", ((FilterExpression.SimpleClause)clause1.left).identifier);
		assertEquals("b", ((FilterExpression.SimpleClause)clause1.left).value);
		assertEquals(FilterExpression.ComparisonOperator.EQ, ((FilterExpression.SimpleClause)clause1.left).operator);

		FilterExpression.CompoundClause clause2 = (FilterExpression.CompoundClause)clause1.right;
		assertTrue(clause2.left instanceof FilterExpression.SimpleClause);
		assertTrue(clause2.right instanceof FilterExpression.SimpleClause);
		
		assertEquals("c", ((FilterExpression.SimpleClause)clause2.left).identifier);
		assertEquals("d", ((FilterExpression.SimpleClause)clause2.left).value);
		assertEquals(FilterExpression.ComparisonOperator.NE, ((FilterExpression.SimpleClause)clause2.left).operator);

		assertEquals("e", ((FilterExpression.SimpleClause)clause2.right).identifier);
		assertEquals("f", ((FilterExpression.SimpleClause)clause2.right).value);
		assertEquals(FilterExpression.ComparisonOperator.GT, ((FilterExpression.SimpleClause)clause2.right).operator);

	}
	
	@Test
	public void testUQLComplexLogicalMultiple() throws ParseException {
		FilterELParser p = new FilterELParser("a eq b and c ne d and e gt f");
		FilterExpression fe = p.parse();
		assertNotNull(fe);
		assertTrue(fe.getClause() instanceof FilterExpression.CompoundClause);
		
		FilterExpression.CompoundClause clause1 = (FilterExpression.CompoundClause)fe.getClause();
		assertTrue(clause1.left instanceof FilterExpression.CompoundClause);
		assertEquals(FilterExpression.LogicalOperator.AND , clause1.operator);

		FilterExpression.CompoundClause clause11 = (FilterExpression.CompoundClause)clause1.left;
		assertTrue(clause11.left instanceof FilterExpression.SimpleClause);
		assertEquals("a", ((FilterExpression.SimpleClause)clause11.left).identifier);
		assertEquals("b", ((FilterExpression.SimpleClause)clause11.left).value);
		assertEquals(FilterExpression.ComparisonOperator.EQ, ((FilterExpression.SimpleClause)clause11.left).operator);


		assertTrue(clause11.right instanceof FilterExpression.SimpleClause);
		assertEquals("c", ((FilterExpression.SimpleClause)clause11.right).identifier);
		assertEquals("d", ((FilterExpression.SimpleClause)clause11.right).value);
		assertEquals(FilterExpression.ComparisonOperator.NE, ((FilterExpression.SimpleClause)clause11.right).operator);

		assertTrue(clause1.right instanceof FilterExpression.SimpleClause);
		assertEquals("e", ((FilterExpression.SimpleClause)clause1.right).identifier);
		assertEquals("f", ((FilterExpression.SimpleClause)clause1.right).value);
		assertEquals(FilterExpression.ComparisonOperator.GT, ((FilterExpression.SimpleClause)clause1.right).operator);

	}
	
}
