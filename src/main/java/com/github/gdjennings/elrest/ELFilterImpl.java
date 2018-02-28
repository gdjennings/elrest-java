/*
/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import javax.el.ELProcessor;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.stream.Collectors;

/**
 * @author grantjennings
 */
public class ELFilterImpl<E> extends ELFilter<E> {

	private ELProcessor elp = new ELProcessor();
	private Object data;


	public ELFilterImpl(Object data) {
		this(data, new HashMap<>());
	}

	public ELFilterImpl(Object data, Map<String, Object> context){
		this.data = data;
		context.forEach(elp::setValue);
	}


	private FilterExpression.Clause updateExpression(FilterExpression.Clause clause, Object entity) {
		// work out if the criteria is a simple type (eg string) or related entity
		if (clause instanceof FilterExpression.CompoundClause) {
			FilterExpression.CompoundClause compound = new FilterExpression.CompoundClause();
			compound.left = updateExpression(((FilterExpression.CompoundClause) clause).left, entity);
			compound.right = updateExpression(((FilterExpression.CompoundClause) clause).right, entity);
			compound.operator = ((FilterExpression.CompoundClause) clause).operator;
			return compound;
		} else if (clause instanceof FilterExpression.SimpleClause) {
			FilterExpression.SimpleClause simpleClause = (FilterExpression.SimpleClause) clause;

			FilterExpression.SimpleClause rewrittenClause = new FilterExpression.SimpleClause();
			String rewrittenValue = simpleClause.value;
			rewrittenClause.operator = simpleClause.operator;

			rewrittenClause.identifier = "entity." + simpleClause.identifier;
			if (entity != null) {
				ELProcessor getIdELP = new ELProcessor();
				getIdELP.setValue("entity", entity);
				Object prop = getIdELP.eval(rewrittenClause.identifier);
				if (prop != null && !"null".equals(simpleClause.value)) {
					boolean convertToMillis = false;
					if (Date.class.isAssignableFrom(prop.getClass())) {
						rewrittenClause.identifier += ".getTime()";
						convertToMillis = true;
					} else if (Calendar.class.isAssignableFrom(prop.getClass())) {
						rewrittenClause.identifier += ".getTimeInMillis()";
						convertToMillis = true;
					}
					if (convertToMillis) {
						try {
							Long.parseLong(String.valueOf(simpleClause.value));
						} catch (NumberFormatException e) {
							rewrittenValue = String.valueOf(DatatypeConverter.parseDateTime(simpleClause.value).getTimeInMillis());
						}
					}
				}
			}

			rewrittenClause.value = simpleClause.value.equals("null") ? null : '"' + rewrittenValue + '"';

			switch (simpleClause.operator) {
				case GTE:
					rewrittenClause.operator = FilterExpression.ComparisonOperator.GE;
					break;
				case LTE:
					rewrittenClause.operator = FilterExpression.ComparisonOperator.LE;
					break;
				case NOT_EQ:
					rewrittenClause.operator = FilterExpression.ComparisonOperator.NE;
					break;
				case IN:
					rewrittenClause.identifier = "(" + rewrittenClause.identifier + " ne null) and " + rewrittenClause.value + ".contains(" + rewrittenClause.identifier + ")";
					rewrittenClause.operator = FilterExpression.ComparisonOperator.EQ;
					rewrittenClause.value = "true";

					break;
				case NOT_IN: case NOT_IN2:
					rewrittenClause.identifier = "(" + rewrittenClause.identifier + " ne null) and " + rewrittenClause.value + ".contains(" + rewrittenClause.identifier + ")";
					rewrittenClause.operator = FilterExpression.ComparisonOperator.NE;
					rewrittenClause.value = "true";

					break;
				case LIKE:
					rewrittenClause.identifier += ".matches(" + rewrittenClause.value.replaceAll("%", ".*") + ")";
					rewrittenClause.operator = FilterExpression.ComparisonOperator.EQ;
					rewrittenClause.value = "true";
					break;
				case NOT_LIKE: case NOT_LIKE2:
					rewrittenClause.identifier += ".matches(" + rewrittenClause.value.replaceAll("%", ".*") + ")";
					rewrittenClause.operator = FilterExpression.ComparisonOperator.NE;
					rewrittenClause.value = "true";
					break;
			}
			return rewrittenClause;
		} else {
			return null;
		}
	}

	private Object applyFilter(Object entity) {
		if (Collection.class.isAssignableFrom(entity.getClass())) {
			return ((Collection) entity).stream().filter(e -> this.applyFilter(e) != null)
			.collect(Collectors.toList());
		} else {
			if (expression != null) {
				FilterExpression.Clause updatedExpression = this.updateExpression(expression.getClause(), entity);
				elp.setValue("entity", entity);
				return (Boolean) elp.eval(updatedExpression.toString()) ? entity : null;
			} else {
				return Boolean.TRUE;
			}
		}
	}

	@Override
	public Long count() {
		Object filteredData = applyFilter(data);

		if (Collection.class.isAssignableFrom(filteredData.getClass())) {
			return Long.valueOf(((Collection)filteredData).size());
		} else {
			return filteredData != null ? 1L : 0L;
		}
	}

	@Override
	public <T> T getSingleResult(Class<T> resultClass) {
		Object filteredData = applyFilter(data);
		if (filteredData != null && Collection.class.isAssignableFrom(filteredData.getClass())) {
			return ((Collection)filteredData).isEmpty() ? null : (T) ((Collection)filteredData).iterator().next();
		} else {
			return (T) filteredData;
		}
	}

	@Override
	public <T> List<T> getResultList(Class<T> resultClass, int limit, int skip) {
		Object filteredData = applyFilter(data);
		if (Collection.class.isAssignableFrom(filteredData.getClass())) {
			return new ArrayList<T>((Collection)filteredData);
		} else {
			return Arrays.asList((T) filteredData);
		}
	}
}
