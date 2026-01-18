package growdy.mumuri.login;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AuthGuard {
    private AuthGuard() {
    }

    public static CustomUserDetails requireUser(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user;
    }
}
