package org.hibernate.bugs;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproducer for EntityGraph + multiple collection fetch joins on a query that
 * matches one root entity. {@link TypedQuery#getSingleResult()} throws
 * {@link org.hibernate.NonUniqueResultException} in Hibernate 7.4+.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20643">HHH-20643</a>
 * @see <a href="https://github.com/spring-projects/spring-data-jpa/issues/4284">spring-data-jpa #4284</a>
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-20643")
@DomainModel(annotatedClasses = {
        Request.class,
        Payload.class,
        Group.class,
        Rule.class,
        Decision.class
})
@ServiceRegistry(settings = {
        @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
        @Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
})
@SessionFactory
class EntityGraphMultipleCollectionFetchTest {

    private static UUID requestId;

    @BeforeAll
    static void seedData(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            Request request = new Request();
            request.getAffectedUsers().getUsers().add("alice");
            request.getAffectedUsers().getUsers().add("bob");
            request.getAffectedUsers().getRoles().add("admin");
            request.getAffectedUsers().getRoles().add("viewer");
            request.getAffectedUsers().getGroups().add(1L);
            request.getAffectedUsers().getGroups().add(2L);
            request.getAffectedUsers().getGroups().add(3L);

            Payload payload = new Payload();
            payload.setRequest(request);
            request.setPayload(payload);

            for (int g = 0; g < 3; g++) {
                Group group = new Group();
                group.setRequest(request);
                for (int r = 0; r < 2; r++) {
                    Rule rule = new Rule();
                    rule.setName("rule-" + g + "-" + r);
                    rule.setGroup(group);
                    group.getRules().add(rule);
                }
                for (int d = 0; d < 2; d++) {
                    Decision decision = new Decision();
                    decision.setOutcome("outcome-" + g + "-" + d);
                    decision.setGroup(group);
                    group.getDecisions().add(decision);
                }
                request.getGroups().add(group);
            }

            session.persist(request);
            requestId = request.getId();
        });
    }

    @Test
    void fetchGraph_getSingleResult_returnsSingleRootWithInitializedCollections(SessionFactoryScope scope) {
        scope.inEntityManager(entityManager -> {
            Request request = graphQuery(entityManager).getSingleResult();

            assertThat(request.getId()).isEqualTo(requestId);
            assertThat(Hibernate.isInitialized(request.getGroups())).isTrue();
            assertThat(request.getGroups()).hasSize(3);
            request.getGroups().forEach(group -> {
                assertThat(Hibernate.isInitialized(group.getRules())).isTrue();
                assertThat(Hibernate.isInitialized(group.getDecisions())).isTrue();
                assertThat(group.getRules()).hasSize(2);
                assertThat(group.getDecisions()).hasSize(2);
            });
            assertThat(Hibernate.isInitialized(request.getAffectedUsers().getUsers())).isTrue();
            assertThat(Hibernate.isInitialized(request.getAffectedUsers().getRoles())).isTrue();
            assertThat(Hibernate.isInitialized(request.getAffectedUsers().getGroups())).isTrue();
        });
    }

    @Test
    void fetchGraph_withDistinctJpql_getSingleResult_returnsSingleRoot(SessionFactoryScope scope) {
        scope.inEntityManager(entityManager -> {
            EntityGraph<Request> graph = createFetchGraph(entityManager);
            TypedQuery<Request> query = entityManager.createQuery(
                    "select distinct r from Request r where r.id = :id",
                    Request.class
            );
            query.setParameter("id", requestId);
            query.setHint("jakarta.persistence.fetchgraph", graph);

            Request request = query.getSingleResult();
            assertThat(request.getId()).isEqualTo(requestId);
            assertThat(request.getGroups()).hasSize(3);
        });
    }

    private TypedQuery<Request> graphQuery(jakarta.persistence.EntityManager entityManager) {
        EntityGraph<Request> graph = createFetchGraph(entityManager);
        TypedQuery<Request> query = entityManager.createQuery(
                "select r from Request r where r.id = :id",
                Request.class
        );
        query.setParameter("id", requestId);
        query.setHint("jakarta.persistence.fetchgraph", graph);
        return query;
    }

    private EntityGraph<Request> createFetchGraph(jakarta.persistence.EntityManager entityManager) {
        EntityGraph<Request> graph = entityManager.createEntityGraph(Request.class);
        graph.addAttributeNodes("groups", "payload");
        Subgraph<Group> groupsGraph = graph.addSubgraph("groups");
        groupsGraph.addAttributeNodes("decisions", "rules");
        Subgraph<AffectedUsers> affectedUsersGraph = graph.addSubgraph("affectedUsers");
        affectedUsersGraph.addAttributeNodes("users", "roles", "groups");
        return graph;
    }
}
