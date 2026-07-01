# Hibernate reproducer: EntityGraph + multiple collection fetch + getSingleResult

Standalone Hibernate ORM test case for [spring-data-jpa #4284](https://github.com/spring-projects/spring-data-jpa/issues/4284).

Based on [hibernate-test-case-templates/orm/hibernate-orm-7](https://github.com/hibernate/hibernate-test-case-templates/tree/main/orm/hibernate-orm-7) (`ORMUnitTestCase` style).

## Problem

After upgrading to **Hibernate 7.4** (via Spring Boot 4.1), a JPQL query that matches one `Request` by primary key fails when a wide `EntityGraph` fetch-joins multiple collections (`@OneToMany`, `@ElementCollection`):

```
org.hibernate.NonUniqueResultException: Query did not return a unique result: 3 results were returned
```

Hibernate generates SQL with multiple collection fetch joins (cartesian product). With 3 `Group` rows the JDBC result set has 3 rows for the same root entity. Hibernate 7.3+ enforces strict `getSingleResult()` semantics on the raw row count, not on distinct root entities.

The same failure occurs with plain `EntityManager` + `jakarta.persistence.fetchgraph` (not Spring Data JPA).

Spring Boot reproducer (Postgres Testcontainers): https://github.com/LordKay-sudo/spring-data-jpa-4284-repro

## Run

Requirements: Java 17+, Maven.

```bash
mvn test
```

SQL logging is enabled in `hibernate.properties`.

The tests expect `getSingleResult()` to return one initialized root entity. On Hibernate 7.4.1 they fail with `NonUniqueResultException`.

On H2, `getResultList()` may sometimes deduplicate roots in memory while SQL still shows cartesian joins. Postgres matches production behavior more closely (see Spring reproducer).

## Related

- https://github.com/spring-projects/spring-data-jpa/issues/4284
- https://github.com/LordKay-sudo/spring-data-jpa-4284-repro

## Workarounds

1. Narrow the entity graph; use `Hibernate.initialize()` for remaining collections.
2. Use `getResultStream().findFirst()` instead of `getSingleResult()`.
3. Stay on Spring Boot 4.0.x / Hibernate 7.3 until resolved or refactored.
