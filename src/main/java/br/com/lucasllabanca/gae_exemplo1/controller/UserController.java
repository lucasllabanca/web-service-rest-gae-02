package br.com.lucasllabanca.gae_exemplo1.controller;

import br.com.lucasllabanca.gae_exemplo1.exception.UserAlreadyExistsException;
import br.com.lucasllabanca.gae_exemplo1.exception.UserNotFoundException;
import br.com.lucasllabanca.gae_exemplo1.model.User;
import br.com.lucasllabanca.gae_exemplo1.repository.UserRepository;
import br.com.lucasllabanca.gae_exemplo1.util.CheckRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = Logger.getLogger(UserController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    CheckRole checkRole;

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
    public ResponseEntity<?> updateUser(@RequestBody User user,
                                        @RequestParam("email") String email,
                                        Authentication authentication) {

        if (user.getId() != null && user.getId() != 0) {
            boolean hasRoleAdmin = checkRole.hasRoleAdmin(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            if (hasRoleAdmin || userDetails.getUsername().equals(email)) {
                if (!hasRoleAdmin) {
                    user.setRole("ROLE_USER");
                }

                try {
                    return new ResponseEntity<User>(userRepository.updateUser(user, email), HttpStatus.OK);
                } catch (UserAlreadyExistsException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.PRECONDITION_FAILED);
                } catch (UserNotFoundException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>("Usuário não autorizado", HttpStatus.FORBIDDEN);
            }
        }else {
            return new ResponseEntity<>("Faltando parâmetro id", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/byemail")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email, Authentication authentication) {

        boolean hasRoleAdmin = checkRole.hasRoleAdmin(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (hasRoleAdmin || userDetails.getUsername().equals(email)) {
            Optional<User> optUser = userRepository.getByEmail(email);

            if (optUser.isPresent()){
                return new ResponseEntity<>(optUser.get(), HttpStatus.OK);
            }else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping(path = "/byemail")
    public ResponseEntity<User> deleteUser(@RequestParam("email") String email, Authentication authentication) {
        boolean hasRoleAdmin = checkRole.hasRoleAdmin(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (hasRoleAdmin || userDetails.getUsername().equals(email)) {

            try {
                return new ResponseEntity<>(userRepository.deleteUser(email), HttpStatus.OK);
            } catch (UserNotFoundException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

}
