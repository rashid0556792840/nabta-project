package com.nabta.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public final class UserDataDtos {
    private UserDataDtos() {}
    public record AddressRequest(@NotBlank @Size(max=80) String label, @NotBlank @Size(max=1200) String address,
                                 @NotBlank @Pattern(regexp="^[0-9]{7,20}$", message="Phone must contain 7 to 20 digits only.") @Size(max=20) String phone) {}
    public record AddressResponse(Long id, String label, String address, String phone) {}
    public record FavoriteRequest(@NotNull Long productId) {}
}
