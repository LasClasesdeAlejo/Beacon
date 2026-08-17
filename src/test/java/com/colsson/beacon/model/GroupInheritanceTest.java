package com.colsson.beacon.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupInheritanceTest {

    @Test
    void createValidInheritance() {
        Group child = new Group(1, "MVP+", 20);
        Group parent = new Group(2, "MVP", 10);

        GroupInheritance inh = new GroupInheritance(child, parent);
        assertEquals(child, inh.child());
        assertEquals(parent, inh.parent());
    }

    @Test
    void rejectNullChild() {
        Group parent = new Group(2, "MVP", 10);
        assertThrows(IllegalArgumentException.class,
            () -> new GroupInheritance(null, parent));
    }

    @Test
    void rejectNullParent() {
        Group child = new Group(1, "MVP+", 20);
        assertThrows(IllegalArgumentException.class,
            () -> new GroupInheritance(child, null));
    }

    @Test
    void rejectSelfInheritance() {
        Group g = new Group(1, "VIP", 10);
        assertThrows(IllegalArgumentException.class,
            () -> new GroupInheritance(g, g));
    }

    @Test
    void equalityByChildAndParent() {
        Group child = new Group(1, "MVP+", 20);
        Group parent = new Group(2, "MVP", 10);

        GroupInheritance a = new GroupInheritance(child, parent);
        GroupInheritance b = new GroupInheritance(child, parent);
        assertEquals(a, b);
    }

    @Test
    void toStringRepresentation() {
        Group child = new Group(1, "MVP+", 20);
        Group parent = new Group(2, "MVP", 10);

        GroupInheritance inh = new GroupInheritance(child, parent);
        assertEquals("MVP+ → MVP", inh.toString());
    }
}
