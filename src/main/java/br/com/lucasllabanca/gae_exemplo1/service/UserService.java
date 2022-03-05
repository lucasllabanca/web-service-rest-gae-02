package br.com.lucasllabanca.gae_exemplo1.service;

import br.com.lucasllabanca.gae_exemplo1.model.User;
import br.com.lucasllabanca.gae_exemplo1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("userDetailsService")
public class UserService implements UserDetailsService {

    //DI of User Repository
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       Optional<User> optUser = userRepository.getByEmail(username);
       if (optUser.isPresent()) {
           userRepository.updateUserLogin(optUser.get());
           return optUser.get();
       } else {
           throw new UsernameNotFoundException("Usuário não encontrado");
       }
    }
}
