package com.easybuy.product_category.service.implementations;

import com.easybuy.product_category.exceptions.customException.ImageUploadFailedException;
import com.easybuy.product_category.service.ImageStorageService;
import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


@Service
public class ImageKitIOStorageService implements ImageStorageService {

    private final String privateKey;
    private final String folder;

    public ImageKitIOStorageService(@Value("${spring.image-kit.private-url}") String privateKey,
                                    @Value("${spring.image-kit.products-folder}") String folder) {
        this.privateKey = privateKey;
        this.folder = folder;
    }

    @Override
    public String uploadImages(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadFailedException("Image file cannot be empty");
        }

        String resolvedFileName = resolveFileName(file);
        ImageKitClient client = ImageKitOkHttpClient.builder()
                .privateKey(privateKey)
                .build();

       try{
           FileUploadParams params = FileUploadParams.builder()
                   .file(file.getBytes())
                   .folder(folder)
                   .fileName(resolvedFileName)
                   .useUniqueFileName(false)
                   .build();

           FileUploadResponse response = client.files().upload(params);
           return response.url().orElseThrow(()-> new RuntimeException("Image kit does not return a public URL."));
       }
       catch(IOException e){
           throw new ImageUploadFailedException("Image upload failed.");
       }
    }

    private String resolveFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if(originalFilename == null || originalFilename.isBlank()){
            originalFilename = UUID.randomUUID().toString() + "/jpg";
        }

        return originalFilename;
    }
}
