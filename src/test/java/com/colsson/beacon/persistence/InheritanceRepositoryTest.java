package com.colsson.beacon.persistence;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class InheritanceRepositoryTest {

    private DatabaseManager db;
    private InheritanceRepository repo;
    private GroupRepository groupRepo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);
        repo = new InheritanceRepository(db);
        groupRepo = new GroupRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void addAndGetParents() throws Exception {
        long child = groupRepo.create("MVP+", 20, "");
        long parent = groupRepo.create("MVP", 10, "");

        repo.addInheritance(child, parent);

        var parents = repo.getParentIds(child);
        assertEquals(1, parents.size());
        assertEquals(parent, parents.get(0));
    }

    @Test
    void getChildren() throws Exception {
        long parent = groupRepo.create("MVP", 10, "");
        long child1 = groupRepo.create("MVP+", 20, "");
        long child2 = groupRepo.create("MVP++", 30, "");

        repo.addInheritance(child1, parent);
        repo.addInheritance(child2, parent);

        var children = repo.getChildIds(parent);
        assertEquals(2, children.size());
        assertTrue(children.contains(child1));
        assertTrue(children.contains(child2));
    }

    @Test
    void removeInheritance() throws Exception {
        long child = groupRepo.create("MVP+", 20, "");
        long parent = groupRepo.create("MVP", 10, "");
        repo.addInheritance(child, parent);

        boolean removed = repo.removeInheritance(child, parent);

        assertTrue(removed);
        assertTrue(repo.getParentIds(child).isEmpty());
        assertTrue(repo.getChildIds(parent).isEmpty());
    }

    @Test
    void removeInheritanceReturnsFalseWhenNotExists() throws Exception {
        assertFalse(repo.removeInheritance(1, 2));
    }

    @Test
    void exists() throws Exception {
        long child = groupRepo.create("MVP+", 20, "");
        long parent = groupRepo.create("MVP", 10, "");

        assertFalse(repo.exists(child, parent));

        repo.addInheritance(child, parent);

        assertTrue(repo.exists(child, parent));
    }

    @Test
    void multiLevelInheritance() throws Exception {
        long mvp = groupRepo.create("MVP", 10, "");
        long mvpPlus = groupRepo.create("MVP+", 20, "");
        long mvpElite = groupRepo.create("MVP++", 30, "");

        repo.addInheritance(mvpPlus, mvp);
        repo.addInheritance(mvpElite, mvpPlus);

        // MVP++ parents: [MVP+]
        assertEquals(1, repo.getParentIds(mvpElite).size());
        assertEquals(mvpPlus, repo.getParentIds(mvpElite).get(0));

        // MVP+ parents: [MVP]
        assertEquals(1, repo.getParentIds(mvpPlus).size());
        assertEquals(mvp, repo.getParentIds(mvpPlus).get(0));

        // MVP parents: []
        assertTrue(repo.getParentIds(mvp).isEmpty());
    }

    @Test
    void deleteCascade() throws Exception {
        long child = groupRepo.create("MVP+", 20, "");
        long parent = groupRepo.create("MVP", 10, "");
        repo.addInheritance(child, parent);

        groupRepo.delete(child);

        assertTrue(repo.getParentIds(child).isEmpty());
    }
}
