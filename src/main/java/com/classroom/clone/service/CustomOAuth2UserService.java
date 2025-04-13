package com.classroom.clone.service;

import com.classroom.clone.model.User;
import com.classroom.clone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CustomOAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract user info
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String pictureUrl = oauth2User.getAttribute("picture");
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub"); // Google's unique ID

        // Find or create user
        User user = processOAuth2User(provider, providerId, name, email, pictureUrl);

        // Return a custom OAuth2User that also contains our User entity
        return new CustomOAuth2User(oauth2User, user);
    }

    private User processOAuth2User(String providerName, String providerId, String name, String email, String pictureUrl) {
        User.OAuthProvider provider = User.OAuthProvider.valueOf(providerName.toUpperCase());

        // Try to find user by OAuth provider and ID
        Optional<User> userOptional = userRepository.findByOauthProviderAndOauthId(provider, providerId);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            // Update user information
            existingUser.setName(name);
            existingUser.setProfilePicture(pictureUrl);
            return userRepository.save(existingUser);
        } else {
            // Try to find by email
            userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                // Update existing user with OAuth info
                User existingUser = userOptional.get();
                existingUser.setOauthProvider(provider);
                existingUser.setOauthId(providerId);
                existingUser.setProfilePicture(pictureUrl);
                return userRepository.save(existingUser);
            } else {
                // Create new user
                User newUser = new User();
                newUser.setName(name);
                newUser.setEmail(email);
                newUser.setOauthProvider(provider);
                newUser.setOauthId(providerId);
                newUser.setProfilePicture(pictureUrl);
                newUser.setUserType(User.UserType.STUDENT); // Default role
                newUser.setActive(true);
                return userRepository.save(newUser);
            }
        }
    }
}