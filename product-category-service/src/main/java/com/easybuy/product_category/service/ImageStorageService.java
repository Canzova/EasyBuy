package com.easybuy.product_category.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface ImageStorageService {
    String uploadImages(MultipartFile files) throws IOException;
}
