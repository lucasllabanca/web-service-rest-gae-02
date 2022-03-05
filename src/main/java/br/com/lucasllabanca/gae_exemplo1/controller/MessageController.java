package br.com.lucasllabanca.gae_exemplo1.controller;

import br.com.lucasllabanca.gae_exemplo1.model.Product;
import br.com.lucasllabanca.gae_exemplo1.model.User;
import br.com.lucasllabanca.gae_exemplo1.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Http;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/message")
public class MessageController {

    private static final Logger log = Logger.getLogger(MessageController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://gae-exemplo1-aula.firebaseio.com")
                    .build();
            log.info("FirebaseApp inicializado com sucesso!");
        } catch (IOException e) {
            log.info("Falha ao configurar FirebaseApp");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/sendproduct")
    public ResponseEntity<String> sendProduct(@RequestParam("email") String email,
                                              @RequestParam("ProductCode") int productCode) {

        Optional<User> optUser = userRepository.getByEmail(email);

        if (optUser.isPresent()) {

            User user = optUser.get();
            Optional<Product> optProduct = findProduct(productCode);

            if (optProduct.isPresent()){

                String fcmRegId = user.getFcmRegId();

                if (fcmRegId != null) {

                    try {
                        Message message = Message.builder()
                                .putData("product", objectMapper.writeValueAsString(optProduct.get()))
                                .setToken(fcmRegId)
                                .build();

                        String response = FirebaseMessaging.getInstance().send(message);
                        log.info("Produto enviado: " + optProduct.get().getName());
                        log.info("Resposta do FCM: " + response);
                        return new ResponseEntity<>("Mensagem enviada com o produto " + optProduct.get().getName(), HttpStatus.OK);
                    } catch (FirebaseMessagingException | JsonProcessingException e) {
                        log.severe("Falha ao enviar mensagem pelo FCM: " + e.getMessage());
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    }

                } else {
                    log.severe("Usuário não registrado");
                    return new ResponseEntity<>("Usuário não registrado", HttpStatus.PRECONDITION_FAILED);
                }

            } else {
                log.severe("Produto não encontrado");
                return new ResponseEntity<>("Produto não encontrado", HttpStatus.NOT_FOUND);
            }

        } else {
            log.severe("Usuário não encontrado");
            return new ResponseEntity<>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }

    }

    private Optional<Product> findProduct(int code) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);

        Query query = new Query("Products").setFilter(codeFilter);

        Entity entity = datastoreService.prepare(query).asSingleEntity();

        if (entity != null) {
            Product product = entityToProduct(entity);
            return Optional.of(product);
        } else {
            return Optional.empty();
        }
    }

    private Product entityToProduct(Entity entity) {
        Product product = new Product();
        product.setId(entity.getKey().getId());
        product.setProductId((String)entity.getProperty("ProductId"));
        product.setName((String)entity.getProperty("Name"));
        product.setCode(Integer.parseInt(entity.getProperty("Code").toString()));
        product.setModel((String)entity.getProperty("Model"));
        product.setPrice(Float.parseFloat(entity.getProperty("Price").toString()));
        return product;
    }
}
