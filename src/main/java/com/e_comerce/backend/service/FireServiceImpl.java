package com.e_comerce.backend.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FireServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile image) throws IOException {
        String originalFileName = image.getOriginalFilename();

        // generate unique file name
        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf('.')));
        String filePath = path + File.separator + fileName;
        
        // Check if the directory exists, if not create it
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Upload the file to the server
        Files.copy(image.getInputStream(), Paths.get(filePath));

        return fileName;
    }
}
