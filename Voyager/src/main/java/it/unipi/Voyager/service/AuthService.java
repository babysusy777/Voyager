package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.ModifyPasswordRequest;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.model.Host;
import it.unipi.Voyager.model.UserRole;
import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private TravellerRepository travellerRepository;
    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public String registerTraveller(String fullName, String email, String password) {
        if (travellerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        if (hostRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        if (isAdminUser(email)) {
            Host newHost= new Host();
            newHost.setFullName(fullName);
            newHost.setEmail(email);
            newHost.setPassword(passwordEncoder.encode(password)); // Password cifrata
            newHost.setRole(UserRole.HOST);
            hostRepository.save(newHost);
            return "Host registered successfully!";
        } else {
            Traveller newTraveller = new Traveller();
            newTraveller.setFullName(fullName);
            newTraveller.setEmail(email);
            newTraveller.setPassword(passwordEncoder.encode(password)); // Password cifrata
            newTraveller.setRole(UserRole.TRAVELLER);
            travellerRepository.save(newTraveller);
            return "Traveller registered successfully!";
        }


    }

    private boolean isAdminUser(String email) {
        return email.endsWith("@voyager.com");
    }

    public Object login(String email, String password) {
        // 1. Cerca tra i Traveller
        Optional<Traveller> travellerOpt = travellerRepository.findByEmail(email);
        if (travellerOpt.isPresent()) {
            Traveller t = travellerOpt.get();
            if (passwordEncoder.matches(password, t.getPassword())) return t;
        }

        // 2. Cerca tra gli Host
        Optional<Host> hostOpt = hostRepository.findByEmail(email);
        if (hostOpt.isPresent()) {
            Host h = hostOpt.get();
            if (passwordEncoder.matches(password, h.getPassword())) return h;
        }

        throw new RuntimeException("Invalid email or password");
    }

    public Object modifyPassword(ModifyPasswordRequest request) {
        // Cerca prima nel Traveller
        Optional<Traveller> tOpt = travellerRepository.findByEmail(request.getEmail());
        if (tOpt.isPresent()) {
            Traveller t = tOpt.get();
            t.setPassword(passwordEncoder.encode(request.getPassword()));
            return travellerRepository.save(t);
        }

        // Se non trovato, cerca nell'Host
        Optional<Host> hOpt = hostRepository.findByEmail(request.getEmail());
        if (hOpt.isPresent()) {
            Host h = hOpt.get();
            h.setPassword(passwordEncoder.encode(request.getPassword()));
            return hostRepository.save(h);
        }

        throw new RuntimeException("User not found in any repository");
    }
}