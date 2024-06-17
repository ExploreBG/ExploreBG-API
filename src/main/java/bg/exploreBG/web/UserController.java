package bg.exploreBG.web;

import bg.exploreBG.config.UserAuthProvider;
import bg.exploreBG.model.dto.user.*;
import bg.exploreBG.model.dto.user.single.UserEmailDto;
import bg.exploreBG.model.dto.user.UserIdNameDto;
import bg.exploreBG.model.dto.user.single.UserUsernameDto;
import bg.exploreBG.model.dto.user.validate.*;
import bg.exploreBG.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserAuthProvider userAuthProvider;

    public UserController(UserService userService, UserAuthProvider userAuthProvider) {
        this.userService = userService;
        this.userAuthProvider = userAuthProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<UserIdNameDto> register(@Valid @RequestBody UserRegisterDto userRegisterDto) {
        UserIdNameEmailDto createdUser = this.userService.register(userRegisterDto);
        String token = this.userAuthProvider.createToken(createdUser.email());

        return ResponseEntity
                .created(URI.create("/api/users/" + createdUser.id() + "/my-profile"))
                .header(HttpHeaders.AUTHORIZATION, token)
                .body(new UserIdNameDto(createdUser.id(), createdUser.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<UserIdNameDto> login(@RequestBody UserLoginDto userLoginDto) {
        UserIdNameEmailDto loggedUser = this.userService.login(userLoginDto);
        String token = this.userAuthProvider.createToken(loggedUser.username());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.AUTHORIZATION, token)
                .body(new UserIdNameDto(loggedUser.id(), loggedUser.username()));
    }

    @GetMapping("/{id}/my-profile")
    public ResponseEntity<UserDetailsDto> myProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        System.out.println(userDetails.getPassword());
        UserDetailsDto byId = this.userService.findById(id, userDetails);

        return ResponseEntity
                .ok(byId);
    }

    @PatchMapping("/{id}/update-email")
    public ResponseEntity<UserEmailDto> updateEmail(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateEmailDto userUpdateEmailDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserEmailDto newEmail = this.userService.updateEmail(id, userUpdateEmailDto, userDetails);
        String token = this.userAuthProvider.createToken(newEmail.email());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.AUTHORIZATION, token)
                .body(newEmail);
    }

    @PatchMapping("/{id}/update-username")
    public ResponseEntity<UserUsernameDto> updateUsername(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateUsernameDto userUpdateUsernameDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserUsernameDto userUsernameDto = this.userService.updateUsername(id, userUpdateUsernameDto, userDetails);

        return ResponseEntity
                .ok(userUsernameDto);
    }

    @PatchMapping("/{id}/update-password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdatePasswordDto updatePassword,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String successes = this.userService.updatePassword(id, updatePassword, userDetails);

        return ResponseEntity.ok(successes);
    }


}
