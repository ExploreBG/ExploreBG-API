package bg.exploreBG.service;

import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, String> uploadFile(
            MultipartFile file,
            String folderName,
            String cloudinaryId
    ) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folderName);
            options.put("overwrite", true);
            options.put("public_id", cloudinaryId);

            Map uploadResult = this.cloudinary.uploader().upload(file.getBytes(), options);

            String publicIdFolder = (String) uploadResult.get("public_id");
            String[] split = publicIdFolder.split("/");
            String folder = split[0];
            String id = split[1];

            String version = uploadResult.get("version").toString();

            String generatedUrl = this.cloudinary.url().secure(true).publicId(publicIdFolder).version(version).generate();

            Map<String, String> result = new HashMap<>();
            result.put("public_id", id);
            result.put("folder", folder);
            result.put("url", generatedUrl);
            result.put("version", version);
            return result;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
