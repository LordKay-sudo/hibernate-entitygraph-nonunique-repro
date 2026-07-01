package org.hibernate.bugs;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "request")
public class Request implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false, updatable = false)
    private UUID id;

    @OneToOne(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @PrimaryKeyJoinColumn
    private Payload payload;

    @OneToMany(mappedBy = "request", orphanRemoval = true, cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private Set<Group> groups = new LinkedHashSet<>();

    @Embedded
    private AffectedUsers affectedUsers = new AffectedUsers();

    public UUID getId() {
        return id;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public AffectedUsers getAffectedUsers() {
        return affectedUsers;
    }

    public void setAffectedUsers(AffectedUsers affectedUsers) {
        this.affectedUsers = affectedUsers;
    }
}
