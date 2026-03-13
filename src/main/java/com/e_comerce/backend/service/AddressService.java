package com.e_comerce.backend.service;

import java.util.List;

import com.e_comerce.backend.model.User;
import com.e_comerce.backend.payload.dto.AddressDTO;
import com.e_comerce.backend.payload.response.AddressResponse;

import jakarta.transaction.Transactional;

public interface AddressService {
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO, User user);
    public AddressDTO getAddressById(Long addressId);
    public List<AddressDTO> getUserAddresses(User user);
    public AddressResponse getAddresses(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO, User user);
    public String deleteAddress(Long addressId, User user);
}
