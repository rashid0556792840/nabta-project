package com.nabta.backend.controller;
import com.nabta.backend.dto.CatalogDtos.ProductResponse;
import com.nabta.backend.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog; public CatalogController(CatalogService catalog) { this.catalog=catalog; }
    @GetMapping("/products") public Page<ProductResponse> products(@RequestParam(required=false) String q, @RequestParam(required=false) Long categoryId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return catalog.products(q,categoryId,false,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)),Sort.by("name").ascending())); }
    @GetMapping("/products/{id}") public ProductResponse product(@PathVariable Long id) { return catalog.getProduct(id,false); }
    @GetMapping("/categories") public List<?> categories() { return catalog.categories(false); }
    @GetMapping("/categories/{id}/products") public List<ProductResponse> categoryProducts(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return catalog.productsForCategory(id,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100)))); }
}
