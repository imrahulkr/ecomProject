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
        Address address = null;
        List<Address> addressList = user.getAddresses();
        for(Address curAddress : addressList){
            if(curAddress.getAddressId().equals(addressId)){
                address = curAddress;
                break;
            }
        }

        if(address == null){
            throw new ResourceNotFoundException("Address", "addressId", addressId);
        }
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow( () ->
                new ResourceNotFoundException("Address", "addressId", addressId));
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addressList = addressRepository.findAll();
        List<AddressDTO> addressDTOs = addressList.stream().map(address ->
                modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address currAddress = addressRepository.findById(addressId).orElseThrow(
                () -> new ResourceNotFoundException("Address", "addressId", addressId));
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
    public String deleteAddress(Long addressId) {
        Address curAddress = addressRepository.findById(addressId).orElseThrow(() ->
                new ResourceNotFoundException("Address", "addressId", addressId));
        addressRepository.delete(curAddress);
        return "Address has been removed successfully !!! ";
    }
}
