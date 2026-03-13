package com.e_comerce.backend.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.exception.ResourceNotFoundException;
import com.e_comerce.backend.model.Cart;
import com.e_comerce.backend.model.CartItem;
import com.e_comerce.backend.model.Product;
import com.e_comerce.backend.payload.dto.CartDTO;
import com.e_comerce.backend.payload.dto.CartItemDTO;
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

        return toCartDTO(cart);
    }

    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if (carts.isEmpty()) {
            throw new APIException("No carts found");
        }
        return carts.stream().map(this::toCartDTO).toList();
    }

    public CartDTO getUserCart() {
        Long userId = authUtils.loggedInUserId();
        Cart cart = cartRepository.findCartByUserId(userId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "userId", userId);
        }

        return toCartDTO(cart);
    }

    @Transactional
    public CartDTO updateProductQuantity(Long productId, int quantity) {
        Cart cart = cartRepository.findCartByUserId(authUtils.loggedInUserId());
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "userId", authUtils.loggedInUserId());
        }

        Product product = productRepository.findById(productId) 
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        CartItem cartItem = cartItemRepository.findCartItemByCartIdAndProductId(cart.getCartId(), productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        // Caculate delta quantity and price
        int delta = quantity - cartItem.getQuantity();

        if (delta > 0 && product.getQuantity() < delta) {
            throw new APIException("Requested quantity exceeds available stock");
        }

        product.setQuantity(product.getQuantity() - delta);
        productRepository.save(product);

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        cart.setTotalPrice(cart.getTotalPrice() + delta * cartItem.getProductPrice());
        cartRepository.save(cart);

        return toCartDTO(cart);
    }

    private CartDTO toCartDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItemDTO> cartItemDTOs = cart.getCartItems().stream()
                .map(item -> {
                    CartItemDTO cartItemDTO = new CartItemDTO();
                    cartItemDTO.setCartItemId(item.getCartItemId());
                    cartItemDTO.setProduct(modelMapper.map(item.getProduct(), ProductDTO.class));
                    cartItemDTO.setQuantity(item.getQuantity());
                    cartItemDTO.setDiscount(item.getDiscount());
                    cartItemDTO.setPrice(item.getProductPrice());
                    return cartItemDTO;
                })
                .toList();
        cartDTO.setCartItems(cartItemDTOs);
        return cartDTO;
    }

    @Transactional
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "id", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByCartIdAndProductId(cartId, productId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("CartItem", "productId", productId);
        }

        Product product = cartItem.getProduct();
        int quantity = cartItem.getQuantity();

        // Update product stock
        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);

        // Remove cart item
        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        // Update cart total price
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * quantity));
        cartRepository.save(cart);

        return "Product removed from cart successfully";
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
