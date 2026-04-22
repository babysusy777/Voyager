package it.unipi.Voyager.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.Voyager.dto.LoginRequest;
import it.unipi.Voyager.dto.ModifyPasswordRequest;
import it.unipi.Voyager.dto.SignupRequest;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//Questo Controller gestisce tutte le chiamate API relative all'autenticazione e agli utenti
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    // Per la registrazione di un nuovo utente
    @Operation(summary = "User signup",
            description = "Registers a new user account given full name, email and password. Returns an error if the email is already in use. Note: host accounts must be registered with a @voyager.com email address.")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            String result = authService.registerTraveller(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Controlla se email e password sono corrette
    @Operation(summary = "Login",
            description = "Authenticates a user by email and password.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Qui usiamo Object perché può tornare o Traveller o Host
            Object user = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Login failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Modify password",
            description = "Updates the password for a Traveller or Host account. Requires the user email and the new password. Returns the updated user profile on success.")
    @PostMapping("/modify-password")
    public ResponseEntity<?> modifyPassword(@RequestBody ModifyPasswordRequest request) {
        try {
            Object updatedUser = authService.modifyPassword(request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
