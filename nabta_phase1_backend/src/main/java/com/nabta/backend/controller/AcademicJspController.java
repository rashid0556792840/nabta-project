package com.nabta.backend.controller;

import com.nabta.backend.service.CatalogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AcademicJspController {
    private final CatalogService catalogService;

    public AcademicJspController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/academic/jsp/products")
    public String products(Model model) {
        model.addAttribute("products",
                catalogService.products(null, null, false, PageRequest.of(0, 50)).getContent());
        return "products";
    }
}