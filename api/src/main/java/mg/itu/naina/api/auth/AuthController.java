package mg.itu.naina.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public record RegisterRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token, Long userId, String username) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nom d'utilisateur déjà pris");
        }
        User user = new User(req.username(), passwordEncoder.encode(req.password()));
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }
}
