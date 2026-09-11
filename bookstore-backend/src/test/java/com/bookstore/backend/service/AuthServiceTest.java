package com.bookstore.backend.service;

import com.bookstore.backend.dto.*;
import com.bookstore.backend.exception.*;
import com.bookstore.backend.model.RefreshToken;
import com.bookstore.backend.model.Role;
import com.bookstore.backend.model.User;
import com.bookstore.backend.repository.RefreshTokenRepository;
import com.bookstore.backend.repository.RoleRepository;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PasswordEncoder is used for real (low BCrypt strength, for test speed) —
 * password hashing correctness is exactly what we want to verify, not stub
 * away. What's mocked is the persistence boundary: the repositories.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtService);
        customerRole = Role.builder().id(1L).name("ROLE_CUSTOMER").build();
    }

    // ---------- registration ----------

    @Test
    void register_newEmail_createsCustomerAccountWithHashedPassword() {
        RegisterRequest request = validRegisterRequest();
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder()
                    .id(42L).email(u.getEmail()).passwordHash(u.getPasswordHash())
                    .firstName(u.getFirstName()).lastName(u.getLastName()).roles(u.getRoles())
                    .build();
        });

        UserSummaryResponse result = authService.register(request);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getEmail()).isEqualTo("jane.doe@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("StrongPass1!");
        assertThat(passwordEncoder.matches("StrongPass1!", captor.getValue().getPasswordHash())).isTrue();
        assertThat(captor.getValue().getRoles()).containsExactly(customerRole);
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = validRegisterRequest();
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_correctPassword_returnsTokenPair() {
        User user = activeUser(passwordEncoder.encode("Correct1!"));
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token-value");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        AuthResponse response = authService.login(loginRequest("Correct1!"));

        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_wrongPassword_registersFailedLoginOnTheUser() {
        User user = activeUser(passwordEncoder.encode("Correct1!"));
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("WrongPass1!")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void login_fifthConsecutiveFailure_locksAccount() {
        User user = activeUser(passwordEncoder.encode("Correct1!"));
        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin();
        }
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("WrongPass1!")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.isAccountLocked()).isTrue();
    }

    @Test
    void login_unknownEmail_throwsGenericInvalidCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("whatever1!");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_lockedAccount_rejectsEvenWithCorrectPassword() {
        User user = activeUser(passwordEncoder.encode("Correct1!"));
        for (int i = 0; i < 5; i++) {
            user.registerFailedLogin();
        }
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("Correct1!")))
                .isInstanceOf(AccountLockedException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_successAfterPriorFailures_resetsFailedAttemptCounter() {
        User user = activeUser(passwordEncoder.encode("Correct1!"));
        user.registerFailedLogin();
        user.registerFailedLogin();
        user.registerFailedLogin();
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        authService.login(loginRequest("Correct1!"));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isAccountLocked()).isFalse();
    }

    // ---------- refresh ----------

    @Test
    void refresh_validToken_rotatesAndReturnsNewPair() {
        User user = activeUser("hash");
        RefreshToken stored = RefreshToken.builder()
                .id(1L).user(user)
                .tokenHash("will-not-match-directly-since-hash-is-computed-in-service")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("some-raw-refresh-token-value");

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(stored.isValid()).isFalse(); // revoked as part of rotation
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // old revoked + new issued
    }

    @Test
    void refresh_unknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("does-not-exist");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_revokedToken_throws() {
        User user = activeUser("hash");
        RefreshToken revoked = RefreshToken.builder()
                .id(1L).user(user).tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("stolen-and-replayed-token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_expiredToken_throws() {
        User user = activeUser("hash");
        RefreshToken expired = RefreshToken.builder()
                .id(1L).user(user).tokenHash("hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired-token");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ---------- logout ----------

    @Test
    void logout_existingToken_revokesIt() {
        User user = activeUser("hash");
        RefreshToken token = RefreshToken.builder()
                .id(1L).user(user).tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("some-token");
        authService.logout(request);

        assertThat(token.isValid()).isFalse();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_unknownToken_doesNothingAndDoesNotThrow() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("does-not-exist");

        authService.logout(request); // should not throw
        verify(refreshTokenRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("StrongPass1!");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        return request;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword(password);
        return request;
    }

    private User activeUser(String passwordHash) {
        return User.newCustomer("jane.doe@example.com", passwordHash, "Jane", "Doe", customerRole);
    }
}
