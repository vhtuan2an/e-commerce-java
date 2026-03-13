package com.e_comerce.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.e_comerce.backend.config.AppConstants;
import com.e_comerce.backend.model.User;
import com.e_comerce.backend.payload.dto.AddressDTO;
import com.e_comerce.backend.payload.response.AddressResponse;
import com.e_comerce.backend.repository.AddressRepository;
import com.e_comerce.backend.service.AddressService;
import com.e_comerce.backend.util.AuthUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AuthUtil authUtil;

    @PostMapping("")
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();
        AddressDTO savedAddressDTO = addressService.createAddress(addressDTO, user);
        return new ResponseEntity<>(savedAddressDTO, HttpStatus.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<AddressResponse> getAddresses(
        @RequestParam (name = "pageNumber", 
            defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, 
            required = false) 
            Integer pageNumber,
        @RequestParam (name = "pageSize", 
            defaultValue = AppConstants.DEFAULT_PAGE_SIZE, 
            required = false) 
            Integer pageSize,
        @RequestParam (name = "sortBy", 
            // defaultValue = AppConstants.SORT_BY_DEFAULT, 
            defaultValue = "addressId",
            required = false) 
            String sortBy,
        @RequestParam (name = "sortOrder", 
            defaultValue = AppConstants.SORT_ORDER_DEFAULT, 
            required = false) 
            String sortOrder
    ) {
        AddressResponse addressResponse = addressService.getAddresses(pageNumber, pageSize, sortBy, sortOrder);   
        return new ResponseEntity<>(addressResponse, HttpStatus.OK);
    }

    @GetMapping("{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId) {
        AddressDTO addressDTO = addressService.getAddressById(addressId);
        return new ResponseEntity<>(addressDTO, HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<List<AddressDTO>> getUserAddresses() {
        User user = authUtil.loggedInUser();
        List<AddressDTO> addressDTOs = addressService.getUserAddresses(user);
        return new ResponseEntity<List<AddressDTO>>(addressDTOs, HttpStatus.OK);
    }

    @PutMapping("{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(
        @PathVariable Long addressId, 
        @Valid @RequestBody AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();
        AddressDTO updatedAddressDTO = addressService.updateAddress(addressId, addressDTO, user);
        return new ResponseEntity<>(updatedAddressDTO, HttpStatus.OK);
    }

    @DeleteMapping("{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        User user = authUtil.loggedInUser();
        String response = addressService.deleteAddress(addressId, user);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
