package com.ecommerce.project.address;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.address.dto.AddressDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface AddressService {
    public AddressDTO createAddress(AddressDTO addressDTO, User user);
    List<AddressDTO> getUserAddresses(User loggedInUser);
    AddressDTO getAddressById(User loggedInUser, Long addressId);
    AddressDTO getAddressById(Long addressId);
    List<AddressDTO> getAddresses();

    AddressDTO updateAddress(Long addressId, @Valid AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}
