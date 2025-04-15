package md.drivestudio.drivestudio.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;

    private String firstName;
    private String lastName;
    private String phone;
    private String country;
    private String email;
}
