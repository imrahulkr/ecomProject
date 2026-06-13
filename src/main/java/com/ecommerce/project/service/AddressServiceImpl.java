package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService{
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        System.out.println("Inside Create Address Class : ");
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        return  modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList = user.getAddresses();
        List<AddressDTO> addressDTOs = addressList.stream().map(address ->
                modelMapper.map(address, AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO getAddressById(User user, Long addressId) {
        //Address address = addressRepository.findByUserAndId(user, addressId);
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
//        Address address = addressRepository.findByAddressId(addressId);
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

        User user =  currAddress.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        Address curAddress = addressRepository.findById(addressId).orElseThrow(() ->
                new ResourceNotFoundException("Address", "addressId", addressId));
        addressRepository.delete(curAddress);

        User user =  curAddress.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));
        userRepository.save(user);

        return "Address has been removed successfully !!! ";
    }



}
