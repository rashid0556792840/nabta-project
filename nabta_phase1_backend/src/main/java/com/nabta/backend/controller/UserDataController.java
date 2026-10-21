package com.nabta.backend.controller;
import com.nabta.backend.dto.UserDataDtos.*;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.Product;
import com.nabta.backend.model.UserAddress;
import com.nabta.backend.model.UserFavorite;
import com.nabta.backend.repository.ProductRepository;
import com.nabta.backend.repository.UserAddressRepository;
import com.nabta.backend.repository.UserFavoriteRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/users/me")
public class UserDataController {
    private final UserAddressRepository addresses; private final UserFavoriteRepository favorites; private final ProductRepository products;
    public UserDataController(UserAddressRepository addresses, UserFavoriteRepository favorites, ProductRepository products) { this.addresses=addresses; this.favorites=favorites; this.products=products; }
    @GetMapping("/addresses") public List<AddressResponse> addresses(@AuthenticationPrincipal AppUser user) { return addresses.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(this::addressDto).toList(); }
    @PostMapping("/addresses") public AddressResponse createAddress(@AuthenticationPrincipal AppUser user, @Valid @RequestBody AddressRequest request) { UserAddress a=new UserAddress(); a.setUser(user); a.setLabel(request.label().trim()); a.setAddress(request.address().trim()); a.setPhone(request.phone().trim()); return addressDto(addresses.save(a)); }
    @PutMapping("/addresses/{id}") public AddressResponse updateAddress(@AuthenticationPrincipal AppUser user, @PathVariable Long id, @Valid @RequestBody AddressRequest request) { UserAddress a=addresses.findByIdAndUser_Id(id,user.getId()).orElseThrow(()->ApiException.notFound("Saved address not found.")); a.setLabel(request.label().trim()); a.setAddress(request.address().trim()); a.setPhone(request.phone().trim()); return addressDto(addresses.save(a)); }
    @DeleteMapping("/addresses/{id}") public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal AppUser user, @PathVariable Long id) { UserAddress a=addresses.findByIdAndUser_Id(id,user.getId()).orElseThrow(()->ApiException.notFound("Saved address not found.")); addresses.delete(a); return ResponseEntity.noContent().build(); }
    @GetMapping("/favorites") public List<Long> favoriteProductIds(@AuthenticationPrincipal AppUser user) { return favorites.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(f->f.getProduct().getId()).toList(); }
    @PostMapping("/favorites") public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal AppUser user, @Valid @RequestBody FavoriteRequest request) { Product product=products.findById(request.productId()).orElseThrow(()->ApiException.notFound("Product not found.")); if(!product.isActive()) throw ApiException.badRequest("This product is not currently available."); if(!favorites.existsByUser_IdAndProduct_Id(user.getId(),product.getId())) { UserFavorite f=new UserFavorite(); f.setUser(user); f.setProduct(product); favorites.save(f); } return ResponseEntity.noContent().build(); }
    @DeleteMapping("/favorites") public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal AppUser user, @RequestParam Long productId) { favorites.findByUser_IdAndProduct_Id(user.getId(),productId).ifPresent(favorites::delete); return ResponseEntity.noContent().build(); }
    private AddressResponse addressDto(UserAddress a) { return new AddressResponse(a.getId(),a.getLabel(),a.getAddress(),a.getPhone()); }
}
