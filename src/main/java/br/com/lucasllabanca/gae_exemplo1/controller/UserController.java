package br.com.lucasllabanca.gae_exemplo1.controller;

import br.com.lucasllabanca.gae_exemplo1.exception.UserAlreadyExistsException;
import br.com.lucasllabanca.gae_exemplo1.model.User;
import br.com.lucasllabanca.gae_exemplo1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = Logger.getLogger(UserController.class.getName());

    @Autowired
    UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<User> getUsers() {
        return userRepository.getUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<User> saveUser(@RequestBody User user) {
        try {
            return new ResponseEntity<User>(userRepository.saveUser(user), HttpStatus.CREATED);
        } catch (UserAlreadyExistsException e) {
            log.info(e.getMessage());
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @PutMapping(path = "/byemail")
    public ResponseEntity<User> updateUser(@RequestBody User user,
                                           @RequestParam("email") String email,
                                           Authentication authentication) {

    }

}
