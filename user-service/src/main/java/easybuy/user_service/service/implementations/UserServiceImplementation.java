package easybuy.user_service.service.implementations;

import com.easybuy.common.exceptions.customException.EmailAlreadyExistsException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import easybuy.user_service.dto.UserDTO;
import easybuy.user_service.dto.UserPageResponse;
import easybuy.user_service.dto.UserUpdateRequestDTO;
import easybuy.user_service.dto.UserUpdateResponseDTO;
import easybuy.user_service.entity.User;
import easybuy.user_service.repository.UserRepository;
import easybuy.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {
        User user = getUserFromDTO(userDTO);
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
