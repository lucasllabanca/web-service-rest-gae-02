package br.com.lucasllabanca.gae_exemplo1.repository;

import br.com.lucasllabanca.gae_exemplo1.exception.UserAlreadyExistsException;
import br.com.lucasllabanca.gae_exemplo1.exception.UserNotFoundException;
import br.com.lucasllabanca.gae_exemplo1.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Repository
public class UserRepository {

    private static final String ADMIN_EMAIL = "admin@admin.com.br";
    private static final Logger log = Logger.getLogger(UserRepository.class.getName());

    //this annotation finds in the project the class that implements PasswordEncoder
    //Not needed to create an instance, it creates by itself
    //It works 'cause of @Configuration plus @Bean
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String USER_KIND = "users";
    private static final String USER_KEY = "userKey";

    private static final String PROPERTY_ID = "userId";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_FCM_REG_ID = "fcmRegId";
    private static final String PROPERTY_LAST_LOGIN = "lastLogin";
    private static final String PROPERTY_LAST_FCM_REGISTER = "lastFCMRegister";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_ENABLED = "enabled";

    @PostConstruct
    public void init() {

        User adminUser;
        Optional<User> optUser = this.getByEmail(ADMIN_EMAIL);

        try {
            if (optUser.isPresent()) {
                adminUser = optUser.get();
                if (!adminUser.getRole().equals("ROLE_ADMIN")){
                    adminUser.setRole("ROLE_ADMIN");
                    this.updateUser(adminUser, ADMIN_EMAIL);
                }
            } else {
                adminUser = new User();
                adminUser.setRole("ROLE_ADMIN");
                adminUser.setEnabled(true);
                adminUser.setPassword("admin");
                adminUser.setEmail(ADMIN_EMAIL);
                this.saveUser(adminUser);
            }
        } catch (UserAlreadyExistsException | UserNotFoundException e) {
            log.severe("Falha ao criar ou alterar usuário ADMIN");
            log.severe(e.getMessage());
        }
    }

    public Optional<User> getByEmail (String email) {
        log.info("Get user by email: " + email);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastoreService.prepare(query).asSingleEntity();

        if (userEntity != null) {
            return Optional.of(entityToUser(userEntity));
        } else {
            return Optional.empty();
        }
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query query = new Query(USER_KIND).addSort(PROPERTY_EMAIL, Query.SortDirection.ASCENDING);

        List<Entity> userEntities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity userEntity : userEntities) {
            User user = entityToUser(userEntity);
            users.add(user);
        }

        return users;
    }

    public User deleteUser(String email) throws UserNotFoundException {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastoreService.prepare(query).asSingleEntity();

        if (userEntity != null) {
            datastoreService.delete(userEntity.getKey());
            return entityToUser(userEntity);
        } else {
            throw new UserNotFoundException("Usuário " + email + " não encontrado");
        }
    }

    public User saveUser(User user) throws UserAlreadyExistsException {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        if (!checkIfEmailExist(user)) {
            Key userKey = KeyFactory.createKey(USER_KIND, USER_KEY);
            Entity userEntity = new Entity(USER_KIND, userKey);

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userToEntity(user, userEntity, true);

            datastoreService.put(userEntity);
            user.setId(userEntity.getKey().getId());
            return user;

        } else {
            throw new UserAlreadyExistsException("Usuário" + user.getEmail() + " já existe");
        }
    }

    public User updateUser(User user, String email) throws UserAlreadyExistsException, UserNotFoundException {

        if (!checkIfEmailExist(user)) {

            DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

            Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
            Query query = new Query(USER_KIND).setFilter(filter);
            Entity userEntity = datastoreService.prepare(query).asSingleEntity();

            if (userEntity != null) {
                userToEntity(user, userEntity, false);
                datastoreService.put(userEntity);
                user.setId(userEntity.getKey().getId());
                return user;

            } else {
                throw new UserNotFoundException("Usuário " + email + " não encontrado");
            }

        } else {
            throw new UserAlreadyExistsException("Usuário" + user.getEmail() + " já existe");
        }
    }

    private boolean checkIfEmailExist (User user) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, user.getEmail());
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastoreService.prepare(query).asSingleEntity();

        if (userEntity == null) {
            return false;
        } else {
            if (user.getId() == null) {
                return true;
            } else {
                return userEntity.getKey().getId() != user.getId();
            }
        }
    }

    private void userToEntity (User user, Entity userEntity, boolean updatePassword) {
        userEntity.setProperty(PROPERTY_ID, user.getId());
        userEntity.setProperty(PROPERTY_EMAIL, user.getEmail());
        userEntity.setProperty(PROPERTY_FCM_REG_ID, user.getFcmRegId());
        userEntity.setProperty(PROPERTY_LAST_LOGIN, user.getLastLogin());
        userEntity.setProperty(PROPERTY_LAST_FCM_REGISTER, user.getLastFCMRegister());
        userEntity.setProperty(PROPERTY_ROLE, user.getRole());
        userEntity.setProperty(PROPERTY_ENABLED, user.isEnabled());

        if (updatePassword)
            userEntity.setProperty(PROPERTY_PASSWORD, user.getPassword());
    }

    private User entityToUser (Entity userEntity) {
        User user = new User();
        user.setId(userEntity.getKey().getId());
        user.setEmail((String) userEntity.getProperty(PROPERTY_EMAIL));
        user.setPassword((String) userEntity.getProperty(PROPERTY_PASSWORD));
        user.setFcmRegId((String) userEntity.getProperty(PROPERTY_FCM_REG_ID));
        user.setLastLogin((Date) userEntity.getProperty(PROPERTY_LAST_LOGIN));
        user.setLastFCMRegister((Date) userEntity.getProperty(PROPERTY_LAST_FCM_REGISTER));
        user.setRole((String) userEntity.getProperty(PROPERTY_ROLE));
        user.setEnabled((Boolean) userEntity.getProperty(PROPERTY_ENABLED));
        return user;
    }

}
