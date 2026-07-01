# Hibernate reproducer: EntityGraph + multiple collection fetch + getSingleResult

Standalone Hibernate ORM test case for [spring-data-jpa #4284](https://github.com/spring-projects/spring-data-jpa/issues/4284), created per maintainer guidance after that issue was closed as **not planned** / **external project**.

Based on [hibernate-test-case-templates/orm/hibernate-orm-7](https://github.com/hibernate/hibernate-test-case-templates/tree/main/orm/hibernate-orm-7) (`ORMUnitTestCase` style).

## Problem

After upgrading to **Hibernate 7.4** (via Spring Boot 4.1), a JPQL query that matches **one** `Request` by primary key fails when a wide `EntityGraph` fetch-joins multiple collections (`@OneToMany`, `@ElementCollection`):

```
org.hibernate.NonUniqueResultException: Query did not return a unique result: 3 results were returned
```

Hibernate generates SQL with multiple collection fetch joins (cartesian product). With 3 `Group` rows the JDBC result set has 3 rows for the same root entity. Hibernate 7.3+ enforces strict `getSingleResult()` semantics on the **raw row count**, not on distinct root entities.

This is **not Spring Data JPA** — the same failure occurs with plain `EntityManager` + `jakarta.persistence.fetchgraph`.

Spring Boot reproducer (broader context, Postgres Testcontainers):  
https://github.com/LordKay-sudo/spring-data-jpa-4284-repro

## Run

Requirements: Java 17+, Maven.

```bash
cd hibernate-entitygraph-nonunique-repro
mvn test
```

Enable SQL logging is on in `hibernate.properties` so you can inspect the cartesian join SQL.

**Note:** On H2, `getResultList()` may sometimes deduplicate roots in memory while SQL still shows cartesian joins. `getSingleResult()` reproduces the failure reliably. Postgres matches production behavior more closely (see Spring reproducer).

## Submit to Hibernate

1. Fork or zip this project.
2. Open a ticket at https://hibernate.atlassian.net/ (project **HHH**).
3. Attach this repo link and the draft below.

### Suggested JIRA title

`getSingleResult() throws NonUniqueResultException when EntityGraph fetch-joins multiple collections for a single root entity`

### Suggested JIRA description

**Environment**

- Hibernate ORM 7.4.1.Final
- Jakarta Persistence 3.x
- H2 (in-memory); also reproduced on PostgreSQL (see linked Spring reproducer)

**Summary**

When applying an `EntityGraph` with `jakarta.persistence.fetchgraph` that fetch-joins multiple collections on a query filtered to a single root entity (`where r.id = :id`), `TypedQuery.getSingleResult()` throws `NonUniqueResultException` because the JDBC result set contains multiple rows (cartesian product from collection fetch joins), even though every row maps to the same `Request` instance.

**Steps to reproduce**

1. Clone https://github.com/LordKay-sudo/hibernate-entitygraph-nonunique-repro (or attach a zip).
2. Run `mvn test`.
3. `EntityGraphMultipleCollectionFetchTest` fails on `fetchGraph_getSingleResult_throwsNonUniqueResultException` — that test documents current behavior; invert assertions if you consider this a regression.

**Expected behavior**

`getSingleResult()` returns the single matching `Request` with collections initialized (behavior we saw before upgrading to Hibernate 7.4 / Spring Boot 4.1).

**Actual behavior**

`NonUniqueResultException: Query did not return a unique result: N results were returned` where N equals the number of child collection rows in the cartesian product (e.g. 3 groups → 3 JDBC rows).

**Additional notes**

- `SELECT DISTINCT` in JPQL does not fix this when fetch joins are driven by the entity graph.
- `getResultStream().findFirst()` works as a workaround.
- Spring Data JPA #4284 was closed as external; reporter confirmed identical behavior via raw `EntityManager`.

**Related links**

- https://github.com/spring-projects/spring-data-jpa/issues/4284
- https://github.com/LordKay-sudo/spring-data-jpa-4284-repro

## Workarounds (application level)

1. Narrow the entity graph; use `Hibernate.initialize()` for remaining collections.
2. Use `getResultStream().findFirst()` instead of `getSingleResult()`.
3. Stay on Spring Boot 4.0.x / Hibernate 7.3 until resolved or refactored.
