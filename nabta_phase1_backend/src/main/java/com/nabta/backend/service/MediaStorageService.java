package com.nabta.backend.service;
import com.nabta.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
@Service
public class MediaStorageService {
    private final Path uploadDir;
    public MediaStorageService(@Value("${nabta.media.upload-dir:uploads}") String uploadDir) { this.uploadDir=Path.of(uploadDir).toAbsolutePath().normalize(); }
    public String storeImage(MultipartFile file) {
        if(file==null||file.isEmpty()) throw ApiException.badRequest("Choose an image file.");
        if(file.getSize()>5L*1024*1024) throw ApiException.badRequest("Image must be 5 MB or smaller.");
        String type=file.getContentType()==null?"":file.getContentType().toLowerCase(Locale.ROOT);
        String extension=switch(type){case "image/png"->".png";case "image/jpeg"->".jpg";default->throw ApiException.badRequest("Only PNG and JPEG images are supported.");};
        try(ImageInputStream input=ImageIO.createImageInputStream(file.getInputStream())) {
            if(input==null) throw ApiException.badRequest("The uploaded file is not a valid supported image.");
            Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw ApiException.badRequest("The uploaded file is not a valid supported image.");
            ImageReader reader=readers.next();
            try { reader.setInput(input,true,true); int w=reader.getWidth(0),h=reader.getHeight(0); if(w<1||h<1||w>6000||h>6000||(long)w*h>20000000L) throw ApiException.badRequest("Image dimensions are too large."); }
            finally { reader.dispose(); }
        } catch(IOException ex) { throw ApiException.badRequest("Could not read the uploaded image."); }
        String name=UUID.randomUUID().toString().replace("-","")+extension;
        try { Files.createDirectories(uploadDir); Path destination=uploadDir.resolve(name).normalize(); if(!destination.startsWith(uploadDir)) throw ApiException.badRequest("Invalid upload path."); file.transferTo(destination); return "/api/media/"+name; }
        catch(IOException ex) { throw ApiException.badRequest("Image could not be stored. Check the configured persistent upload directory."); }
    }
    public Path resolve(String filename) { if(filename==null||!filename.matches("[a-f0-9]{32}\\.(png|jpg)")) throw ApiException.notFound("Image not found."); Path file=uploadDir.resolve(filename).normalize(); if(!file.startsWith(uploadDir)||!Files.isRegularFile(file)) throw ApiException.notFound("Image not found."); return file; }
}
