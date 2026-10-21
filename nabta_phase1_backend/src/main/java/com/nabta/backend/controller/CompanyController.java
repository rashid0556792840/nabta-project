package com.nabta.backend.controller;
import com.nabta.backend.dto.AdminDtos.CompanyResponse;
import com.nabta.backend.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/company")
public class CompanyController {
    private final AdminService admin; public CompanyController(AdminService admin) { this.admin=admin; }
    @GetMapping public CompanyResponse company() { return admin.company(); }
}
