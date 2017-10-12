/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

/**
 * @author grantjennings
 */
public class FilterExpression {
	private Clause clause;

	public Clause getClause() {
		return clause;
	}

	public void setClause(Clause clause) {
		this.clause = clause;
	}


	public enum LogicalOperator {
		AND, OR
	}

	public enum ComparisonOperator {
		EQ("eq"),
		NE("ne"),
		NOT_EQ("!eq"),
		LT("lt"),
		LTE("lte"),
		GTE("gte"),
		GT("gt"),
		IN("in"),
		NOT_IN("not in"),
		NOT_IN2("!in"),
		LIKE("like"),
		NOT_LIKE("not like"),
		NOT_LIKE2("!like");

		private String op;

		ComparisonOperator(final String op) {
			this.op = op;
		}

		@Override
		public String toString() {
			return op;
		}

		public static ComparisonOperator fromString(String text) {
			if (text != null) {
				for (ComparisonOperator b : ComparisonOperator.values()) {
					if (text.equalsIgnoreCase(b.op)) {
						return b;
					}
				}
			}
			return null;
		}
	}

	public interface Clause {

	}

	public static class SimpleClause implements Clause {
		public String identifier;
		public ComparisonOperator operator;
		public String value;

		@Override
		public String toString() {
			return identifier + " " + operator + " " + value;
		}
	}


	public static class CompoundClause implements Clause {
		public Clause left;
		public LogicalOperator operator;
		public Clause right;

		@Override
		public String toString() {
			return "(" + left + ") " + operator.toString().toLowerCase() + " (" + right + ")";
		}
	}

}
