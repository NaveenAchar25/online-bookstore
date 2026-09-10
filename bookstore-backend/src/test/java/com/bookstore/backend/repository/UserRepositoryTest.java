package com.bookstore.backend.repository;

import com.bookstore.backend.model.Role;
import com.bookstore.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles({"dev", "test"})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void savesUserWithRolesAndFindsByEmail() {
        // ROLE_CUSTOMER already exists — seeded by V9__seed_roles.sql as
        // reference data that runs in every profile, including this one.
        // Creating a second row with the same name would violate the
        // unique constraint on roles.name; fetch the seeded one instead.
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
        User user = User.newCustomer("jane@example.com", "hashed-value", "Jane", "Doe", customerRole);

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).extracting(Role::getName).containsExactly("ROLE_CUSTOMER");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        userRepository.save(User.builder()
                .email("dup@example.com").passwordHash("h1")
                .firstName("A").lastName("B").build());
        userRepository.flush();

        assertThatThrownBy(() -> {
            userRepository.save(User.builder()
                    .email("dup@example.com").passwordHash("h2")
                    .firstName("C").lastName("D").build());
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmail_reflectsPersistedState() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();

        userRepository.save(User.builder()
                .email("somebody@example.com").passwordHash("h")
                .firstName("A").lastName("B").build());

        assertThat(userRepository.existsByEmail("somebody@example.com")).isTrue();
    }
}
