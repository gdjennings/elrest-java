/*
 *  
 * Copyright (c) .Grant Jennings. All rights reserved.  
 * Licensed under the ##LICENSENAME##. See LICENSE file in the project root for full license information.  
*/
package com.github.gdjennings.elrest;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author grantjennings
 */
public class JpaELFilterImpl<E> extends ELFilter<E> {
	private static final Pattern AGGREGATE_FUNCTION = Pattern.compile("(?<fn>count|sum|min|max|avg)\\((?<field>.*)\\)");


	private final EntityManager em;
	private CriteriaBuilder build;
	private Class<E> entityClass;

	static final Pattern CASE_PATTERN = Pattern.compile("(lower|upper)\\((.*)\\)");

	public JpaELFilterImpl(EntityManager em, Class<E> entityClass) {
		this.em = em;
		build = em.getCriteriaBuilder();
		this.entityClass = entityClass;
	}

	private void buildPredicate(AbstractQuery query, Root resultRoot, Map<Attribute, Join> joins) {

		Predicate predicate = null;
		if (expression != null && filter.trim().length() > 0) {
			FilterExpression.Clause rootClause = this.expression.getClause();
			predicate = buildPredicate(rootClause, resultRoot, joins);
		}

		if (predicate != null) {
			query.where(predicate);
		}
	}

	private Predicate buildPredicate(FilterExpression.Clause clause, Root resultRoot, Map<Attribute, Join> joins) {
		if (clause instanceof FilterExpression.CompoundClause) {
			return buildCompoundPredicate((FilterExpression.CompoundClause) clause, resultRoot, joins);
		} else if (clause instanceof FilterExpression.SimpleClause) {
			return buildSimplePredicate((FilterExpression.SimpleClause) clause, resultRoot, joins);
		} else {
			return null;
		}
	}

	private Predicate buildCompoundPredicate(FilterExpression.CompoundClause clause, Root resultRoot, Map<Attribute, Join> joins) {
		Predicate tempPredicate = null;
		if (clause.operator == FilterExpression.LogicalOperator.AND) {
			tempPredicate = build.and(buildPredicate(clause.left, resultRoot, joins), buildPredicate(clause.right, resultRoot, joins));
		}
		if (clause.operator == FilterExpression.LogicalOperator.OR) {
			tempPredicate = build.or(buildPredicate(clause.left, resultRoot, joins), buildPredicate(clause.right, resultRoot, joins));
		}
		return tempPredicate;
	}

	private Predicate buildSimplePredicate(FilterExpression.SimpleClause clause, Root resultRoot, Map<Attribute, Join> joins) {
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
			throw new IllegalArgumentException(changeCase + " function on non-string type");
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
					throw new NumberFormatException("Invalid value for numeric property: " + clause.identifier + " caused by: " + e.getMessage());
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

	private Path getPath(String field, Root resultRoot, Map<Attribute, Join> joins) {
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

	private void prepareQuery(CriteriaQuery selectQ, Root selectRoot, Map<Attribute, Join> joins) {

		buildPredicate(selectQ, selectRoot, joins);

		if (this.selectFields != null && this.selectFields.length > 0) {
			List<Selection<?>> multiSelection = new ArrayList<>();
			for (String f : selectFields) {
				Matcher m = AGGREGATE_FUNCTION.matcher(f);
				if (m.matches()) {
					switch (m.group("fn")) {
						case "count":
							multiSelection.add(build.count(getPath(m.group("field"), selectRoot, joins)));
							break;
						case "sum":
							multiSelection.add(build.sum(getPath(m.group("field"), selectRoot, joins)));
							break;
						case "min":
							multiSelection.add(build.min(getPath(m.group("field"), selectRoot, joins)));
							break;
						case "max":
							multiSelection.add(build.max(getPath(m.group("field"), selectRoot, joins)));
							break;
						case "avg":
							multiSelection.add(build.avg(getPath(m.group("field"), selectRoot, joins)));
							break;
					}
				} else {
					multiSelection.add(selectRoot.get(f));
				}
			}
			selectQ.multiselect(multiSelection);
		} else {
			selectQ.select(selectRoot).distinct(true);
		}
	}

	private void prepareGroupBy(AbstractQuery selectQ, Root selectRoot, Map<Attribute, Join> joins) {
		if (groupByFields != null && groupByFields.length > 0) {
			List<Expression> groupings =
					Arrays.stream(groupByFields).map(f -> getPath(f, selectRoot, joins)).collect(Collectors.toList());
			selectQ.groupBy(groupings);
		}
	}

	private <T> TypedQuery<T> prepareSelect(Class<T> resultClass) {
		Map<Attribute, Join> joins = new HashMap<>();

		CriteriaQuery selectQ = build.createQuery(resultClass);
		Root<E> selectRoot = selectQ.from(this.entityClass);
		prepareQuery(selectQ, selectRoot, joins);

		if (orderByFields != null && orderByFields.length > 0) {
			List<Order> orders = new ArrayList<>();
			for (String o : orderByFields) {

				if (o.startsWith("-")) {
					orders.add(build.desc(getPath(o.substring(1), selectRoot, joins)));
				} else {
					orders.add(build.asc(getPath(o, selectRoot, joins)));
				}
			}
			selectQ.orderBy(orders);
		}

		prepareGroupBy(selectQ, selectRoot, joins);
		return em.createQuery(selectQ);
	}

	public <T> T getSingleResult(Class<T> resultClass) {
		return prepareSelect(resultClass).getSingleResult();
	}

	public <T> List<T> getResultList(Class<T> resultClass, int limit, int skip) {
		return getResultList(resultClass, limit, skip, null);
	}

	public <T> List<T> getResultList(Class<T> resultClass, int limit, int skip, EntityGraph graph) {
		return prepareSelect(resultClass).setMaxResults(limit).setFirstResult(skip).setHint("javax.persistence.loadgraph", graph).getResultList();
	}

	public Long count() {

		Map<Attribute, Join> joins = new HashMap<>();

		CriteriaQuery<Long> countQ = build.createQuery(Long.class);
		Root<E> countRoot = countQ.from(this.entityClass);
		EntityType<E> countType = countRoot.getModel();
		Type idType = countType.getIdType();

		if ((countType.hasSingleIdAttribute() && Type.PersistenceType.BASIC.equals(idType.getPersistenceType())) ||
				!needsCountDistinctWorkaround()) {
			// types with a single basic id or databases that can count distinct multiple columns
			prepareQuery(countQ, countRoot, joins);
			countQ.select(build.countDistinct(countRoot));
			return em.createQuery(countQ).getSingleResult();
		} else {
			final Set idAttributes;
			if (idType == null || Type.PersistenceType.BASIC.equals(idType.getPersistenceType())) {
				idAttributes = countType.getSingularAttributes().stream().filter(SingularAttribute::isId).collect(Collectors.toSet());
			} else {
				idAttributes = countRoot.getModel().getIdClassAttributes();
			}
			Iterator<SingularAttribute<? super E, ?>> pkFields = idAttributes.iterator();
			SingularAttribute first = pkFields.next();

			List<Expression<?>> groupBys = new ArrayList<>();
			while (pkFields.hasNext()) {
				groupBys.add(countRoot.get(pkFields.next().getName()));
			}

			prepareQuery(countQ, countRoot, joins);

			countQ.groupBy(groupBys);

			countQ.select(build.sum(build.countDistinct(countRoot.get(first.getName())))).distinct(false);

			List<Long> counts = em.createQuery(countQ).getResultList();

			return counts.get(0);
		}
	}


	private boolean needsCountDistinctWorkaround() {
		//take from current EntityManager current DB Session
		boolean override = Boolean.getBoolean("com.github.gdjennings.elrest.no_countdistinct");
		if (override) {
			return true;
		}

		EntityManagerFactory emf = em.getEntityManagerFactory();
		Map<String, Object> emfProperties = emf.getProperties();

		String driverClass = (String)emfProperties.get("javax.persistence.jdbc.driver");
		return "oracle.jdbc.OracleDriver".equals(driverClass);
	}

}
