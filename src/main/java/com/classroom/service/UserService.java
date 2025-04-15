package com.classroom.service;
import com.classroom.model.User;
import com.classroom.model.UserType;
import com.classroom.repository.UserRepository;


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
            var userObj = user.get();
            return org.springframework.security.core.userdetails.User
                    .withUsername(userObj.getEmail())
                    .password(userObj.getPassword())
                    .roles("USER") // You can replace this with dynamic roles later
                    .build();
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
            // or default to whatever you want

            // Set a dummy password as it's not needed for OAuth users
            newUser.setPassword("OAUTH_USER");

            repository.save(newUser);
        }
    }
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    public void register(User user) {
//        // Hash the password before saving
//        //user.setPassword(passwordEncoder.encode(user.getPassword()));
//        userRepository.save(user);
//    }
}
