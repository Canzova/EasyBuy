package easybuy.user_service.service.implementations;

import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.common.exceptions.customException.EmailAlreadyExistsException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import easybuy.user_service.dto.*;
import easybuy.user_service.entity.RefreshToken;
import easybuy.user_service.entity.User;
import easybuy.user_service.repository.RefreshTokenRepository;
import easybuy.user_service.repository.UserRepository;
import easybuy.user_service.security.JWTService;
import easybuy.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        User user = getUserFromDTO(userDTO);

        // Before saving the user to DB, encode the password
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        user = userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    private User getUserFromDTO(UserDTO userDTO) {
        if(userRepository.findByEmail(userDTO.getEmail()).isPresent()) throw new EmailAlreadyExistsException("Email already exists");
        return modelMapper.map(userDTO, User.class);
    }

    @Override
    public UserDTO getUserByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User does not exists with the given userId."));
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User with given Email does not exists."));
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserPageResponse getAllUsers(int pageNo, int pageSize, String sortBy, String sortOrder) {
        Sort sortByOrder = sortOrder.equals("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sortByOrder);

        Page<User> userPage = userRepository.findAll(pageable);
        return getUserPageResponse(userPage);

    }

    @Override
    public UserUpdateResponseDTO updateUser(UserUpdateRequestDTO userDTO, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User with given Email does not exists."));

        user.setName(userDTO.getName());
        user.setAddress(userDTO.getAddress());
        user.setPhone(userDTO.getPhone());

        user = userRepository.save(user);
        return modelMapper.map(user, UserUpdateResponseDTO.class);
    }

    @Override
    public void deleteUserByUserId(UUID userId) {
        if(userRepository.findById(userId).isEmpty()) throw new ResourceNotFoundException("User with given userId does not exists.");
        userRepository.deleteById(userId);
    }

    @Override
    public LoginResponse loginUser(LoginRequest loginRequest) {
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String accessToken = jwtService.generateNewAccessToken(user.getUserId().toString(), user.getUsername(), user.getRole());
            String refreshToken = jwtService.generateRefreshToken(user.getUserId().toString(), user.getUsername(), user.getRole();

            storeRefreshTokenInDB(refreshToken, user);

            return LoginResponse.builder()
                    .username(user.getUsername())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }catch (BadCredentialsException | UsernameNotFoundException e){
            log.info("Invalid username or password", e);
            throw new BusinessException("Invalid username or password.", e);
        }
    }

    private void storeRefreshTokenInDB(String refreshToken, User user) {
        // Store the refresh token into db
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .refreshToken(refreshToken)
                .user(user)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
    }

    @Override
    public RefreshTokenResponse updateRefreshAndAccessToken(RefreshTokenRequest refreshTokenRequest) {
        // Check if the userId sent into this refreshTokenRequest belongs to the user stored into that token and token is not expired
        if(!jwtService.isTokenValid(refreshTokenRequest.getRefreshToken(), refreshTokenRequest.getUsername()))
            throw new BusinessException("Token expired");

        // Now also check if this refreshToken is present into db
        RefreshToken refreshTokenFromDb = refreshTokenRepository.findByUser_Username(refreshTokenRequest.getUsername())
                .orElseThrow(()-> new ResourceNotFoundException("Refresh token does not exists."));

        String refreshToken = refreshTokenRequest.getRefreshToken();
        String roleStr = jwtService.extractRole(refreshToken);
        Role role = Role.valueOf(roleStr);
        String userId = refreshTokenRequest.getUsername();
        String username = jwtService.extractUsername(refreshToken);

        // Delete the old refreshToken so user cannot use it again
        refreshTokenRepository.delete(refreshTokenFromDb);

        refreshToken = jwtService.generateRefreshToken(userId, username, role);
        String accessToken = jwtService.generateNewAccessToken(userId, username, role);

        User user = userRepository.findByEmail(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User with given Email does not exists."));

        // Store the nre RefreshToken into DB
        storeRefreshTokenInDB(refreshToken, user);

        return RefreshTokenResponse.builder()
                .username(refreshTokenRequest.getUsername())
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .build();
    }

    private UserPageResponse getUserPageResponse(Page<User> userPage) {
        List<User> userList = userPage.getContent();

        List<UserDTO> userDTOList = userList.stream()
                .map(user-> modelMapper.map(user, UserDTO.class))
                .toList();

        return UserPageResponse.builder()
                .content(userDTOList)
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .pageSize(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .build();
    }
}
