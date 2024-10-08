package bg.exploreBG.service;

import bg.exploreBG.exception.AppException;
import bg.exploreBG.model.dto.LikeBooleanDto;
import bg.exploreBG.model.dto.ReviewBooleanDto;
import bg.exploreBG.model.dto.accommodation.AccommodationBasicDto;
import bg.exploreBG.model.dto.accommodation.AccommodationWrapperDto;
import bg.exploreBG.model.dto.accommodation.single.AccommodationIdDto;
import bg.exploreBG.model.dto.comment.CommentDto;
import bg.exploreBG.model.dto.comment.validate.CommentCreateDto;
import bg.exploreBG.model.dto.destination.DestinationBasicDto;
import bg.exploreBG.model.dto.destination.DestinationWrapperDto;
import bg.exploreBG.model.dto.destination.single.DestinationIdDto;
import bg.exploreBG.model.dto.hikingTrail.*;
import bg.exploreBG.model.dto.hikingTrail.single.*;
import bg.exploreBG.model.dto.hikingTrail.validate.*;
import bg.exploreBG.model.dto.image.single.ImageUrlDto;
import bg.exploreBG.model.dto.image.validate.ImageMainUpdateDto;
import bg.exploreBG.model.dto.user.single.UserIdDto;
import bg.exploreBG.model.entity.*;
import bg.exploreBG.model.enums.StatusEnum;
import bg.exploreBG.model.enums.SuitableForEnum;
import bg.exploreBG.model.enums.SuperUserReviewStatusEnum;
import bg.exploreBG.model.mapper.CommentMapper;
import bg.exploreBG.model.mapper.HikingTrailMapper;
import bg.exploreBG.model.user.ExploreBgUserDetails;
import bg.exploreBG.repository.HikingTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class HikingTrailService {

    private static final Logger logger = LoggerFactory.getLogger(HikingTrailService.class);
    private final HikingTrailRepository hikingTrailRepository;
    private final HikingTrailMapper hikingTrailMapper;
    private final CommentMapper commentMapper;
    private final UserService userService;
    private final DestinationService destinationService;
    private final AccommodationService accommodationService;
    private final CommentService commentService;

    public HikingTrailService(
            HikingTrailRepository hikingTrailRepository,
            HikingTrailMapper hikingTrailMapper,
            CommentMapper commentMapper,
            UserService userService,
            DestinationService destinationService,
            AccommodationService accommodationService,
            CommentService commentService
    ) {
        this.hikingTrailRepository = hikingTrailRepository;
        this.hikingTrailMapper = hikingTrailMapper;
        this.commentMapper = commentMapper;
        this.userService = userService;
        this.destinationService = destinationService;
        this.accommodationService = accommodationService;
        this.commentService = commentService;
    }

    public List<HikingTrailBasicDto> getRandomNumOfHikingTrails(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return this.hikingTrailRepository.findRandomApprovedTrails(pageable);
    }

    public List<HikingTrailBasicLikesDto> getRandomNumOfHikingTrailsWithLikes(
            int limit,
            UserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(0, limit);
        return this.hikingTrailRepository.
                findRandomApprovedTrailsWithLikes(userDetails.getUsername(), pageable);
    }

    public HikingTrailDetailsDto getHikingTrail(Long id) {
        HikingTrailEntity trailById = getApprovedHikingTrailById(id);
        return this.hikingTrailMapper.hikingTrailEntityToHikingTrailDetailsDto(trailById);
    }

    @SuppressWarnings("unchecked")
    public <T> T getHikingTrailAuthenticated(Long id, UserDetails userDetails) {
        String username = userDetails.getUsername();
        Optional<HikingTrailEntity> possibleOwner = this.hikingTrailRepository
                .findByIdAndStatusApprovedOrStatusPendingAndOwner(
                        id, username, StatusEnum.APPROVED, StatusEnum.PENDING, StatusEnum.REVIEW
                );

        HikingTrailEntity trail = possibleOwner
                .orElseThrow(() -> new AppException("HikingTrail not found or invalid status!",
                        HttpStatus.BAD_REQUEST));

        boolean isNotOwner =
                trail.getDetailsStatus().equals(StatusEnum.APPROVED)
                        && trail.getCreatedBy() != null
                        && !trail.getCreatedBy().getEmail().equals(username);
        UserEntity loggedUser = this.userService.getUserEntityByEmail(username);
        return isNotOwner
                ? (T) this.hikingTrailMapper.hikingTrailEntityToHikingTrailDetailsLikeDto(trail, loggedUser)
                : (T) this.hikingTrailMapper.hikingTrailEntityToHikingTrailDetailsDto(trail);
    }

    public Page<HikingTrailBasicDto> getAllHikingTrails(Pageable pageable) {
        return this.hikingTrailRepository
                .findAllByTrailStatus(StatusEnum.APPROVED, pageable);
    }

    @Transactional
    public Page<HikingTrailBasicLikesDto> getAllHikingTrailsWithLikes(
            UserDetails userDetails,
            Pageable pageable,
            Boolean sortByLikedUser
    ) {
        return this.hikingTrailRepository
                .getTrailsWithLikes(StatusEnum.APPROVED, userDetails.getUsername(), pageable, sortByLikedUser);
    }

    public List<HikingTrailIdTrailNameDto> selectAll() {
        return this.hikingTrailRepository.findAllBy();
    }

    public Long createHikingTrail(
            HikingTrailCreateOrReviewDto hikingTrailCreateOrReviewDto,
            UserDetails userDetails
    ) {
        UserEntity validUser = this.userService.getUserEntityByEmail(userDetails.getUsername());

        HikingTrailEntity newHikingTrail =
                this.hikingTrailMapper
                        .hikingTrailCreateDtoToHikingTrailEntity(hikingTrailCreateOrReviewDto);

//        logger.debug("{}", newHikingTrail);
        boolean superUser = isSuperUser(userDetails);

        StatusEnum detailsStatus = superUser ? StatusEnum.APPROVED : StatusEnum.PENDING;

        SuperUserReviewStatusEnum trailStatus =
                superUser ? SuperUserReviewStatusEnum.APPROVED : SuperUserReviewStatusEnum.PENDING;

        if (superUser) {
            newHikingTrail.setReviewedBy(validUser);
        }

        newHikingTrail.setDetailsStatus(detailsStatus);
        newHikingTrail.setTrailStatus(trailStatus);
        newHikingTrail.setMaxNumberOfImages(10);
        newHikingTrail.setCreatedBy(validUser);
        newHikingTrail.setCreationDate(LocalDateTime.now());

        if (!hikingTrailCreateOrReviewDto.destinations().isEmpty()) {
            List<DestinationEntity> destinationEntities =
                    mapDtoToDestinationEntities(hikingTrailCreateOrReviewDto.destinations());
            newHikingTrail.setDestinations(destinationEntities);
        }

        if (!hikingTrailCreateOrReviewDto.availableHuts().isEmpty()) {
            List<AccommodationEntity> accommodationEntities
                    = mapDtoToAccommodationEntities(hikingTrailCreateOrReviewDto.availableHuts());
            newHikingTrail.setAvailableHuts(accommodationEntities);
        }

        return saveTrailWithReturn(newHikingTrail).getId();
    }

//    public boolean deleteHikingTrail(Long id) {
//        this.hikingTrailRepository.deleteById(id);
//        return false;
//    }

    public HikingTrailStartPointDto updateHikingTrailStartPoint(
            Long id,
            HikingTrailUpdateStartPointDto newStartPoint,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getStartPoint,
                currentTrail::setStartPoint,
                newStartPoint.startPoint());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailStartPointDto(
                currentTrail.getStartPoint(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailEndPointDto updateHikingTrailEndPoint(
            Long id,
            HikingTrailUpdateEndPointDto newEndPoint,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getEndPoint,
                currentTrail::setEndPoint,
                newEndPoint.endPoint());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailEndPointDto(
                currentTrail.getEndPoint(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailTotalDistanceDto updateHikingTrailTotalDistance(
            Long id,
            HikingTrailUpdateTotalDistanceDto newTotalDistance,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getTotalDistance,
                currentTrail::setTotalDistance,
                newTotalDistance.totalDistance());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailTotalDistanceDto(
                currentTrail.getTotalDistance(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailElevationGainedDto updateHikingTrailElevationGained(
            Long id,
            HikingTrailUpdateElevationGainedDto newElevationGained,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getElevationGained,
                currentTrail::setElevationGained,
                newElevationGained.elevationGained());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailElevationGainedDto(
                currentTrail.getElevationGained(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailWaterAvailableDto updateHikingTrailWaterAvailable(
            Long id,
            HikingTrailUpdateWaterAvailableDto newWaterAvailable,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getWaterAvailable,
                currentTrail::setWaterAvailable,
                newWaterAvailable.waterAvailable());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailWaterAvailableDto(
                currentTrail.getWaterAvailable().getValue(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailActivityDto updateHikingTrailActivity(
            Long id,
            HikingTrailUpdateActivityDto newActivity,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());
        /*TODO: Test Object.equals with list, might need to change to set*/
        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getActivity,
                currentTrail::setActivity,
                newActivity.activity());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailActivityDto(
                currentTrail.getActivity().stream().map(SuitableForEnum::getValue).collect(Collectors.toList()),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public HikingTrailTrailInfoDto updateHikingTrailTrailInfo(
            Long id,
            HikingTrailUpdateTrailInfoDto newTrailInfo,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getTrailInfo,
                currentTrail::setTrailInfo,
                newTrailInfo.trailInfo());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailTrailInfoDto(
                currentTrail.getTrailInfo(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public AccommodationWrapperDto updateHikingTrailAvailableHuts(
            Long id,
            HikingTrailUpdateAvailableHutsDto newHuts,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailWithHutsByIdAndStatusIfOwner(id, userDetails.getUsername());
        /*TODO: Test Object.equals with list, might need to change to set*/
        boolean isUpdated = updateAccommodationList(currentTrail, newHuts.availableHuts());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        List<AccommodationBasicDto> availableHuts = currentTrail
                .getAvailableHuts()
                .stream()
                .map(hut -> new AccommodationBasicDto(hut.getId(), hut.getAccommodationName()))
                .collect(Collectors.toList());

        return new AccommodationWrapperDto(
                availableHuts,
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public DestinationWrapperDto updateHikingTrailDestinations(
            Long id,
            HikingTrailUpdateDestinationsDto newDestinations,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailWithDestinationsByAndStatusIfOwner(id, userDetails.getUsername());
        /*TODO: Test Object.equals with list, might need to change to set*/
        boolean isUpdated = updateDestinationList(currentTrail, newDestinations.destinations());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        List<DestinationBasicDto> destinations = currentTrail
                .getDestinations()
                .stream()
                .map(destination -> new DestinationBasicDto(destination.getId(), destination.getDestinationName()))
                .collect(Collectors.toList());

        return new DestinationWrapperDto(
                destinations,
                isUpdated ? currentTrail.getModificationDate() : null
        );
    }

    public HikingTrailDifficultyDto updateHikingTrailDifficulty(
            Long id,
            HikingTrailUpdateTrailDifficultyDto newDifficulty,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailByIdAndStatusIfOwner(id, userDetails.getUsername());

        boolean isUpdated = updateFieldIfDifferent(
                currentTrail::getTrailDifficulty,
                currentTrail::setTrailDifficulty,
                newDifficulty.trailDifficulty());

        if (isUpdated) {
            currentTrail.setDetailsStatus(StatusEnum.PENDING);
            currentTrail.setTrailStatus(SuperUserReviewStatusEnum.PENDING);
            currentTrail.setModificationDate(LocalDateTime.now());
            currentTrail = saveTrailWithReturn(currentTrail);
        }

        return new HikingTrailDifficultyDto(
                currentTrail.getTrailDifficulty().getLevel(),
                isUpdated ? currentTrail.getModificationDate() : null);
    }

    public boolean updateHikingTrailMainImage(
            Long id,
            ImageMainUpdateDto imageMainUpdateDto,
            UserDetails userDetails
    ) {

        HikingTrailEntity currentTrail = getTrailWithImagesByIdAndStatusIfOwner(id, userDetails.getUsername());

        ImageEntity found =
                currentTrail
                        .getImages()
                        .stream()
                        .filter(i -> i.getId().equals(imageMainUpdateDto.imageId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new AppException("Unable to update main image: The specified image is not part of the user's collection.",
                                        HttpStatus.BAD_REQUEST));

        boolean isUpdated = updateFieldIfDifferent(currentTrail::getMainImage, currentTrail::setMainImage, found);

        if (isUpdated) {
            currentTrail.setMainImage(found);
            saveTrailWithoutReturn(currentTrail);
        }

        return true;
    }

    public CommentDto addNewTrailComment(
            Long trailId,
            CommentCreateDto commentDto,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getApprovedTrailWithCommentsById(trailId);

        UserEntity userCommenting = this.userService.getUserEntityByEmail(userDetails.getUsername());

        CommentEntity savedComment = this.commentService.saveComment(commentDto, userCommenting);

        currentTrail.setSingleComment(savedComment);
        saveTrailWithoutReturn(currentTrail);

        return this.commentMapper.commentEntityToCommentDto(savedComment);
    }

    /*
    In this example, the ParentEntity has a list of ChildEntity objects. The @OneToMany annotation with the cascade = CascadeType.ALL
    attribute means that any operation (including deletion) performed on the ParentEntity will be cascaded to the ChildEntity objects.
    The orphanRemoval = true attribute ensures that if a ChildEntity object is removed from the collection, it will be deleted from the database.

    To delete a ChildEntity, you can simply remove it from the collection in the ParentEntity and then save the ParentEntity.
    The removed ChildEntity will be deleted from the database due to the cascading delete operation.
    */
    public boolean deleteTrailComment(
            Long trailId,
            Long commentId,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getTrailWithCommentsById(trailId);

        this.commentService.validateCommentOwnership(commentId, userDetails.getUsername());

        boolean commentRemoved = currentTrail.getComments().removeIf(c -> c.getId().equals(commentId));

        if (!commentRemoved) {
            throw new AppException("Comment with id " + commentId + " was not found in the trail!",
                    HttpStatus.NOT_FOUND);
        }

        this.hikingTrailRepository.save(currentTrail);
        /*TODO: think how to handle exceptions*/
        this.commentService.deleteCommentById(commentId);

        return true;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public int getPendingApprovalTrailCount() {
        return this.hikingTrailRepository
                .countHikingTrailEntitiesByTrailStatus(SuperUserReviewStatusEnum.PENDING);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public int getUnderReviewTrailCount() {
        return this.hikingTrailRepository
                .countHikingTrailEntitiesByTrailStatus(SuperUserReviewStatusEnum.PENDING);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Page<HikingTrailForApprovalProjection> getAllHikingTrailsForApproval(
            SuperUserReviewStatusEnum status,
            Pageable pageable
    ) {
        return this.hikingTrailRepository
                .getHikingTrailEntitiesByTrailStatus(status, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public HikingTrailReviewDto reviewTrail(
            Long id,
            ExploreBgUserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getPendingTrailWithImagesById(id);

        StatusEnum detailsStatus = currentTrail.getDetailsStatus();

        if (isEligibleForReview(detailsStatus, currentTrail.getReviewedBy(), userDetails)) {
            return this.hikingTrailMapper.hikingTrailEntityToHikingTrailReviewDto(currentTrail);
        }

        for (ImageEntity image : currentTrail.getImages()) {
            logger.info("Image reviewer:{}",image.getReviewedBy());
            if (isEligibleForReview(image.getImageStatus(), image.getReviewedBy(), userDetails)) {
                return this.hikingTrailMapper.hikingTrailEntityToHikingTrailReviewDto(currentTrail);
            }
        }

        throw new AppException("Item with invalid status for review!", HttpStatus.BAD_REQUEST);
    }

    private boolean isEligibleForReview(
            StatusEnum status,
            UserEntity reviewedBy,
            ExploreBgUserDetails userDetails
    ) {
        if (status == StatusEnum.PENDING) {
            return true;
        }

        return status == StatusEnum.REVIEW &&
                Objects.equals(reviewedBy != null
                                ? reviewedBy.getUsername() : null,
                        userDetails.getProfileName());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public boolean claimTrailReview(
            Long id,
            ReviewBooleanDto reviewBoolean,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getHikingTrailById(id);
        StatusEnum detailsStatus = currentTrail.getDetailsStatus();

        UserEntity loggedUser = this.userService.getUserEntityByEmail(userDetails.getUsername());

        UserEntity reviewedByUser = currentTrail.getReviewedBy();
        String reviewedByUserName = reviewedByUser != null ? reviewedByUser.getUsername() : null;

        if (reviewBoolean.review()) {
            handleClaimReview(currentTrail, detailsStatus, reviewedByUserName, loggedUser);
        } else {
            handleCancelClaim(currentTrail, detailsStatus, reviewedByUserName, loggedUser);
        }

        saveTrailWithoutReturn(currentTrail);
        return true;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public boolean approveTrail(
            Long id,
            HikingTrailCreateOrReviewDto trailCreateOrReview,
            ExploreBgUserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getHikingTrailById(id);
        validateTrailApproval(currentTrail, userDetails);

        updateTrailFields(currentTrail, trailCreateOrReview);
        /*TODO: TrailStatus to be updated if no images and no gpx with status PENDING or REVIEWED to APPROVED*/
        currentTrail.setDetailsStatus(StatusEnum.APPROVED);

        saveTrailWithoutReturn(currentTrail);

        return true;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public UserIdDto getReviewerId(Long id) {
        Long reviewerId = this.hikingTrailRepository.findReviewerId(id);
        return new UserIdDto(reviewerId);
    }

    private HikingTrailEntity getPendingTrailWithImagesById(Long id) {
        Optional<HikingTrailEntity> trail = this.hikingTrailRepository.findWithImagesByIdAndTrailStatus(
                id,
                SuperUserReviewStatusEnum.PENDING
        );

        if (trail.isEmpty()) {
            throw new AppException("Hiking trail not found or has an invalid status!", HttpStatus.BAD_REQUEST);
        }

        return trail.get();
    }

    public HikingTrailEntity getTrailByIdIfOwner(Long id, UserDetails userDetails) {
        return this.hikingTrailRepository
                .findByIdAndCreatedBy_Email(id, userDetails.getUsername())
                .orElseThrow(() ->
                        new AppException("Hiking trail not found or is not owned by the specified user!",
                                HttpStatus.BAD_REQUEST));
    }

    public HikingTrailEntity getTrailWithImagesByIdIfOwner(Long id, UserDetails userDetails) {
        return this.hikingTrailRepository
                .findWithImagesByIdAndCreatedBy_Email(id, userDetails.getUsername())
                .orElseThrow(() ->
                        new AppException("Hiking trail not found or is not owned by the specified user!",
                                HttpStatus.BAD_REQUEST));
    }

    public HikingTrailEntity getTrailByIdAndStatusIfOwner(Long id, String email) {
        Optional<HikingTrailEntity> exist = this.hikingTrailRepository
                .findByIdAndDetailsStatusInAndCreatedByEmail(
                        id,
                        List.of(StatusEnum.PENDING, StatusEnum.APPROVED),
                        email);
        logger.info("user id " + id + "username " + email);
        if (exist.isEmpty()) {
            throw new AppException(
                    "Hiking trail not found, has an invalid status, or is not owned by the specified user!",
                    HttpStatus.BAD_REQUEST);
        }

        return exist.get();
    }

    public HikingTrailEntity getTrailWithImagesByIdAndStatusIfOwner(Long id, String email) {
        Optional<HikingTrailEntity> exist = this.hikingTrailRepository.findWithImagesByIdAndDetailsStatusInAndCreatedByEmail(
                id,
                List.of(StatusEnum.PENDING, StatusEnum.APPROVED),
                email);

        logger.info(String.format("Method: %s, Fetching hiking trail with ID: %d for user: %s with statuses: %s",
                Thread.currentThread().getStackTrace()[1].getMethodName(),
                id,
                email,
                List.of(StatusEnum.PENDING, StatusEnum.APPROVED)));

        if (exist.isEmpty()) {
            throw new AppException(
                    "Hiking trail not found, has an invalid status, or is not owned by the specified user!",
                    HttpStatus.BAD_REQUEST);
        }
        return exist.get();
    }

    public HikingTrailEntity getTrailWithImagesAndImageReviewerByIdAndStatusIfOwner(Long id, String email) {
        Optional<HikingTrailEntity> trail = this.hikingTrailRepository.findWithImagesAndImageReviewerByIdAndDetailsStatusInAndCreatedByEmail(
                id,
                List.of(StatusEnum.PENDING, StatusEnum.APPROVED),
                email);

        logger.info(String.format("Method: %s, Fetching hiking trail with ID: %d for user: %s with statuses: %s",
                Thread.currentThread().getStackTrace()[1].getMethodName(),
                id,
                email,
                List.of(StatusEnum.PENDING, StatusEnum.APPROVED)));

        if (trail.isEmpty()) {
            throw new AppException(
                    "Hiking trail not found, has an invalid status, or is not owned by the specified user!",
                    HttpStatus.BAD_REQUEST);
        }

        return trail.get();
    }

    public HikingTrailEntity getTrailWithDestinationsByAndStatusIfOwner(Long id, String email) {
        Optional<HikingTrailEntity> exist =
                this.hikingTrailRepository.findWithDestinationsByIdAndDetailsStatusInAndCreatedByEmail(
                        id,
                        List.of(StatusEnum.PENDING, StatusEnum.APPROVED),
                        email);
        logger.info("user id " + id + "username " + email);
        if (exist.isEmpty()) {
            throw new AppException(
                    "Hiking trail not found, has an invalid status, or is not owned by the specified user!",
                    HttpStatus.BAD_REQUEST);
        }
        return exist.get();
    }

    public HikingTrailEntity getTrailWithHutsByIdAndStatusIfOwner(Long id, String email) {
        Optional<HikingTrailEntity> exist =
                this.hikingTrailRepository.findWithHutsByIdAndDetailsStatusInAndCreatedByEmail(
                        id,
                        List.of(StatusEnum.PENDING, StatusEnum.APPROVED),
                        email);
        logger.info("user id " + id + "username " + email);
        if (exist.isEmpty()) {
            throw new AppException(
                    "Hiking trail not found, has an invalid status, or is not owned by the specified user!",
                    HttpStatus.BAD_REQUEST);
        }
        return exist.get();
    }

    public HikingTrailEntity saveTrailWithReturn(HikingTrailEntity trail) {
        return this.hikingTrailRepository.save(trail);
    }

    public void saveTrailWithoutReturn(HikingTrailEntity trail) {
        this.hikingTrailRepository.save(trail);
    }

    protected HikingTrailEntity getHikingTrailById(Long id) {
        Optional<HikingTrailEntity> trailById = this.hikingTrailRepository.findById(id);

        if (trailById.isEmpty()) {
            throw new AppException("Hiking trail not found!", HttpStatus.NOT_FOUND);
        }

        return trailById.get();
    }

    protected HikingTrailEntity getTrailWithCommentsById(Long id) {
        Optional<HikingTrailEntity> exist = this.hikingTrailRepository.findWithCommentsById(id);

        if (exist.isEmpty()) {
            throw new AppException("Hiking trail not found!", HttpStatus.NOT_FOUND);
        }

        return exist.get();
    }

    public HikingTrailEntity getApprovedTrailWithLikesById(Long id) {
        Optional<HikingTrailEntity> exist =
                this.hikingTrailRepository.findWithLikesByIdAndDetailsStatus(id, StatusEnum.APPROVED);

        if (exist.isEmpty()) {
            throw new AppException("Hiking trail not found or not approved!", HttpStatus.NOT_FOUND);
        }

        return exist.get();
    }

    //TODO: use this method for members
    private HikingTrailEntity getApprovedHikingTrailById(Long id) {
        Optional<HikingTrailEntity> byIdAndTrailStatus =
                this.hikingTrailRepository.findByIdAndDetailsStatus(id, StatusEnum.APPROVED);

        if (byIdAndTrailStatus.isEmpty()) {
            throw new AppException("Hiking trail not found or not approved!", HttpStatus.NOT_FOUND);
        }

        return byIdAndTrailStatus.get();
    }

    public HikingTrailEntity getApprovedTrailWithCommentsById(Long id) {
        Optional<HikingTrailEntity> exist =
                this.hikingTrailRepository.findWithCommentsByIdAndDetailsStatus(id, StatusEnum.APPROVED);

        if (exist.isEmpty()) {
            throw new AppException("Hiking trail not found or has an invalid status!", HttpStatus.BAD_REQUEST);
        }

        return exist.get();
    }

    private void validateTrailApproval(
            HikingTrailEntity currentTrail,
            ExploreBgUserDetails userDetails
    ) {
        StatusEnum trailStatus = currentTrail.getDetailsStatus();
        String reviewedByUserProfile = currentTrail.getReviewedBy() != null ? currentTrail.getReviewedBy().getUsername() : null;

        if (reviewedByUserProfile == null) {
            throw new AppException("A pending item can not be approved!", HttpStatus.BAD_REQUEST);
        }

        if (trailStatus.equals(StatusEnum.REVIEW) && !reviewedByUserProfile.equals(userDetails.getProfileName())) {
            throw new AppException("The item has already been claimed by another user! You can not approved it!", HttpStatus.BAD_REQUEST);
        }

        if (trailStatus.equals(StatusEnum.APPROVED)) {
            throw new AppException("The item has already been approved!", HttpStatus.BAD_REQUEST);
        }
    }

    private void updateTrailFields(
            HikingTrailEntity currentTrail,
            HikingTrailCreateOrReviewDto trailCreateOrReview
    ) {
        boolean isUpdated =
                updateFieldIfDifferent(currentTrail::getStartPoint, currentTrail::setStartPoint, trailCreateOrReview.startPoint()) ||
                        updateFieldIfDifferent(currentTrail::getEndPoint, currentTrail::setEndPoint, trailCreateOrReview.endPoint()) ||
                        updateFieldIfDifferent(currentTrail::getTotalDistance, currentTrail::setTotalDistance, trailCreateOrReview.totalDistance()) ||
                        updateFieldIfDifferent(currentTrail::getTrailInfo, currentTrail::setTrailInfo, trailCreateOrReview.trailInfo()) ||
                        updateFieldIfDifferent(currentTrail::getSeasonVisited, currentTrail::setSeasonVisited, trailCreateOrReview.seasonVisited()) ||
                        updateFieldIfDifferent(currentTrail::getWaterAvailable, currentTrail::setWaterAvailable, trailCreateOrReview.waterAvailable()) ||
                        updateFieldIfDifferent(currentTrail::getTrailDifficulty, currentTrail::setTrailDifficulty, trailCreateOrReview.trailDifficulty()) ||
                        updateFieldIfDifferent(currentTrail::getActivity, currentTrail::setActivity, trailCreateOrReview.activity()) ||
                        updateFieldIfDifferent(currentTrail::getElevationGained, currentTrail::setElevationGained, trailCreateOrReview.elevationGained()) ||
                        updateFieldIfDifferent(currentTrail::getNextTo, currentTrail::setNextTo, trailCreateOrReview.nextTo()) ||

                        updateAccommodationList(currentTrail, trailCreateOrReview.availableHuts()) ||
                        updateDestinationList(currentTrail, trailCreateOrReview.destinations());

        if (isUpdated) {
            currentTrail.setModificationDate(LocalDateTime.now());
        }
    }

    private <T> boolean updateFieldIfDifferent(Supplier<T> getter, Consumer<T> setter, T newValue) {
        T currentValue = getter.get();

        if (!Objects.equals(currentValue, newValue)) {
            setter.accept(newValue);
            return true;
        }
        return false;
    }

    private boolean updateAccommodationList(HikingTrailEntity currentTrail, List<AccommodationIdDto> newHuts) {
        List<AccommodationIdDto> currentHuts = currentTrail.getAvailableHuts()
                .stream()
                .map(a -> new AccommodationIdDto(a.getId()))
                .toList();

        if (!Objects.equals(currentHuts, newHuts)) {
            List<AccommodationEntity> accommodationEntities = mapDtoToAccommodationEntities(newHuts);
            currentTrail.setAvailableHuts(accommodationEntities);
            return true;
        }
        return false;
    }

    private boolean updateDestinationList(HikingTrailEntity currentTrail, List<DestinationIdDto> newDestinations) {
        List<DestinationIdDto> currentDestinations = currentTrail.getDestinations()
                .stream()
                .map(de -> new DestinationIdDto(de.getId()))
                .toList();

        if (!Objects.equals(currentDestinations, newDestinations)) {
            List<DestinationEntity> destinationEntities = mapDtoToDestinationEntities(newDestinations);
            currentTrail.setDestinations(destinationEntities);
            return true;
        }
        return false;
    }

    private List<AccommodationEntity> mapDtoToAccommodationEntities(List<AccommodationIdDto> ids) {

        List<Long> accommodationIds = ids.stream().map(AccommodationIdDto::id).toList();

        return this.accommodationService.getAccommodationsById(accommodationIds);
    }

    private List<DestinationEntity> mapDtoToDestinationEntities(List<DestinationIdDto> ids) {

        List<Long> destinationIds = ids.stream().map(DestinationIdDto::id).toList();

        return this.destinationService.getDestinationsByIds(destinationIds);
    }

    private void handleClaimReview(
            HikingTrailEntity trail,
            StatusEnum detailsStatus,
            String reviewedByUserName,
            UserEntity loggedUser
    ) {
        if (detailsStatus.equals(StatusEnum.REVIEW)) {
            if (Objects.equals(reviewedByUserName, loggedUser.getUsername())) {
                throw new AppException("You have already claimed this item for review!", HttpStatus.BAD_REQUEST);
            } else {
                throw new AppException("The item has already been claimed by another user!", HttpStatus.BAD_REQUEST);
            }
        }

        if (detailsStatus.equals(StatusEnum.APPROVED)) {
            throw new AppException("The item has already been approved!", HttpStatus.BAD_REQUEST);
        }

        trail.setDetailsStatus(StatusEnum.REVIEW);
        trail.setReviewedBy(loggedUser);
    }

    private void handleCancelClaim(
            HikingTrailEntity trail,
            StatusEnum detailsStatus,
            String reviewedByUserName,
            UserEntity loggedUser
    ) {
        if (detailsStatus.equals(StatusEnum.PENDING)) {
            throw new AppException("You cannot cancel the review for an item that you haven't claimed!", HttpStatus.BAD_REQUEST);
        }

        if (detailsStatus.equals(StatusEnum.REVIEW)) {
            if (Objects.equals(reviewedByUserName, loggedUser.getUsername())) {
                trail.setDetailsStatus(StatusEnum.PENDING);
                trail.setReviewedBy(null);
            } else {
                throw new AppException("The item has already been claimed by another user!", HttpStatus.BAD_REQUEST);
            }
        }

        if (detailsStatus.equals(StatusEnum.APPROVED)) {
            throw new AppException("The item has already been approved!", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isSuperUser(UserDetails userDetails) {
        return userDetails
                .getAuthorities()
                .stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals("ROLE_ADMIN")
                                || grantedAuthority.getAuthority().equals("ROLE_MODERATOR"));
    }

    public boolean likeOrUnlikeTrail(
            Long trailId,
            LikeBooleanDto likeBoolean,
            UserDetails userDetails
    ) {
        HikingTrailEntity currentTrail = getApprovedTrailWithLikesById(trailId);
        UserEntity loggedUser = userService.getUserEntityByEmail(userDetails.getUsername());
        Set<UserEntity> likedByUsers = currentTrail.getLikedByUsers();
        boolean userHasLiked = likedByUsers.contains(loggedUser);

        if (likeBoolean.like()) {
            handleLike(likedByUsers, loggedUser, userHasLiked);
        } else {
            handleUnlike(likedByUsers, loggedUser, userHasLiked);
        }

        saveTrailWithoutReturn(currentTrail);
        return true;
    }

    private void handleLike(Set<UserEntity> likedByUsers, UserEntity user, boolean userHasLiked) {
        if (userHasLiked) {
            throw new AppException("You have already liked the item!", HttpStatus.BAD_REQUEST);
        }
        likedByUsers.add(user);
    }

    private void handleUnlike(Set<UserEntity> likedByUsers, UserEntity user, boolean userHasLiked) {
        if (!userHasLiked) {
            throw new AppException("You cannot unlike an item that you haven't liked!", HttpStatus.BAD_REQUEST);
        }
        likedByUsers.remove(user);
    }
}
