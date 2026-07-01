package org.hibernate.bugs;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;

@Embeddable
public class AffectedUsers implements Serializable {

    @ElementCollection
    @CollectionTable(name = "affected_user_users", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "user_name")
    private Set<String> users = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "affected_user_roles", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "affected_user_groups", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "group_id")
    private Set<Long> groups = new HashSet<>();

    public Set<String> getUsers() {
        return users;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<Long> getGroups() {
        return groups;
    }
}
