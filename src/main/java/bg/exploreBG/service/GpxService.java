package bg.exploreBG.service;

import bg.exploreBG.exception.AppException;
import bg.exploreBG.model.dto.GpxUrlDateDto;
import bg.exploreBG.model.entity.GpxEntity;
import bg.exploreBG.model.entity.HikingTrailEntity;
import bg.exploreBG.repository.GpxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class GpxService {

    Logger logger = LoggerFactory.getLogger(GpxService.class);
    private final GpxRepository gpxRepository;
    private final HikingTrailService hikingTrailService;
    private final S3Service s3Service;

    public GpxService(
            GpxRepository gpxRepository,
            HikingTrailService hikingTrailService,
            S3Service s3Service
    ) {
        this.gpxRepository = gpxRepository;
        this.hikingTrailService = hikingTrailService;
        this.s3Service = s3Service;
    }

    public GpxUrlDateDto saveGpxFileIfOwner(
            Long id,
            String folder,
            MultipartFile file,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail =
                this.hikingTrailService.getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        GpxEntity gpxCurrentTrail = currentTrail.getGpxFile();

        if (gpxCurrentTrail != null) {
            throw new AppException("A GPX file already exists for this hiking trail. Please delete it before uploading a new one!",
                    HttpStatus.BAD_REQUEST);
        }

        String awsId = String.valueOf(UUID.randomUUID()).concat(".gpx");

        GpxEntity newGpx = createNewGpxEntity(file, folder, awsId);
        GpxEntity saved = this.gpxRepository.save(newGpx);

        currentTrail.setGpxFile(saved);

        this.hikingTrailService.saveTrailWithoutReturn(currentTrail);

        return new GpxUrlDateDto(saved.getGpxUrl(), saved.getCreationDate());
    }

    @Transactional
    public boolean deleteGpxFileIfOwner(
            Long id,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = this.hikingTrailService.getTrailByIdIfOwner(id, userDetails.getUsername());
        GpxEntity gpxFile = currentTrail.getGpxFile();

        if (gpxFile == null) {
            throw new AppException("Cannot delete a non-existent GPX file.", HttpStatus.BAD_REQUEST);
        }
        logger.info("Gpx file is not null - {}", gpxFile);

        String folder = gpxFile.getFolder();
        String aswId = gpxFile.getCloudId();

        boolean deleted = deleteS3GpxFileWithValidation(folder, aswId);
        logger.info("Deleted s3 - {}", deleted);
        currentTrail.setGpxFile(null);
        this.hikingTrailService.saveTrailWithoutReturn(currentTrail);

        this.gpxRepository.delete(gpxFile);

        return deleted;
    }

    private GpxEntity createNewGpxEntity(
            MultipartFile file,
            String folder,
            String awsId
    ) {
        String url = uploadS3GpxFileWithValidation(folder, awsId, file);

        GpxEntity gpx = new GpxEntity();
        gpx.setCloudId(awsId);
        gpx.setGpxUrl(url);
        gpx.setFolder(folder);
        gpx.setCreationDate(LocalDateTime.now());

        return gpx;
    }

    private String uploadS3GpxFileWithValidation(
            String folder,
            String awsId,
            MultipartFile file
    ) {
        String url = this.s3Service.uploadGpxFile(folder, awsId, file);
        if (url == null) {
            throw new AppException("Invalid gpx url!", HttpStatus.BAD_REQUEST);
        }
        return url;
    }

    private boolean deleteS3GpxFileWithValidation(
            String folder,
            String awsId
    ) {
        boolean deleted = this.s3Service.deleteGpxFile(folder, awsId);

        if (!deleted) {
            throw new AppException("Failed to delete the resource due to an unexpected error on the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return true;
    }
}
