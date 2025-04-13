//package com.classroom.controller;
//
//import com.classroom.model.user.User;
//import com.classroom.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import java.util.List;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/users")
//public class UserController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    // Create User
//    @PostMapping("/create")
//    public User createUser(@RequestBody User user) {
//        return userRepository.save(user);
//    }
//
//    // Get All Users
//    @GetMapping("/all")
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//    // Get User by ID
//    @GetMapping("/{id}")
//    public ResponseEntity<User> getUserById(@PathVariable String id) {
//        Optional<User> user = userRepository.findById(id);
//        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    // Update User
//    @PutMapping("/update/{id}")
//    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User userDetails) {
//        return userRepository.findById(id).map(user -> {
//            user.setName(userDetails.getName());
//            user.setEmail(userDetails.getEmail());
//            user.setPassword(userDetails.getPassword());
//            return ResponseEntity.ok(userRepository.save(user));
//        }).orElseGet(() -> ResponseEntity.notFound().build());
//    }
//
//    // Delete User
//    @DeleteMapping("/delete/{id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
//        if (userRepository.existsById(id)) {
//            userRepository.deleteById(id);
//            return ResponseEntity.noContent().build();
//        }
//        return ResponseEntity.notFound().build();
//    }
//
//}
