/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 *
 * @author grantjennings
 */
public class JpaELFilterImpl {

	private final EntityManager em;
	private final CriteriaBuilder build;
	private final CriteriaQuery critQ;
	private final Root resultRoot;

	static final Pattern LOWER_PATTERN = Pattern.compile("lower\\((.*)\\)");

	public JpaELFilterImpl(EntityManager em, Class entityClass, Class resultClass) {
		this.em = em;
		build = em.getCriteriaBuilder();
		if (Tuple.class.isAssignableFrom(resultClass)) {
			critQ = build.createTupleQuery();
		} else {
			critQ = build.createQuery(resultClass);
		}
		resultRoot = critQ.from(entityClass);
	}
	
		/**
	 * @throws com.github.gdjennings.elrest.ParseException
	 * @throws IllegalArgumentException
	 * @param filter
	 */
	public void buildExpression(String filter) throws ParseException {

		Map<Attribute, Join> joins = new HashMap<>();
		
		HashMap<String, Object> expressionContext = new HashMap<>();

		List<Predicate> predicates = new ArrayList<>();

		if (filter != null && filter.trim().length() > 0) {

			predicates.add(toPredicate(filter, expressionContext, joins));
		}

		switch (predicates.size()) {
			case 0:
				break;
			case 1:
				critQ.where(predicates.get(0));
				break;
			default:
				critQ.where(build.and(predicates.toArray(new Predicate[predicates.size()])));
		}


	}

	private Predicate toPredicate(String filter, Map<String, Object> context, Map<Attribute, Join> joins) throws ParseException {

		FilterExpression expression = new FilterELParser(filter).parse();
		FilterExpression.Clause rootClause = expression.getClause();
		return buildPredicate(rootClause, context, joins);
	}

	private Predicate buildPredicate(FilterExpression.Clause clause, Map<String, Object> context, Map<Attribute, Join> joins) throws ParseException {
		if (clause instanceof FilterExpression.CompoundClause) {
			return buildCompoundPredicate((FilterExpression.CompoundClause) clause, context, joins);
		} else if (clause instanceof FilterExpression.SimpleClause) {
			return buildSimplePredicate((FilterExpression.SimpleClause) clause, context, joins);
		} else {
			return null;
		}
	}

	private Predicate buildCompoundPredicate(FilterExpression.CompoundClause clause, Map<String, Object> context, Map<Attribute, Join> joins) throws ParseException {
		Predicate tempPredicate = null;
		if (clause.operator == FilterExpression.LogicalOperator.AND) {
			tempPredicate = build.and(buildPredicate(clause.left, context, joins), buildPredicate(clause.right, context, joins));
		}
		if (clause.operator == FilterExpression.LogicalOperator.OR) {
			tempPredicate = build.or(buildPredicate(clause.left, context, joins), buildPredicate(clause.right, context, joins));
		}
		return tempPredicate;
	}

	private Predicate buildSimplePredicate(FilterExpression.SimpleClause clause, Map<String, Object> context, Map<Attribute, Join> joins) throws ParseException {
		Predicate tempPredicate;
		Path propertyRoot = resultRoot;
		Path<Date> timestampProperty = null;
		Attribute propertyRootAttribute = null;
		String leafPropName = null;

		boolean toLowerCase = false;
		Matcher lowerMatcher = LOWER_PATTERN.matcher(clause.identifier);
		if (lowerMatcher.matches()) {
			clause.identifier = lowerMatcher.group(1);
			toLowerCase = true;
		}
		
		String[] lhs = clause.identifier.split("\\.");
		for (String propName : lhs) {
			propertyRoot = propertyRoot.get(propName);
			propertyRootAttribute = (Attribute) propertyRoot.getModel();
			if (Attribute.PersistentAttributeType.MANY_TO_ONE.equals(propertyRootAttribute.getPersistentAttributeType())) {
				SingularAttribute manyToOne = (SingularAttribute) propertyRootAttribute;
				Join joined = joins.get(manyToOne);
				JoinType jt = JoinType.LEFT;
				if (!manyToOne.isOptional()) {
					jt = JoinType.INNER;
				}

				if (joined == null) {
					joined = resultRoot.join(manyToOne, jt);
					joins.put(manyToOne, joined);
				}
				propertyRoot = joined;
			} else if (Attribute.PersistentAttributeType.ONE_TO_MANY.equals(propertyRootAttribute.getPersistentAttributeType())) {
				PluralAttribute oneToMany = (PluralAttribute) propertyRootAttribute;
				Join joined = joins.get(oneToMany);
				JoinType jt = JoinType.LEFT;

				if (joined == null) {
					if (SetAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = resultRoot.joinSet(propName, jt);
					} else if (ListAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = resultRoot.joinList(propName, jt);
					} else if (CollectionAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = resultRoot.joinCollection(propName, jt);
					}
					joins.put(oneToMany, joined);
				}
				propertyRoot = joined;
			}
			leafPropName = propName;
		}


		Object discriminatorEntity = "null".equalsIgnoreCase(clause.value) ? null : clause.value;
		toLowerCase = String.class.isAssignableFrom(propertyRoot.getJavaType()) && toLowerCase;
		boolean emptySetMatch = discriminatorEntity == null;

		if (propertyRoot.getJavaType().isEnum() && discriminatorEntity != null) {
			discriminatorEntity = Enum.valueOf(propertyRoot.getJavaType(), String.valueOf(discriminatorEntity));
		} else if (!EnumSet.of(FilterExpression.ComparisonOperator.IN, FilterExpression.ComparisonOperator.NOT_IN).contains(clause.operator) && Number.class.isAssignableFrom(propertyRoot.getJavaType()) 
			 || EnumSet.of(FilterExpression.ComparisonOperator.GT, FilterExpression.ComparisonOperator.GTE, FilterExpression.ComparisonOperator.LT, FilterExpression.ComparisonOperator.LTE).contains(clause.operator)) {
			try {
				discriminatorEntity = new BigDecimal(String.valueOf(discriminatorEntity));
			} catch (NumberFormatException e) {
				throw new ParseException("Invalid value for numeric property: "+ clause.identifier + " caused by: " + e.getMessage());
			}
		} else if (Date.class.isAssignableFrom(propertyRoot.getJavaType())) {
			timestampProperty = propertyRoot.getParentPath().<Date>get(leafPropName);
			discriminatorEntity = new Date(Long.valueOf(String.valueOf(discriminatorEntity)));
		}

		switch (clause.operator) {
			case EQ: {
				if (propertyRootAttribute instanceof PluralAttribute && emptySetMatch) {
					tempPredicate = build.isEmpty(propertyRoot);
				} else if (emptySetMatch) {
					tempPredicate = build.isNull(propertyRoot);
				} else {
					if (toLowerCase) {
						tempPredicate = build.equal(build.lower(propertyRoot), discriminatorEntity);
					} else {
						tempPredicate = build.equal(propertyRoot, discriminatorEntity);
					}
				}
				break;
			}
			case NE: {
				if (propertyRootAttribute instanceof PluralAttribute && emptySetMatch) {
					tempPredicate = build.isNotEmpty(propertyRoot);
				} else if (emptySetMatch) {
					tempPredicate = build.isNotNull(propertyRoot);
				} else {
					if (toLowerCase) {
						tempPredicate = build.notEqual(build.lower(propertyRoot), discriminatorEntity);
					} else {
						tempPredicate = build.notEqual(propertyRoot, discriminatorEntity);
					}
				}
				break;
			}
			case IN: {
				if (emptySetMatch) {
					tempPredicate = build.notEqual(build.size(propertyRoot), 0);
				} else {
					Expression<String> exp = propertyRoot;
					CriteriaBuilder.In in = build.in(exp);
					String[] ids = ((String) discriminatorEntity).split(",");
					for (String id : ids) {
						if (Number.class.isAssignableFrom(propertyRoot.getJavaType())) {
							try {
								in.value(new BigDecimal(String.valueOf(id.trim())));
							} catch (NumberFormatException e) {
								//logger.warn("Invalid value for numeric property: {0}", clause.identifier, e);
							}
						} else {
							in.value(id.trim());
						}
					}
					// check property is not null before evaluating in or we get an error
					Predicate inPredicate = in;
					Predicate notNullPredicate = build.isNotNull(resultRoot);
					tempPredicate = build.and(notNullPredicate, inPredicate);
				}
				break;
			}
			case LIKE: {
				if (emptySetMatch) {
					tempPredicate = build.equal(build.size(propertyRoot), 0);
				} else {
					if (toLowerCase) {
						tempPredicate = build.like(build.lower(propertyRoot), (String) discriminatorEntity);
					} else {
						tempPredicate = build.like(propertyRoot, (String) discriminatorEntity);
					}
				}
				break;
			}
			case NOT_LIKE: {
				if (emptySetMatch) {
					tempPredicate = build.equal(build.size(propertyRoot), 0);
				} else {
					if (toLowerCase) {
						tempPredicate = build.notLike(build.lower(propertyRoot), (String) discriminatorEntity);
					} else {
						tempPredicate = build.notLike(propertyRoot, (String) discriminatorEntity);
					}
				}
				break;
			}
			case GT: {
				if (timestampProperty != null) {
					tempPredicate = build.greaterThan(timestampProperty, (Date) discriminatorEntity);
				} else {
					tempPredicate = build.gt(propertyRoot, (Number) discriminatorEntity);
				}
				break;
			}
			case LT: {
				if (timestampProperty != null) {
					tempPredicate = build.lessThan(timestampProperty, (Date) discriminatorEntity);
				} else {
					tempPredicate = build.lt(propertyRoot, (Number) discriminatorEntity);
				}
				break;
			}
			case GTE: {
				if (timestampProperty != null) {
					tempPredicate = build.greaterThanOrEqualTo(timestampProperty, (Date) discriminatorEntity);
				} else {
					tempPredicate = build.greaterThanOrEqualTo(propertyRoot, (BigDecimal) discriminatorEntity);
				}
				break;
			}
			case LTE: {
				if (timestampProperty != null) {
					tempPredicate = build.lessThanOrEqualTo(timestampProperty, (Date) discriminatorEntity);
				} else {
					tempPredicate = build.lessThanOrEqualTo(propertyRoot, (BigDecimal) discriminatorEntity);
				}
				break;
			}

			default:
				tempPredicate = null; // this will trigger an error but it shouldn't happen
		}
		return tempPredicate;
	}

	/**
	 * @param distinctFields
	 * @return 
	 * @throws IllegalArgumentException if the field is not part of the entity
	*/
	public JpaELFilterImpl distinct(String ...distinctFields) {
		if (distinctFields != null) {
			List<Selection> multiSelection = new ArrayList<>();
			for (String f : distinctFields) {
				multiSelection.add(resultRoot.get(f));
			}

			if (distinctFields.length == 1) {
				critQ.select(multiSelection.get(0)).distinct(true);
			} else if (distinctFields.length > 0) {
				critQ.multiselect(multiSelection).distinct(true);
			}
		}

		return this;
	}

	/**
	 * @throws IllegalArgumentException if the field is not part of the entity
	 * @param fields
	 * @return 
	 */
	public JpaELFilterImpl selectFields(String[] fields) {
		List<Selection> multiSelection = new ArrayList<>();

		for (String f : fields) {
			multiSelection.add(resultRoot.get(f));
		}

		critQ.multiselect(multiSelection);

		return this;
	}

	public JpaELFilterImpl orderBy(String orderByFields) {
		if (orderByFields != null) {
			if (orderByFields.startsWith("-")) {
				critQ.orderBy(build.desc(resultRoot.get(orderByFields.substring(1))));
			} else {
				critQ.orderBy(build.asc(resultRoot.get(orderByFields)));	
			}
		}
		return this;
	}
	

	public Long count() {
		critQ.select(build.count(resultRoot));
		return (Long) em.createQuery(critQ).getSingleResult();
	}

	public Object getSingleResult() {
		return em.createQuery(critQ.distinct(true)).getSingleResult();
	}
	public List getResultList(int limit, int skip) {
		return em.createQuery(critQ).setMaxResults(limit).setFirstResult(skip).getResultList();
	}

}