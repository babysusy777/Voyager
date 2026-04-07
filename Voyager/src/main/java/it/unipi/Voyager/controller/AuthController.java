package it.unipi.Voyager.controller;

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
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Traveller traveller = authService.loginTraveller(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(traveller);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("login failed" + e.getMessage());
        }
    }

    @PostMapping("/modify-password")
    public ResponseEntity<?> modifyPassword(@RequestBody ModifyPasswordRequest request) {
        try {
            Traveller updatedTraveller = authService.modifyPassword(request);
            return ResponseEntity.ok(updatedTraveller);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
