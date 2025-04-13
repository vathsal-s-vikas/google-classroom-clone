package com.classroom.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.classroom.model.User;
import com.classroom.repository.UserRepository;

@RestController
public class RegistrationController {


    @Autowired
    private UserRepository myUserRepository;

    @PostMapping(value = "/req/signup/",consumes = "application/json")
    public User createUser(@RequestBody User user){
        return myUserRepository.save(user);
    }
}
