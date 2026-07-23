package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.dao.UserDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.security.PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.session.UserSession;
import it.unibo.fantasyf1.validation.InputValidator;

import java.util.Objects;

/**
 * U1, login, migrazione degli hash legacy e logout.
 */
public final class AuthenticationService {

    private final TransactionManager transactions;
    private final UserDao users;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessions;

    public AuthenticationService(
        final TransactionManager transactions,
        final UserDao users,
        final PasswordHasher passwordHasher,
        final SessionManager sessions
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.users = Objects.requireNonNull(users);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.sessions = Objects.requireNonNull(sessions);
    }

    public int register(final RegistrationRequest request) {
        Objects.requireNonNull(request, "La richiesta non può essere null");
        final String firstName = limitedRequired(
            request.firstName(),
            "Il nome",
            50
        );
        final String lastName = limitedRequired(
            request.lastName(),
            "Il cognome",
            50
        );
        final String username = limitedRequired(
            request.username(),
            "Lo username",
            50
        );
        final String password = InputValidator.password(request.password());
        final String email = InputValidator.max(
            InputValidator.email(request.email()),
            254,
            "L'email"
        );
        final String phone = InputValidator.phone(request.phone());

        return transactions.inTransaction(connection -> {
            if (users.findByUsername(connection, username).isPresent()) {
                throw new AppException(
                    ErrorCode.DUPLICATE,
                    "Lo username indicato è già in uso."
                );
            }
            if (users.existsEmail(connection, email)) {
                throw new AppException(
                    ErrorCode.DUPLICATE,
                    "L'email indicata è già in uso."
                );
            }
            return users.insert(
                connection,
                firstName,
                lastName,
                username,
                passwordHasher.hash(password),
                email,
                phone
            );
        });
    }

    public UserSession login(
        final String usernameValue,
        final String password
    ) {
        final String username = limitedRequired(
            usernameValue,
            "Lo username",
            50
        );
        if (password == null || password.isEmpty()) {
            throw invalidCredentials();
        }

        final UserDao.UserRow authenticated = transactions.inTransaction(
            connection -> {
                final UserDao.UserRow user = users
                    .findByUsername(connection, username)
                    .orElseThrow(AuthenticationService::invalidCredentials);
                if (!passwordHasher.verify(password, user.passwordHash())) {
                    throw invalidCredentials();
                }
                if (passwordHasher.needsRehash(user.passwordHash())) {
                    users.updatePasswordHash(
                        connection,
                        user.id(),
                        passwordHasher.hash(password)
                    );
                }
                return user;
            }
        );
        return sessions.login(authenticated.id(), authenticated.username());
    }

    public void logout() {
        sessions.logout();
    }

    public boolean isAuthenticated() {
        return sessions.isAuthenticated();
    }

    public SessionManager sessions() {
        return sessions;
    }

    private static String limitedRequired(
        final String value,
        final String label,
        final int maximum
    ) {
        return InputValidator.max(
            InputValidator.required(value, label),
            maximum,
            label
        );
    }

    private static AppException invalidCredentials() {
        return new AppException(
            ErrorCode.INVALID_CREDENTIALS,
            "Username o password non validi."
        );
    }
}
