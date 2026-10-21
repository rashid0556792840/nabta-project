package com.nabta.backend.controller;
import com.nabta.backend.service.MediaStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.Map;
@RestController @RequestMapping("/api")
public class MediaController {
    private final MediaStorageService media;
    public MediaController(MediaStorageService media){this.media=media;}
    @PostMapping(value="/admin/media", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Map<String,String> upload(@RequestPart("file") MultipartFile file){return Map.of("url",media.storeImage(file));}
    @GetMapping("/media/{filename}") public ResponseEntity<Resource> image(@PathVariable String filename){Path path=media.resolve(filename); MediaType type=filename.toLowerCase(java.util.Locale.ROOT).endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CACHE_CONTROL,"public, max-age=86400").body(new FileSystemResource(path));}
}
