package com.ecommerce.project.address;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.address.dto.AddressDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService{
    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        return  modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList = addressRepository.findByUser_UserId(user.getUserId());
        List<AddressDTO> addressDTOs = addressList.stream().map(address ->
                modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO getAddressById(User user, Long addressId) {
        Address address = findOwnedAddress(user, addressId);
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses(User user) {
        List<Address> addressList = addressRepository.findByUser_UserId(user.getUserId());
        List<AddressDTO> addressDTOs = addressList.stream().map(address ->
                modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO updateAddress(User user, Long addressId, AddressDTO addressDTO) {
        Address currAddress = findOwnedAddress(user, addressId);
        currAddress.setCity(addressDTO.getCity());
        currAddress.setCountry(addressDTO.getCountry());
        currAddress.setStreet(addressDTO.getStreet());
        currAddress.setState(addressDTO.getState());
        currAddress.setPincode(addressDTO.getPincode());
        currAddress.setBuildingName(addressDTO.getBuildingName());
        Address updatedAddress =  addressRepository.save(currAddress);
        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(User user, Long addressId) {
        Address curAddress = findOwnedAddress(user, addressId);
        addressRepository.delete(curAddress);
        return "Address has been removed successfully !!! ";
    }

    private Address findOwnedAddress(User user, Long addressId) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, user.getUserId());
        if (address == null) {
            throw new ResourceNotFoundException("Address", "addressId", addressId);
        }
        return address;
    }
}
