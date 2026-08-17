package com.colsson.beacon.persistence;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private DatabaseManager db;
    private UserRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        db = TestDatabaseHelper.createInMemoryDb();
        TestDatabaseHelper.createSchema(db);
        repo = new UserRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void createAndFindByUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        repo.create(uuid, "Colsson");

        var user = repo.findByUuid(uuid);
        assertTrue(user.isPresent());
        assertEquals("Colsson", user.get().username());
        assertEquals(uuid, user.get().uuid());
    }

    @Test
    void findByName() throws Exception {
        UUID uuid = UUID.randomUUID();
        repo.create(uuid, "Colsson");

        var user = repo.findByName("Colsson");
        assertTrue(user.isPresent());
        assertEquals(uuid, user.get().uuid());
    }

    @Test
    void findByUuidReturnsEmpty() throws Exception {
        assertTrue(repo.findByUuid(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByNameReturnsEmpty() throws Exception {
        assertTrue(repo.findByName("NonExistent").isEmpty());
    }

    @Test
    void updateUsername() throws Exception {
        UUID uuid = UUID.randomUUID();
        repo.create(uuid, "OldName");

        repo.updateUsername(uuid, "NewName");

        var user = repo.findByUuid(uuid);
        assertTrue(user.isPresent());
        assertEquals("NewName", user.get().username());
    }

    @Test
    void delete() throws Exception {
        UUID uuid = UUID.randomUUID();
        repo.create(uuid, "Colsson");

        repo.delete(uuid);

        assertTrue(repo.findByUuid(uuid).isEmpty());
    }

    @Test
    void findAll() throws Exception {
        repo.create(UUID.randomUUID(), "User1");
        repo.create(UUID.randomUUID(), "User2");
        repo.create(UUID.randomUUID(), "User3");

        var users = repo.findAll();
        assertEquals(3, users.size());
    }
}
