package com.colsson.beacon.persistence;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class GroupRepositoryTest {

    private DatabaseManager db;
    private GroupRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);
        repo = new GroupRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void createAndFindByName() throws Exception {
        long id = repo.create("VIP", 10, "Very Important Player");

        var group = repo.findByName("VIP");
        assertTrue(group.isPresent());
        assertEquals(id, group.get().id());
        assertEquals("VIP", group.get().name());
        assertEquals(10, group.get().priority());
        assertEquals("Very Important Player", group.get().description());
    }

    @Test
    void createWithDefaultDescription() throws Exception {
        long id = repo.create("MOD", 50, null);

        var group = repo.findById(id);
        assertTrue(group.isPresent());
        assertEquals("", group.get().description());
    }

    @Test
    void findById() throws Exception {
        long id = repo.create("ADMIN", 100, "Administrator");

        var group = repo.findById(id);
        assertTrue(group.isPresent());
        assertEquals("ADMIN", group.get().name());
    }

    @Test
    void findByIdReturnsEmpty() throws Exception {
        assertTrue(repo.findById(999).isEmpty());
    }

    @Test
    void updateName() throws Exception {
        long id = repo.create("VIP", 10, "");

        repo.updateName(id, "VIP-ELITE");

        var group = repo.findById(id);
        assertTrue(group.isPresent());
        assertEquals("VIP-ELITE", group.get().name());
    }

    @Test
    void updateNamePreservesId() throws Exception {
        long id = repo.create("VIP", 10, "");

        repo.updateName(id, "VIP-ELITE");

        var group = repo.findById(id);
        assertEquals(id, group.get().id());
    }

    @Test
    void updatePriority() throws Exception {
        long id = repo.create("VIP", 10, "");

        repo.updatePriority(id, 25);

        var group = repo.findById(id);
        assertEquals(25, group.get().priority());
    }

    @Test
    void updateDescription() throws Exception {
        long id = repo.create("VIP", 10, "");

        repo.updateDescription(id, "New description");

        var group = repo.findById(id);
        assertEquals("New description", group.get().description());
    }

    @Test
    void delete() throws Exception {
        long id = repo.create("VIP", 10, "");

        repo.delete(id);

        assertTrue(repo.findById(id).isEmpty());
    }

    @Test
    void deleteCascadePermissions() throws Exception {
        long groupId = repo.create("VIP", 10, "");
        var permRepo = new PermissionRepository(db);
        permRepo.setGroupPermission(groupId, "anvil.fly", true);

        repo.delete(groupId);

        assertTrue(permRepo.getGroupPermissions(groupId).isEmpty());
    }

    @Test
    void findAllOrderByPriorityDesc() throws Exception {
        repo.create("default", 0, "");
        repo.create("MOD", 50, "");
        repo.create("ADMIN", 100, "");

        var groups = repo.findAll();
        assertEquals(3, groups.size());
        assertEquals("ADMIN", groups.get(0).name());
        assertEquals("MOD", groups.get(1).name());
        assertEquals("default", groups.get(2).name());
    }

    @Test
    void duplicateNameThrows() throws Exception {
        repo.create("VIP", 10, "");

        assertThrows(Exception.class, () -> repo.create("VIP", 20, ""));
    }
}
