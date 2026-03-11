package com.e_comerce.backend.service;

import java.util.List;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.exception.ResourceNotFoundException;
import com.e_comerce.backend.model.Cart;
import com.e_comerce.backend.model.CartItem;
import com.e_comerce.backend.model.Product;
import com.e_comerce.backend.payload.dto.CartDTO;
import com.e_comerce.backend.payload.dto.ProductDTO;
import com.e_comerce.backend.repository.CartItemRepository;
import com.e_comerce.backend.repository.CartRepository;
import com.e_comerce.backend.repository.ProductRepository;
import com.e_comerce.backend.util.AuthUtil;

import jakarta.transaction.Transactional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtils;

    @Transactional
    public CartDTO addProductToCart(Long productId, int quantity) {

        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Validate product availability and quantity
        CartItem cartItem = cartItemRepository.findCartItemByCartIdAndProductId(cart.getCartId(), productId);

        if (cartItem != null) {
            throw new APIException("Product already exists in cart");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Requested quantity exceeds available stock");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setCart(cart);
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);
        cart.getCartItems().add(newCartItem);

        // Cart refreshedCart = cartRepository.findById(cart.getCartId())
        // .orElseThrow(() -> new ResourceNotFoundException("Cart", "id",
        // cart.getCartId()));

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream()
                .map(item -> {
                    ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                    map.setQuantity(item.getQuantity());
                    return map;
                });

        cartDTO.setProducts(productDTOStream.toList());

        return cartDTO;
    }

    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if (carts.isEmpty()) {
            throw new APIException("No carts found");
        }
        List<CartDTO> cartDTOs = carts.stream()
                .map(cart -> {
                    CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                    List<CartItem> cartItems = cart.getCartItems();
                    List<ProductDTO> products = cartItems.stream()
                            .map(item -> modelMapper.map(item.getProduct(), ProductDTO.class))
                            .toList();
                    cartDTO.setProducts(products);
                    return cartDTO;
                })
                .toList();
        return cartDTOs;
    }

    public CartDTO getUserCart() {
        Long userId = authUtils.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "userId", userId);
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(item -> {
                    ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
                    productDTO.setQuantity(item.getQuantity());
                    return productDTO;
                })
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByUserId(authUtils.loggedInUserId());
        if (userCart != null) {
            return userCart;
        }

        Cart newCart = new Cart();
        newCart.setTotalPrice(0.0);
        newCart.setUser(authUtils.loggedInUser());
        Cart savedCart = cartRepository.save(newCart);
        return savedCart;
    }
}
