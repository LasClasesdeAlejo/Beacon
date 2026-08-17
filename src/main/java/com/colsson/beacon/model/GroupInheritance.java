package com.colsson.beacon.model;

import java.util.Objects;

/**
 * Relación de herencia entre grupos: child hereda de parent.
 */
public final class GroupInheritance {

    private final Group child;
    private final Group parent;

    public GroupInheritance(Group child, Group parent) {
        if (child == null || parent == null) {
            throw new IllegalArgumentException("child y parent no pueden ser nulos");
        }
        if (child.equals(parent)) {
            throw new IllegalArgumentException("Un grupo no puede heredar de sí mismo");
        }
        this.child = child;
        this.parent = parent;
    }

    public Group child() {
        return child;
    }

    public Group parent() {
        return parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupInheritance that)) return false;
        return Objects.equals(child, that.child) && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child, parent);
    }

    @Override
    public String toString() {
        return child.name() + " → " + parent.name();
    }
}
