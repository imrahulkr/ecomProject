package com.ecommerce.project.address;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.address.dto.AddressDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AddressController {

    private final AuthUtil authUtil;
    private final AddressService addressService;

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> createAddress(@Validated @RequestBody AddressDTO addressDTO){
        User loggedInUser = authUtil.loggedInUser();
        AddressDTO savedAddressDTO = addressService.createAddress(addressDTO, loggedInUser);
        return  new ResponseEntity<>(savedAddressDTO,  HttpStatus.CREATED);
    }
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses(){
        User loggedInUser = authUtil.loggedInUser();
        List<AddressDTO> addressDTOs = addressService.getAddresses(loggedInUser);
        return new ResponseEntity<>(addressDTOs, HttpStatus.OK);
    }

    @GetMapping("/users/addresses")
    public ResponseEntity<List<AddressDTO>> getUserAddresses(){
        User loggedInUser = authUtil.loggedInUser();
        List<AddressDTO> addressDTOs = addressService.getUserAddresses(loggedInUser);
        return new ResponseEntity<>(addressDTOs, HttpStatus.OK);
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable("addressId") Long addressId){
        User loggedInUser = authUtil.loggedInUser();
        AddressDTO address = addressService.getAddressById(loggedInUser, addressId);
        return new ResponseEntity<>(address, HttpStatus.OK);
    }

    @PutMapping("addresses/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable("addressId") Long addressId, @Valid @RequestBody AddressDTO addressDTO){
        User loggedInUser = authUtil.loggedInUser();
        AddressDTO updatedAddressDTO = addressService.updateAddress(loggedInUser, addressId, addressDTO);
        return new ResponseEntity<>(updatedAddressDTO,  HttpStatus.OK);
    }

    @DeleteMapping("addresses/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable("addressId") Long addressId){
        User loggedInUser = authUtil.loggedInUser();
        String message = addressService.deleteAddress(loggedInUser, addressId);
        return new ResponseEntity<>(message,  HttpStatus.OK);
    }
}
