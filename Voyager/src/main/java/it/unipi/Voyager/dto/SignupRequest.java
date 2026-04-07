package it.unipi.Voyager.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class SignupRequest {
    private String fullName;
    private String email;
    private String password;
    private String gender;
    private int age;
    private String budget;
    private String country;
}