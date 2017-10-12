# elrest-java
ODATA-like filter expressions to JPA Criteria Queries

Filter expressions are a boolean expressions that follow a subset of javax.el.
```
expression := [(]property *operator* value[)][ and | or expression]
```

## Supported operators:
* **lt, lte, gt, gte**: Less Than/Greater than (or equal to). Must operate on Number or <? extends Date> fields else throws ParseException. Value for date fields should milliseconds.
* **eq, ne, !eq**: Equals/Not Equals
* **in, not in, !in**: value must be quoted comma separated values
* **like, not like, !like**: HQL like operator with "%" as wildcard. Value must be quoted


## Supported Functions
* lower(?) or upper(?) function to perform case insensitive comparisons of strings. e.g. lower(firstName) eq "grant"

## Property Expressions
Expressions can operate across joins (M-1, 1-M and M-M). 
**For example:**
```
     address.postCode eq "90210"  // Users in beverly hills
     roles.name eq "Admin"        // Users who hold the admin role
     contacts.type eq "facebook"  // Users that have given facebook profile
```

## Modifiers:
* **select**: Limits the result to provided field names. Result class is *javax.persistence.Tuple*
* **groupBy**: Execute a group by query. Only returns enumerated fields. Result class is *javax.persistence.Tuple*


## Terminal functions
* **getResultList(limit, skip)**: Returns a list of entities
* **getSingleResult()**: Returns a single entity. Assumes the expression is returning a single entity. Throws exception otherwise
* **count()**: Count of the results that would be returned by getResultList


# USAGE
```java

@Entity
public class User {
	@Id
	String username;

	String firstName;

	String lastName;

	@Temporal
	Calendar createdDate;

	String postCode;
	
	...
}

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

```