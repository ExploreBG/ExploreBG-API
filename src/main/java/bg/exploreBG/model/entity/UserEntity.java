package bg.exploreBG.model.entity;

import bg.exploreBG.model.enums.GenderEnum;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private GenderEnum gender;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @OneToOne(cascade = CascadeType.ALL)
    private ImageEntity userImage;

    @Column(name = "user_info")
    private String userInfo;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<RoleEntity> roles = new ArrayList<>();

    // TODO: Think about FavouriteEntity implementation

    @OneToMany(mappedBy = "owner",fetch = FetchType.EAGER)
    private List<HikeEntity> createdHikes;

    public UserEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public GenderEnum getGender() {
        return gender;
    }

    public void setGender(GenderEnum gender) {
        this.gender = gender;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public ImageEntity getUserImage() {
        return userImage;
    }

    public void setUserImage(ImageEntity userImage) {
        this.userImage = userImage;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public List<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleEntity> roles) {
        this.roles = roles;
    }

    public List<HikeEntity> getCreatedHikes() {
        return createdHikes;
    }

    public void setCreatedHikes(List<HikeEntity> createdHikes) {
        this.createdHikes = createdHikes;
    }
}
