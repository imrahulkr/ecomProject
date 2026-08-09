package com.ecommerce.project.address;

import com.ecommerce.project.address.Address;
import com.ecommerce.project.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Address findByAddressId(Long addressId);
    List<Address> findByUser_UserId(Long userId);
}
