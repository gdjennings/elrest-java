/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author grantjennings
 */
public class JpaELFilterImpl<E,R> {
	private static final Pattern AGGREGATE_FUNCTION = Pattern.compile("(?<fn>count|sum|min|max|avg)\\((?<field>.*)\\)");


	private final EntityManager em;
	private final CriteriaBuilder build;
	private final CriteriaQuery<R> rootQ;
	private final AbstractQuery critQ;
	private final Root<E> resultRoot;
	private final Root<E> rootRoot;

	private String filterExpression;
	private List<Selection<?>> multiSelection = new ArrayList<>();
	private boolean isDistinctQuery = false;

	private final Map<Attribute, Join> joins = new HashMap<>();

	static final Pattern CASE_PATTERN = Pattern.compile("(lower|upper)\\((.*)\\)");

	public JpaELFilterImpl(EntityManager em, Class<E> entityClass, Class<R> resultClass) {
		this.em = em;
		build = em.getCriteriaBuilder();
		rootQ = build.createQuery(resultClass);
		rootRoot = rootQ.from(entityClass);

		Type idType = rootRoot.getModel().getIdType();

		if (idType != null && Type.PersistenceType.BASIC.equals(idType.getPersistenceType())) {
			critQ = (AbstractQuery<E>) rootQ;
			resultRoot = rootRoot;
		} else {
			critQ = rootQ.subquery(entityClass);
			critQ.from(entityClass);
			resultRoot = ((Subquery)critQ).correlate(rootRoot);
		}
	}

	/**
	 * @param filter
	 * @throws com.github.gdjennings.elrest.ParseException
	 * @throws IllegalArgumentException
	 */
	public void buildExpression(String filter) throws ParseException {
		this.filterExpression = filter;

		HashMap<String, Object> expressionContext = new HashMap<>();

		List<Predicate> predicates = new ArrayList<>();

		if (filter != null && filter.trim().length() > 0) {

			predicates.add(toPredicate(filter, expressionContext));
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

	private Predicate toPredicate(String filter, Map<String, Object> context) throws ParseException {

		FilterExpression expression = new FilterELParser(filter).parse();
		FilterExpression.Clause rootClause = expression.getClause();
		return buildPredicate(rootClause, context);
	}

	private Predicate buildPredicate(FilterExpression.Clause clause, Map<String, Object> context) throws ParseException {
		if (clause instanceof FilterExpression.CompoundClause) {
			return buildCompoundPredicate((FilterExpression.CompoundClause) clause, context);
		} else if (clause instanceof FilterExpression.SimpleClause) {
			return buildSimplePredicate((FilterExpression.SimpleClause) clause, context);
		} else {
			return null;
		}
	}

	private Predicate buildCompoundPredicate(FilterExpression.CompoundClause clause, Map<String, Object> context) throws ParseException {
		Predicate tempPredicate = null;
		if (clause.operator == FilterExpression.LogicalOperator.AND) {
			tempPredicate = build.and(buildPredicate(clause.left, context), buildPredicate(clause.right, context));
		}
		if (clause.operator == FilterExpression.LogicalOperator.OR) {
			tempPredicate = build.or(buildPredicate(clause.left, context), buildPredicate(clause.right, context));
		}
		return tempPredicate;
	}

	private Predicate buildSimplePredicate(FilterExpression.SimpleClause clause, Map<String, Object> context) throws ParseException {
		Predicate tempPredicate;
		Path propertyRoot = resultRoot;
		EntityType<E> entityType = resultRoot.getModel();
		Path<Date> timestampProperty = null;
		Path<Calendar> calendarProperty = null;
		Attribute propertyRootAttribute = null;
		String leafPropName = null;

		String changeCase = null;
		Matcher caseMatcher = CASE_PATTERN.matcher(clause.identifier);
		if (caseMatcher.matches()) {
			changeCase = caseMatcher.group(1);
			clause.identifier = caseMatcher.group(2);
		}


		String[] lhs = clause.identifier.split("\\.");
		Join joinRoot = null;
		for (String propName : lhs) {
			propertyRoot = propertyRoot.get(propName);
			propertyRootAttribute = (Attribute) propertyRoot.getModel();
			if (propertyRootAttribute == null) {
				propertyRootAttribute = entityType.getAttribute(propName);
			}
			if (Attribute.PersistentAttributeType.MANY_TO_ONE.equals(propertyRootAttribute.getPersistentAttributeType())) {
				SingularAttribute manyToOne = (SingularAttribute) propertyRootAttribute;
				entityType = (EntityType) manyToOne.getType();
				Join joined = joins.get(manyToOne);
				JoinType jt = JoinType.LEFT;
				if (!manyToOne.isOptional()) {
					jt = JoinType.INNER;
				}

				if (joined == null) {
					joined = (joinRoot != null ? joinRoot : resultRoot).join(manyToOne, jt);
					joins.put(manyToOne, joined);
				}
				propertyRoot = joined;
				joinRoot = joined;
			} else if (Attribute.PersistentAttributeType.ONE_TO_MANY.equals(propertyRootAttribute.getPersistentAttributeType())) {
				PluralAttribute oneToMany = (PluralAttribute) propertyRootAttribute;
				entityType = (EntityType) oneToMany.getElementType();
				Join joined = joins.get(oneToMany);
				JoinType jt = JoinType.LEFT;

				if (joined == null) {
					if (SetAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = (joinRoot != null ? joinRoot : resultRoot).joinSet(propName, jt);
					} else if (ListAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = (joinRoot != null ? joinRoot : resultRoot).joinList(propName, jt);
					} else if (CollectionAttribute.class.isAssignableFrom(oneToMany.getClass())) {
						joined = (joinRoot != null ? joinRoot : resultRoot).joinCollection(propName, jt);
					}
					joins.put(oneToMany, joined);
				}
				propertyRoot = joined;
				joinRoot = joined;
			}
			leafPropName = propName;
		}


		Class propertyJavaType = propertyRoot.getJavaType();
		Object discriminatorEntity = "null".equalsIgnoreCase(clause.value) ? null : clause.value;
		if (changeCase != null && !String.class.isAssignableFrom(propertyJavaType)) {
			throw new ParseException(changeCase + " function on non-string type");
		}
		boolean emptySetMatch = discriminatorEntity == null;

		if (discriminatorEntity != null) {
			if (propertyJavaType.isEnum()) {
				discriminatorEntity = Enum.valueOf(propertyJavaType, String.valueOf(discriminatorEntity));
			} else if (Date.class.isAssignableFrom(propertyJavaType)) {
				timestampProperty = propertyRoot.getParentPath().get(leafPropName);
				try {
					long millisSinceEpoch = Long.parseLong(String.valueOf(discriminatorEntity));
					discriminatorEntity = new Date(millisSinceEpoch);
				} catch (NumberFormatException e) {
					discriminatorEntity = DatatypeConverter.parseDateTime(String.valueOf(discriminatorEntity)).getTime();
				}
			} else if (Calendar.class.isAssignableFrom(propertyJavaType)) {
				calendarProperty = propertyRoot.getParentPath().get(leafPropName);
				Calendar tmp = Calendar.getInstance();
				try {
					long millisSinceEpoch = Long.parseLong(String.valueOf(discriminatorEntity));
					tmp.setTimeInMillis(millisSinceEpoch);
				} catch (NumberFormatException e) {
					tmp = DatatypeConverter.parseDateTime(String.valueOf(discriminatorEntity));
				}
				discriminatorEntity = tmp;
			} else if (!EnumSet.of(FilterExpression.ComparisonOperator.IN, FilterExpression.ComparisonOperator.NOT_IN).contains(clause.operator) && Number.class.isAssignableFrom(propertyRoot.getJavaType())
					|| EnumSet.of(FilterExpression.ComparisonOperator.GT, FilterExpression.ComparisonOperator.GTE, FilterExpression.ComparisonOperator.GE, FilterExpression.ComparisonOperator.LT, FilterExpression.ComparisonOperator.LTE, FilterExpression.ComparisonOperator.LE).contains(clause.operator)) {
				try {
					discriminatorEntity = new BigDecimal(String.valueOf(discriminatorEntity));
				} catch (NumberFormatException e) {
					throw new ParseException("Invalid value for numeric property: " + clause.identifier + " caused by: " + e.getMessage());
				}
			} else if (propertyJavaType == boolean.class || propertyJavaType == Boolean.class) {
				discriminatorEntity = Boolean.valueOf(String.valueOf(discriminatorEntity));
			}
		}

		switch (clause.operator) {
			case EQ: {
				if (propertyRootAttribute instanceof PluralAttribute && emptySetMatch) {
					tempPredicate = build.isEmpty(propertyRoot);
				} else if (emptySetMatch) {
					tempPredicate = build.isNull(propertyRoot);
				} else {
					if (changeCase != null) {
						Expression ignoredCase = "upper".equals(changeCase) ? build.upper(propertyRoot) : build.lower(propertyRoot);
						tempPredicate = build.equal(ignoredCase, discriminatorEntity);
					} else {
						tempPredicate = build.equal(propertyRoot, discriminatorEntity);
					}
				}
				break;
			}
			case NE: case NOT_EQ: {
				if (propertyRootAttribute instanceof PluralAttribute && emptySetMatch) {
					tempPredicate = build.isNotEmpty(propertyRoot);
				} else if (emptySetMatch) {
					tempPredicate = build.isNotNull(propertyRoot);
				} else {
					if (changeCase != null) {
						Expression ignoredCase = "upper".equals(changeCase) ? build.upper(propertyRoot) : build.lower(propertyRoot);
						tempPredicate = build.notEqual(ignoredCase, discriminatorEntity);
					} else {
						tempPredicate = build.notEqual(propertyRoot, discriminatorEntity);
					}
				}
				break;
			}
			case IN: case NOT_IN: case NOT_IN2: {
				if (emptySetMatch) {
					if (clause.operator == FilterExpression.ComparisonOperator.IN) {
						tempPredicate = build.notEqual(build.size(propertyRoot), 0);
					} else {
						tempPredicate = build.equal(build.size(propertyRoot), 0);
					}
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
					Predicate inPredicate = (clause.operator == FilterExpression.ComparisonOperator.IN) ? in : in.not();
					Predicate notNullPredicate = build.isNotNull(resultRoot);
					tempPredicate = build.and(notNullPredicate, inPredicate);
				}
				break;
			}
			case LIKE: {
				if (emptySetMatch) {
					tempPredicate = build.equal(build.size(propertyRoot), 0);
				} else {
					if (changeCase != null) {
						Expression ignoredCase = "upper".equals(changeCase) ? build.upper(propertyRoot) : build.lower(propertyRoot);
						tempPredicate = build.like(ignoredCase, (String) discriminatorEntity);
					} else {
						tempPredicate = build.like(propertyRoot, (String) discriminatorEntity);
					}
				}
				break;
			}
			case NOT_LIKE: case NOT_LIKE2: {
				if (emptySetMatch) {
					tempPredicate = build.equal(build.size(propertyRoot), 0);
				} else {
					if (changeCase != null) {
						Expression ignoredCase = "upper".equals(changeCase) ? build.upper(propertyRoot) : build.lower(propertyRoot);
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
				} else if (calendarProperty != null) {
					tempPredicate = build.greaterThan(calendarProperty, (Calendar) discriminatorEntity);
				} else {
					tempPredicate = build.gt(propertyRoot, (Number) discriminatorEntity);
				}
				break;
			}
			case LT: {
				if (timestampProperty != null) {
					tempPredicate = build.lessThan(timestampProperty, (Date) discriminatorEntity);
				} else if (calendarProperty != null) {
					tempPredicate = build.lessThan(calendarProperty, (Calendar) discriminatorEntity);
				} else {
					tempPredicate = build.lt(propertyRoot, (Number) discriminatorEntity);
				}
				break;
			}
			case GTE: case GE: {
				if (timestampProperty != null) {
					tempPredicate = build.greaterThanOrEqualTo(timestampProperty, (Date) discriminatorEntity);
				} else if (calendarProperty != null) {
					tempPredicate = build.greaterThanOrEqualTo(calendarProperty, (Calendar) discriminatorEntity);
				} else {
					tempPredicate = build.greaterThanOrEqualTo(propertyRoot, (BigDecimal) discriminatorEntity);
				}
				break;
			}
			case LTE: case LE: {
				if (timestampProperty != null) {
					tempPredicate = build.lessThanOrEqualTo(timestampProperty, (Date) discriminatorEntity);
				} else if (calendarProperty != null) {
					tempPredicate = build.lessThanOrEqualTo(calendarProperty, (Calendar) discriminatorEntity);
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

	private Path getPath(String field) {
		String[] lhs = field.split("\\.");
		Path propertyRoot = resultRoot;
		Attribute propertyRootAttribute = null;

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
		}
		return propertyRoot;
	}

	public JpaELFilterImpl<E,R> orderBy(String orderByFields) {
		if (orderByFields != null) {
			if (orderByFields.startsWith("-")) {
				rootQ.orderBy(build.desc(getPath(orderByFields.substring(1))));
			} else {
				rootQ.orderBy(build.asc(getPath(orderByFields)));
			}
		}
		return this;
	}

	/**
	 * @param distinctFields
	 * @return
	 * @throws IllegalArgumentException if the field is not part of the entity
	 */
	public JpaELFilterImpl distinct(String... distinctFields) {
		isDistinctQuery = true;
		multiSelection.clear();
		if (distinctFields != null) {
			for (String f : distinctFields) {
				multiSelection.add(resultRoot.get(f));
			}
		}

		return this;
	}

	/**
	 * @param fields
	 * @return
	 * @throws IllegalArgumentException if the field is not part of the entity
	 */
	public JpaELFilterImpl<E,R> selectFields(String[] fields) {
		multiSelection.clear();
		for (String f : fields) {
			multiSelection.add(resultRoot.get(f));
		}
		return this;
	}



	public JpaELFilterImpl<E,R> groupBy(String[] fields, String aggregate, String[] groupBy) {
		multiSelection.clear();

		for (String f : fields) {
			multiSelection.add(getPath(f));
		}

		Matcher m = AGGREGATE_FUNCTION.matcher(aggregate);
		if (m.matches()) {
			switch (m.group("fn")) {
				case "count":
					multiSelection.add(build.count(getPath(m.group("field"))));
					break;
				case "sum":
					multiSelection.add(build.sum(getPath(m.group("field"))));
					break;
				case "min":
					multiSelection.add(build.min(getPath(m.group("field"))));
					break;
				case "max":
					multiSelection.add(build.max(getPath(m.group("field"))));
					break;
				case "avg":
					multiSelection.add(build.avg(getPath(m.group("field"))));
					break;
			}

			List<Expression<?>> groupings = new ArrayList<>();
			Arrays.stream(groupBy).map(f -> getPath(f)).forEach(p -> groupings.add(p));
			critQ.groupBy(groupings);

		}
		return this;
	}


	private TypedQuery<R> prepareQuery(boolean distinct, List<Selection<?>> selections) {
		if (selections.size() == 1) {
			rootQ.select((Selection<? extends R>) selections.get(0));
		} else if (!selections.isEmpty()) {
			rootQ.multiselect(selections);
		} else {
			rootQ.select((Selection<? extends R>) rootRoot);
		}
		if (critQ != rootQ) {
			((Subquery) critQ).select(resultRoot).distinct(false);
			rootQ.where(build.exists((Subquery) critQ));
		}

		rootQ.distinct(distinct);
		return em.createQuery(rootQ);
	}

	public Long count() {
		Type idType = resultRoot.getModel().getIdType();
		TypedQuery countQ;

		if (idType != null && Type.PersistenceType.BASIC.equals(idType.getPersistenceType())) {
			countQ = prepareQuery(false, Arrays.asList(build.countDistinct(resultRoot)));
		} else {
			// hibernate generates invalid SQL for SQLServer and ORACLE when doing countDistinct on entities with composite keys
			Attribute firstPkField = resultRoot.getModel().getIdClassAttributes().iterator().next();

			// SELECT COUNT(t0.KEY1) FROM COMPOSITEKEYINSTANCE t0 WHERE EXISTS (SELECT DISTINCT t1.KEY1 FROM {oj COMPOSITEKEYINSTANCE t1 LEFT OUTER JOIN ONETOMANYCOMPOSITEINSTANCE t2 ON ((t2.key1 = t1.KEY1) AND (t2.key2 = t1.KEY2))} WHERE (((t0.KEY1 = t1.KEY1) AND (t0.KEY2 = t1.KEY2)) AND (t2.ASTRING = ?)))
			((Subquery) critQ).select(resultRoot.get(firstPkField.getName())).distinct(true);
			rootQ.where(build.exists((Subquery) critQ));
			rootQ.select((Selection<? extends R>) build.count(rootRoot.get(firstPkField.getName())));
			countQ = em.createQuery(rootQ);

			((Subquery) critQ).select(resultRoot).distinct(false);
			rootQ.where(build.exists((Subquery) critQ));
		}

		return (Long) countQ.getSingleResult();

	}

	public R getSingleResult() {
		return prepareQuery(true, multiSelection).getSingleResult();
	}

	public List<R> getResultList(int limit, int skip) {
		return getResultList(limit, skip, null);
	}

	public List<R> getResultList(int limit, int skip, EntityGraph graph) {
		return prepareQuery(true, multiSelection).setMaxResults(limit).setFirstResult(skip).setHint("javax.persistence.loadgraph", graph).getResultList();
	}
}
