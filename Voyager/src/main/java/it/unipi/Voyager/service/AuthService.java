package it.unipi.Voyager.service;

/*import it.unipi.Voyager.dto.ModifyEmailRequest;
import it.unipi.Voyager.dto.ModifyPasswordRequest;*/
import it.unipi.Voyager.dto.ModifyPasswordRequest;
import it.unipi.Voyager.model.Traveller;
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
    private BCryptPasswordEncoder passwordEncoder;

    public String registerTraveller(String fullName, String email, String password, String gender, int age, String budget, String country) {
        if (travellerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        Traveller newTraveller = new Traveller();
        newTraveller.setFullName(fullName);
        newTraveller.setEmail(email);
        newTraveller.setPassword(passwordEncoder.encode(password)); // Password cifrata
        newTraveller.setAge(age);
        newTraveller.setGender(gender);
        newTraveller.setCountry(country);
        newTraveller.setBudget(budget);

        //COME SI FA HOST/TRAVELLER????????????????????????????
        /*
        if (isAdminUser(email, username)) {
            newUser.setRole(UserRole.ADMINISTRATOR);
        } else {
            newUser.setRole(UserRole.MARKETING_ANALYST);
        }
         */

        travellerRepository.save(newTraveller);
        return "Traveller registered successfully!";
    }

    /*
    private boolean isAdminUser(String email, String username) {
        return email.endsWith("@Voyager.com")
                || username.equalsIgnoreCase("admin");
    }
    */

    public Traveller loginTraveller(String email, String password) {
        Optional<Traveller> userOpt = travellerRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            Traveller traveller = userOpt.get();
            if (passwordEncoder.matches(password, traveller.getPassword())) {
                return traveller;
            }
        }
        throw new RuntimeException("Not valid email or password");
    }
    
    
    public Traveller modifyPassword(ModifyPasswordRequest request) {

        Traveller traveller = travellerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            traveller.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return travellerRepository.save(traveller);
    }
}