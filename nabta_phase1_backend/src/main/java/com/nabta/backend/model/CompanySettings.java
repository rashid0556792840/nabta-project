package com.nabta.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "company_settings")
public class CompanySettings {
    @Id private Long id = 1L;
    @Column(name = "company_name", nullable = false, length = 150) private String companyName = "Nabta";
    @Column(length = 40) private String phone;
    @Column(name = "whatsapp_number", length = 40) private String whatsappNumber;
    @Column(length = 190) private String email;
    @Column(length = 500) private String address;
    @Column(name = "logo_url", length = 1000) private String logoUrl;
    @Column(name = "x_url", length = 1000) private String xUrl = "https://x.com/";
    @Column(name = "instagram_url", length = 1000) private String instagramUrl = "https://www.instagram.com/";
    @Column(name = "snapchat_url", length = 1000) private String snapchatUrl = "https://www.snapchat.com/";
    @Column(name = "maps_url", length = 1000) private String mapsUrl = "https://www.google.com/maps/search/?api=1&query=Nabta+Food+Company+Madinah";
}
