package it.unipi.Voyager.dto;

public class ModifyPasswordRequest {
    private String email;   // identificativo
    private String password;   // nuova password (opzionale)

    public String getEmail() { return email; }
    public void setEmail(String username) { this.email = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
