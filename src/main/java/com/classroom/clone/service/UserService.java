package com.classroom.clone.service;

import com.classroom.clone.model.Course;
import com.classroom.clone.model.User;
import com.classroom.clone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByOAuthIdAndProvider(String oauthId, String provider) {
        return userRepository.findByOauthProviderAndOauthId(User.OAuthProvider.valueOf(oauthId), provider);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

//    public List<User> findByUserType(String userType) {
//        return userRepository.findByUserType(userType);
//    }
//
//    public List<User> findAllActiveUsers() {
//        return userRepository.findByActiveTrue();
//    }

    public void toggleUserStatus(Integer userId, boolean active) {
        Optional<User> userOptional = userRepository.findById(userId);
        userOptional.ifPresent(user -> {
            user.setActive(active);
            userRepository.save(user);
        });
    }

    /**
     * Gets a list of unread notifications for a user
     * @param userId The user ID
     * @return The count of unread notifications
     */
    public int getUnreadNotificationCount(Integer userId) {
        // This would typically use a NotificationRepository
        // For now, we'll return a placeholder
        return 0;
    }

    /**
     * Checks if a user belongs to a specific course
     * @param userId The user ID
     * @param courseId The course ID
     * @return true if the user is a member of the course, false otherwise
     */
    public boolean isUserInCourse(Integer userId, Integer courseId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getCourseMemberships().stream()
                    .map(membership -> membership.getCourse().getId())
                    .anyMatch(id -> id.equals(courseId));
        }
        return false;
    }

    /**
     * Checks if a user has a specific role in a course
     * @param userId The user ID
     * @param courseId The course ID
     * @param role The role to check (TEACHER, STUDENT, TA)
     * @return true if the user has the specified role in the course, false otherwise
     */
    public boolean hasUserRoleInCourse(Integer userId, Integer courseId, String role) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.getCourseMemberships().stream()
                    .filter(membership -> membership.getCourse().getId().equals(courseId))
                    .anyMatch(membership -> membership.getRole().equals(role));
            return false;
        }
        return false;
    }
}