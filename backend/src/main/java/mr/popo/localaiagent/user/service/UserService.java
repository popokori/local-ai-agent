package mr.popo.localaiagent.user.service;

import lombok.RequiredArgsConstructor;
import mr.popo.localaiagent.common.exception.BusinessException;
import mr.popo.localaiagent.common.exception.ResourceNotFoundException;
import mr.popo.localaiagent.user.domain.User;
import mr.popo.localaiagent.user.dto.ChangePasswordRequest;
import mr.popo.localaiagent.user.dto.UpdateUserRequest;
import mr.popo.localaiagent.user.dto.UserDto;
import mr.popo.localaiagent.user.mapper.UserMapper;
import mr.popo.localaiagent.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDto getMe(Long userId) {
        return userMapper.toDto(loadUser(userId));
    }

    @Transactional
    public UserDto updateMe(Long userId, UpdateUserRequest request) {
        User user = loadUser(userId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessException("Email already taken");
            }
            user.setEmail(request.email());
        }
        return userMapper.toDto(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = loadUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
