/*
/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import java.util.Map;

/**
 * @author grantjennings
 */
public class ELFilterImpl {


	public String buildExpression(String filter, Map<String, Object> context) throws ParseException {
		if (filter == null || filter.trim().length() == 0) {
			return null;
		}
		FilterExpression expression = new FilterELParser(filter).parse();
		updateExpression(expression.getClause(), context);
		return expression.getClause().toString();
	}


	private void updateExpression(FilterExpression.Clause clause, Map<String, Object> context) {
		// work out if the criteria is a simple type (eg string) or related entity
		if (clause instanceof FilterExpression.CompoundClause) {
			updateExpression(((FilterExpression.CompoundClause) clause).left, context);
			updateExpression(((FilterExpression.CompoundClause) clause).right, context);
		} else if (clause instanceof FilterExpression.SimpleClause) {
			FilterExpression.SimpleClause simpleClause = (FilterExpression.SimpleClause) clause;
			simpleClause.identifier = "entity." + simpleClause.identifier;

			simpleClause.value = simpleClause.value.equals("null") ? null : '"' + simpleClause.value + '"';

			switch (simpleClause.operator) {
				case IN:
					simpleClause.identifier = "(" + simpleClause.identifier + " ne null) and " + simpleClause.value + ".contains(" + simpleClause.identifier + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.EQ;
					simpleClause.value = "true";

					break;
				case LIKE:
					simpleClause.identifier += ".matches(" + simpleClause.value.replaceAll("%", ".*") + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.EQ;
					simpleClause.value = "true";
					break;
				case NOT_LIKE:
					simpleClause.identifier += ".matches(" + simpleClause.value.replaceAll("%", ".*") + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.NE;
					simpleClause.value = "true";
					break;
			}
		}
	}

}
