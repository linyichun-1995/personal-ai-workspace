package com.example.workspace.user.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.user.application.UserService;
import com.example.workspace.user.dto.CurrentUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(CurrentUser currentUser) {
        return userService.me(currentUser);
    }
}
