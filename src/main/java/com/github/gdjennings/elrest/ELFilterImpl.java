/*
/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import javax.el.ELProcessor;
import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author grantjennings
 */
public class ELFilterImpl {

	private String filter;
	ELProcessor elp = new ELProcessor();

	public ELFilterImpl(String filter) throws ParseException {
		this(filter, new HashMap<>());
	}

	public ELFilterImpl(String filter, Map<String, Object> context) throws ParseException {
		this.filter = filter;
		context.forEach(elp::setValue);

	}


	private void updateExpression(FilterExpression.Clause clause, Object entity) {
		// work out if the criteria is a simple type (eg string) or related entity
		if (clause instanceof FilterExpression.CompoundClause) {
			updateExpression(((FilterExpression.CompoundClause) clause).left, entity);
			updateExpression(((FilterExpression.CompoundClause) clause).right, entity);
		} else if (clause instanceof FilterExpression.SimpleClause) {
			FilterExpression.SimpleClause simpleClause = (FilterExpression.SimpleClause) clause;
			simpleClause.identifier = "entity." + simpleClause.identifier;
			elp.setValue("entity", entity);
			Object prop = elp.eval(simpleClause.identifier);
			if (prop != null && !"null".equals(simpleClause.value)) {
				boolean convertToMillis = false;
				if (Date.class.isAssignableFrom(prop.getClass())) {
					simpleClause.identifier += ".getTime()";
					convertToMillis = true;
				} else if (Calendar.class.isAssignableFrom(prop.getClass())) {
					simpleClause.identifier += ".getTimeInMillis()";
					convertToMillis = true;
				}
				if (convertToMillis) {
					try {
						Long.parseLong(String.valueOf(simpleClause.value));
					} catch (NumberFormatException e) {
						simpleClause.value = String.valueOf(DatatypeConverter.parseDateTime(simpleClause.value).getTimeInMillis());
					}
				}
			}

			simpleClause.value = simpleClause.value.equals("null") ? null : '"' + simpleClause.value + '"';

			switch (simpleClause.operator) {
				case GTE:
					simpleClause.operator = FilterExpression.ComparisonOperator.GE;
					break;
				case LTE:
					simpleClause.operator = FilterExpression.ComparisonOperator.LE;
					break;
				case NOT_EQ:
					simpleClause.operator = FilterExpression.ComparisonOperator.NE;
					break;
				case IN:
					simpleClause.identifier = "(" + simpleClause.identifier + " ne null) and " + simpleClause.value + ".contains(" + simpleClause.identifier + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.EQ;
					simpleClause.value = "true";

					break;
				case NOT_IN: case NOT_IN2:
					simpleClause.identifier = "(" + simpleClause.identifier + " ne null) and " + simpleClause.value + ".contains(" + simpleClause.identifier + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.NE;
					simpleClause.value = "true";

					break;
				case LIKE:
					simpleClause.identifier += ".matches(" + simpleClause.value.replaceAll("%", ".*") + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.EQ;
					simpleClause.value = "true";
					break;
				case NOT_LIKE: case NOT_LIKE2:
					simpleClause.identifier += ".matches(" + simpleClause.value.replaceAll("%", ".*") + ")";
					simpleClause.operator = FilterExpression.ComparisonOperator.NE;
					simpleClause.value = "true";
					break;
			}
		}
	}

	public Object filter(Object entity) throws ParseException {
		if (Collection.class.isAssignableFrom(entity.getClass())) {
			try {
				return ((Collection) entity).stream().filter(e -> {
					try {
						return this.filter(e) != null;
					} catch (ParseException e1) {
						throw new RuntimeException(e1);
					}
				})
						.collect(Collectors.toList());
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ParseException) {
					throw (ParseException)e.getCause();
				} else {
					throw e;
				}
			}
		} else {
			FilterExpression expression = new FilterELParser(filter).parse();
			this.updateExpression(expression.getClause(), entity);
			elp.setValue("entity", entity);
			return (Boolean) elp.eval(expression.toString()) ? entity : null;
		}
	}

}
