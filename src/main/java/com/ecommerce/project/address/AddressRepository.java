package com.ecommerce.project.address;

import com.ecommerce.project.address.Address;
import com.ecommerce.project.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Address findByAddressId(Long addressId);
}
