package com.classroom.service;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;
import com.classroom.security.CustomUserDetails;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Getter
@Setter
@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = repository.findByEmail(email);
        if (user.isPresent()) {
            return new CustomUserDetails(user.get());
        } else {
            throw new UsernameNotFoundException(email);
        }
    }

    public void processOAuthPostLogin(String email, String name) {
        Optional<User> existingUser = repository.findByEmail(email);

        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setActive(true);
            newUser.setGoogleLinked(true);
            
            // Set a dummy password as it's not needed for OAuth users
            newUser.setPassword("OAUTH_USER");

            repository.save(newUser);
        }
    }
}
