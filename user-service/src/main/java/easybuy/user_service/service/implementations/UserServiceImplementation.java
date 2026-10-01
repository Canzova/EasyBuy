package easybuy.user_service.service.implementations;

import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.common.exceptions.customException.EmailAlreadyExistsException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import easybuy.user_service.configuration.OtpProperties;
import easybuy.user_service.dto.*;
import easybuy.user_service.entity.PasswordResetToken;
import easybuy.user_service.entity.RefreshToken;
import easybuy.user_service.entity.User;
import easybuy.user_service.repository.PasswordResetTokenRepository;
import easybuy.user_service.repository.RefreshTokenRepository;
import easybuy.user_service.repository.UserRepository;
import easybuy.user_service.security.JWTService;
import easybuy.user_service.service.UserService;
import easybuy.user_service.service.email.EmailService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final OtpProperties otpProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        User user = getUserFromDTO(userDTO);

        // Before saving the user to DB, encode the password
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(Role.GUEST);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    private User getUserFromDTO(UserDTO userDTO) {
        if(userRepository.findByUsername(userDTO.getUsername()).isPresent()) throw new EmailAlreadyExistsException("Email already exists");
        return modelMapper.map(userDTO, User.class);
    }

    @Override
    public UserDTO getUserByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User does not exists with the given userId."));
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByUsername(email).orElseThrow(()-> new ResourceNotFoundException("User with given Email does not exists."));
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
            String refreshToken = jwtService.generateRefreshToken(user.getUserId().toString(), user.getUsername(), user.getRole());

            storeRefreshTokenInDB(refreshToken, user);
            log.info("User {} has been logged in", user.getUsername());
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
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByUser(user).orElse(RefreshToken.builder()
                .refreshToken(refreshToken)
                .user(user)
                .build());

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

        User user = userRepository.findByUsername(userId)
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

    @Override
    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        // 1. Verify user exists with the given email
        userRepository.findByUsername(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with given Email does not exists."));

        // 2. Invalidate any existing unused OTP tokens for this email
        passwordResetTokenRepository.invalidateExistingTokens(request.getEmail());

        // 3. Generate a secure 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 4. Save the new OTP record to database
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .email(request.getEmail())
                .otp(otp)
                .otpExpiryTime(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .isUsed(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        // 5. Send OTP via email provider (Resend / AWS SES)
        emailService.sendOtpEmail(request.getEmail(), otp);

        log.info("Password reset OTP generated and dispatched for email: {}", request.getEmail());
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        log.info("Verifying OTP for email: {}", request.getEmail());

        // 1. Find the latest active OTP record for this email
        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new BusinessException("No active OTP request found for this email. Please request an OTP first."));

        // 2. Check if OTP is expired
        if (LocalDateTime.now().isAfter(token.getOtpExpiryTime())) {
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }

        // 3. Validate the OTP code
        if (!token.getOtp().equals(request.getOtp())) {
            throw new BusinessException("Invalid OTP. Please check the code and try again.");
        }

        // 4. Generate a secure temporary reset token for step 3
        String resetToken = UUID.randomUUID().toString();
        token.setResetToken(resetToken);
        token.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(otpProperties.getResetTokenExpirationMinutes()));
        passwordResetTokenRepository.save(token);

        log.info("OTP verified successfully for email: {}. Reset token issued.", request.getEmail());

        return VerifyOtpResponse.builder()
                .message("OTP verified successfully. You may now reset your password.")
                .resetToken(resetToken)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with reset token");

        // 1. Find the token record by resetToken
        PasswordResetToken token = passwordResetTokenRepository
                .findByResetTokenAndIsUsedFalse(request.getResetToken())
                .orElseThrow(() -> new BusinessException("Invalid or already used reset token."));

        // 2. Verify token expiry
        if (token.getResetTokenExpiryTime() == null || LocalDateTime.now().isAfter(token.getResetTokenExpiryTime())) {
            throw new BusinessException("Reset token has expired. Please initiate the password reset process again.");
        }

        // 3. Retrieve user
        User user = userRepository.findByUsername(token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User associated with this token does not exist."));

        // 4. Encode and update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Invalidate the reset token so it cannot be reused
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // 6. Invalidate all active refresh tokens for security (global session revocation)
        refreshTokenRepository.deleteByUser(user);

        log.info("Password successfully reset for user: {}", user.getUsername());
    }
}
