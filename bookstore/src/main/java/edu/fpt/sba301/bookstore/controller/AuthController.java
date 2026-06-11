package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    private final JwtTokenProvider jwtUtil;

    public AuthController(JwtTokenProvider jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
//        User user = userService.findByUsername(username);
//        if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
//            return jwtUtil.generateToken(username, user.getRole().getName());
//        }
        return "";
    }
}