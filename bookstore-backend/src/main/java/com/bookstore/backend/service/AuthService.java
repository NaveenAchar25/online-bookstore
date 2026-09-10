package com.bookstore.backend.service;

import com.bookstore.backend.dto.AuthResponse;
import com.bookstore.backend.dto.LoginRequest;
import com.bookstore.backend.dto.RefreshRequest;
import com.bookstore.backend.dto.RegisterRequest;
import com.bookstore.backend.dto.UserSummaryResponse;
import com.bookstore.backend.exception.AccountLockedException;
import com.bookstore.backend.exception.EmailAlreadyExistsException;
import com.bookstore.backend.exception.InvalidCredentialsException;
import com.bookstore.backend.exception.InvalidRefreshTokenException;
import com.bookstore.backend.model.RefreshToken;
import com.bookstore.backend.model.Role;
import com.bookstore.backend.model.User;
import com.bookstore.backend.repository.RefreshTokenRepository;
import com.bookstore.backend.repository.RoleRepository;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Owns account lifecycle and authentication orchestration. What each
 * failure state *means* — a locked account, an expired refresh token —
 * lives on User and RefreshToken themselves
 * this class coordinates the infrastructure those decisions depend on:
 * password hashing, token issuance, persistence, and logging.
 */
@Service
@Transactional
@Slf4j
public class AuthService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserSummaryResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Role customerRole = roleRepository.findByName(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        CUSTOMER_ROLE + " is not seeded — check the reference-data migration"));

        User user = User.newCustomer(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                customerRole);

        User saved = userRepository.save(user);
        log.info("Registered new customer account: {}", normalizedEmail);
        return toSummary(saved);
    }


    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isAccountLocked()) {
            log.warn("Login rejected for locked account: {}", normalizedEmail);
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            log.warn("Failed login attempt {}/5 for account: {}", user.getFailedLoginAttempts(), normalizedEmail);
            throw new InvalidCredentialsException();
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);
        log.info("Successful login: {}", normalizedEmail);
        return issueTokenPair(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!existing.isValid()) {
            log.warn("Rejected reuse of an invalid/expired/revoked refresh token for user {}",
                    existing.getUser().getEmail());
            throw new InvalidRefreshTokenException();
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        log.info("Refreshed token pair for: {}", existing.getUser().getEmail());
        return issueTokenPair(existing.getUser());
    }

    public void logout(RefreshRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            log.info("Logged out (refresh token revoked) for: {}", token.getUser().getEmail());
        });
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_TTL))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    /** High-entropy opaque token — not a JWT. Random bytes, nothing derived from user data. */
    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    private UserSummaryResponse toSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
