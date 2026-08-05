package com.ecommerce.project.auth.dto;

import com.ecommerce.project.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import com.ecommerce.project.address.dto.AddressDTO;
import com.ecommerce.project.cart.dto.CartDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private Set<Role> roles =  new HashSet<>();
    private AddressDTO address;
    private CartDTO cart;
}
