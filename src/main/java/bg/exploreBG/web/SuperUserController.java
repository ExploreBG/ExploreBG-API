package bg.exploreBG.web;

import bg.exploreBG.model.dto.ApiResponse;
import bg.exploreBG.model.dto.EntitiesForApprovalUnderReviewCountDto;
import bg.exploreBG.model.dto.ReviewBooleanDto;
import bg.exploreBG.model.dto.hikingTrail.HikingTrailForApprovalProjection;
import bg.exploreBG.model.dto.hikingTrail.HikingTrailReviewDto;
import bg.exploreBG.model.dto.hikingTrail.validate.HikingTrailCreateOrReviewDto;
import bg.exploreBG.model.dto.user.UserClassDataDto;
import bg.exploreBG.model.dto.user.UserDataDto;
import bg.exploreBG.model.dto.user.validate.UserAccountLockUnlockDto;
import bg.exploreBG.model.dto.user.validate.UserModRoleDto;
import bg.exploreBG.model.enums.StatusEnum;
import bg.exploreBG.model.user.ExploreBgUserDetails;
import bg.exploreBG.service.AccommodationService;
import bg.exploreBG.service.DestinationService;
import bg.exploreBG.service.HikingTrailService;
import bg.exploreBG.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-users")
public class SuperUserController {
    private final AccommodationService accommodationService;
    private final DestinationService destinationService;
    private final HikingTrailService hikingTrailService;
    private final UserService userService;

    public SuperUserController(
            AccommodationService accommodationService,
            DestinationService destinationService,
            HikingTrailService hikingTrailService,
            UserService userService
    ) {
        this.accommodationService = accommodationService;
        this.destinationService = destinationService;
        this.hikingTrailService = hikingTrailService;
        this.userService = userService;
    }

    /*
     ADMIN
    */
//    @GetMapping("/users")
//    public ResponseEntity<Page<UserDataProjection>> allUsers(
//            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) int pageNumber,
//            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
//            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
//            @RequestParam(value = "sortDir", defaultValue = "ASC", required = false) String sortDir
//    ) {
//        Sort parameters = Sort.by(Sort.Direction.valueOf(sortDir), sortBy);
//        int currentPage = Math.max(pageNumber - 1, 0);
//
//        Pageable pageable = PageRequest.of(currentPage, pageSize, parameters);
//
//        Page<UserDataProjection> users = this.userService.getAllUsers(pageable);
//
//        return ResponseEntity.ok(users);
//    }
    @Transactional
    @GetMapping("/users")
    public ResponseEntity<List<UserClassDataDto>> allUsers() {

        List<UserClassDataDto> users = this.userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/update-role")
    public ResponseEntity<ApiResponse<UserDataDto>> addRemoveModeratorRole(
            @PathVariable Long id,
            @Valid @RequestBody UserModRoleDto userModRoleDto
    ) {

        UserDataDto updatedUserRole = this.userService.addRemoveModeratorRoleToUserRoles(id, userModRoleDto);

        ApiResponse<UserDataDto> response = new ApiResponse<>(updatedUserRole);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/lock-account")
    public ResponseEntity<ApiResponse<Boolean>> lockUnlockUserAccount(
            @PathVariable Long id,
            @RequestBody UserAccountLockUnlockDto userAccountLockUnlockDto
    ) {
        boolean success = this.userService.lockOrUnlockUserAccount(id, userAccountLockUnlockDto);

        ApiResponse<Boolean> response = new ApiResponse<>(success);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/waiting-approval/count")
    public ResponseEntity<EntitiesForApprovalUnderReviewCountDto> waitingForApprovalUnderReviewCount() {
        int accommodationCountPending = this.accommodationService.getPendingApprovalAccommodationCount();
        int accommodationCountReview = this.accommodationService.getUnderReviewAccommodationCount();

        int destinationCountPending = this.destinationService.getPendingApprovalDestinationCount();
        int destinationCountReview = this.destinationService.getUnderReviewDestinationCount();

        int trailCountPending = this.hikingTrailService.getPendingApprovalTrailCount();
        int trailCountReview = this.hikingTrailService.getUnderReviewTrailCount();

        EntitiesForApprovalUnderReviewCountDto countDto =
                new EntitiesForApprovalUnderReviewCountDto(
                        accommodationCountPending,
                        accommodationCountReview,
                        destinationCountPending,
                        destinationCountReview,
                        trailCountPending,
                        trailCountReview
                );

        return ResponseEntity.ok(countDto);
    }

    @GetMapping("/waiting-approval/trails")
    public ResponseEntity<Page<HikingTrailForApprovalProjection>> waitingForApprovalTrails(
            @RequestParam(value = "pageNumber", defaultValue = "1", required = false) int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "ASC", required = false) String sortDir
    ) {
        Sort parameters = Sort.by(Sort.Direction.valueOf(sortDir), sortBy);
        int currentPage = Math.max(pageNumber - 1, 0);

        Pageable pageable = PageRequest.of(currentPage, pageSize, parameters);

        Page<HikingTrailForApprovalProjection> forApproval =
                this.hikingTrailService
                        .getAllHikingTrailsForApproval(List.of(StatusEnum.PENDING, StatusEnum.REVIEW), pageable);

        return ResponseEntity.ok(forApproval);
    }

    //Add data ???
    @Transactional
    @GetMapping("/review/trail/{id}")
    public ResponseEntity<ApiResponse<HikingTrailReviewDto>> reviewNewTrail(
            @PathVariable Long id,
            @AuthenticationPrincipal ExploreBgUserDetails exploreBgUserDetails
    ) {
        HikingTrailReviewDto toReview = this.hikingTrailService.reviewTrail(id, exploreBgUserDetails);

        ApiResponse<HikingTrailReviewDto> response = new ApiResponse<>(toReview);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/review/trail/{id}/claim")
    public ResponseEntity<ApiResponse<Boolean>> claimNewTrailReview(
            @PathVariable Long id,
            @RequestBody ReviewBooleanDto reviewBooleanDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean success = this.hikingTrailService.claimTrailReview(id, reviewBooleanDto, userDetails);

        ApiResponse<Boolean> response = new ApiResponse<>(success);

        return ResponseEntity.ok(response);
    }

    @Transactional
    @PatchMapping("/approve/trail/{id}")
    public ResponseEntity<ApiResponse<Boolean>> approveNewTrail(
            @PathVariable Long id,
            @Valid @RequestBody HikingTrailCreateOrReviewDto trailCreateOrReviewDto,
            @AuthenticationPrincipal ExploreBgUserDetails exploreBgUserDetails
    ) {
        boolean approved =
                this.hikingTrailService.approveTrail(id, trailCreateOrReviewDto, exploreBgUserDetails);

        ApiResponse<Boolean> response = new ApiResponse<>(approved);

        return ResponseEntity.ok(response);
    }
}
