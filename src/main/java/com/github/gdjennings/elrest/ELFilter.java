package com.github.gdjennings.elrest;

import java.util.List;

public abstract class ELFilter<E> {

	protected String filter;
	protected FilterExpression expression;

	protected String[] orderByFields;
	protected String[] selectFields;
	protected String[] distinctFields;
	protected String[] groupByFields;

	public ELFilter<E> filter(String filter) throws ParseException {
		this.filter = filter;
		if (filter != null && filter.trim().length() > 0) {
			this.expression = new FilterELParser(filter).parse();
		}
		return this;
	}

	public ELFilter<E> orderBy(String... orderByFields) {
		this.orderByFields = orderByFields;
		return this;
	}

	public ELFilter<E> distinct(String... distinctFields) {
		this.distinctFields = distinctFields;
		return this;
	}

	public ELFilter<E> select(String... fields) {
		this.selectFields = fields;
		return this;
	}

	public ELFilter<E> groupBy(String... groupByFields){
		this.groupByFields = groupByFields;
		return this;
	}

	public abstract Long count();

	public abstract <T> T getSingleResult(Class<T> resultClass);

	public abstract <T> List<T> getResultList(Class<T> resultClass, int limit, int skip);
}
