package com.e_comerce.backend.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.model.Address;
import com.e_comerce.backend.model.User;
import com.e_comerce.backend.payload.dto.AddressDTO;
import com.e_comerce.backend.payload.response.AddressResponse;
import com.e_comerce.backend.repository.AddressRepository;
import com.e_comerce.backend.util.AuthUtil;

import jakarta.transaction.Transactional;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Override
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);
        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);
        address.setUser(user);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public AddressResponse getAddresses(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);
        Page<Address> addressPage = addressRepository.findAll(pageDetails);
        List<Address> addresses = addressPage.getContent();
        if (addresses.isEmpty()) {
            throw new APIException("No address found");
        }

        List<AddressDTO> addressDTOs = addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();

        AddressResponse addressResponse = new AddressResponse();
        addressResponse.setContent(addressDTOs);
        addressResponse.setPageNumber(addressPage.getNumber()); 
        addressResponse.setPageSize(addressPage.getSize());
        addressResponse.setTotalElements(addressPage.getTotalElements());
        addressResponse.setTotalPages(addressPage.getTotalPages());
        addressResponse.setIsLast(addressPage.isLast());

        return addressResponse;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new APIException("Address with id '" + addressId + "' not found"));
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addresses = user.getAddresses();
        if (addresses.isEmpty()) {
            throw new APIException("No address found for user with id '" + user.getUserId() + "'");
        }
        return addresses.stream()
            .map(address -> modelMapper.map(address, AddressDTO.class))
            .toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO, User user) {
        Address existingAddress = addressRepository.findById(addressId)
            .orElseThrow(() -> new APIException("Address with id '" + addressId + "' not found"));

        if (!existingAddress.getUser().getUserId().equals(user.getUserId())) {
            throw new APIException("You are not authorized to update this address");
        }

        if (addressDTO.getStreet() != null) existingAddress.setStreet(addressDTO.getStreet());
        if (addressDTO.getCity() != null) existingAddress.setCity(addressDTO.getCity());
        if (addressDTO.getPostalCode() != null) existingAddress.setPostalCode(addressDTO.getPostalCode());
        if (addressDTO.getCountry() != null) existingAddress.setCountry(addressDTO.getCountry());

        Address updatedAddress = addressRepository.save(existingAddress);
        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId, User user) {
        Address existingAddress = addressRepository.findById(addressId)
            .orElseThrow(() -> new APIException("Address with id '" + addressId + "' not found"));

        if (!existingAddress.getUser().getUserId().equals(user.getUserId())) {
            throw new APIException("You are not authorized to delete this address");
        }
        List<Address> userAddresses = user.getAddresses();
        userAddresses.remove(existingAddress);
        user.setAddresses(userAddresses);
        addressRepository.delete(existingAddress);
        return "Address deleted successfully";
    }
}