package br.com.lucasllabanca.gae_exemplo1.controller;

import br.com.lucasllabanca.gae_exemplo1.model.Product;
import com.google.appengine.api.datastore.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/products")
public class ProductController {

    private static final Logger log = Logger.getLogger(ProductController.class.getName());

    @GetMapping("/{code}")
    public ResponseEntity<Product> getProduct(@PathVariable int code) {
        //Product product = createProduct(code);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);

        Query query = new Query("Products").setFilter(codeFilter);

        Entity entity = datastoreService.prepare(query).asSingleEntity();

        if (entity != null) {
            Product product = entityToProduct(entity);
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {

        List<Product> products = new ArrayList<>();

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query query = new Query("Products").addSort("Code", Query.SortDirection.ASCENDING);

        List<Entity> entities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity entity : entities) {
            Product product = entityToProduct(entity);
            products.add(product);
        }

        /*for (int i = 1; i <= 5; i++) {
            products.add(createProduct(i));
        }*/

        return new ResponseEntity<List<Product>>(products, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestBody Product product) {
        //product.setProductId(Integer.toString(product.getCode()));

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        if (!checkIfCodeExist(product, product.getCode())) {
            Key productKey = KeyFactory.createKey("Products", "productKey");
            Entity productEntity = new Entity("Products", productKey);

            this.productToEntity(product, productEntity);

            datastoreService.put(productEntity);

            product.setId(productEntity.getKey().getId());

            return new ResponseEntity<Product>(product, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/{code}")
    public ResponseEntity<Product> deleteProduct(@PathVariable("code") int code) {
        //Product product = createProduct(code);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        log.fine("Tentando apagar produto com código=[" + code + "]");

        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);

        Query query = new Query("Products").setFilter(codeFilter);

        Entity entity = datastoreService.prepare(query).asSingleEntity();

        if (entity != null) {
            datastoreService.delete(entity.getKey());
            Product product = entityToProduct(entity);

            log.info("Producto com código=[" + code + "] apagado com sucesso");
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            log.severe("Erro ao apagar produto com código=[" + code + "]. Produto não encontrado");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(path = "/{code}")
    public ResponseEntity<Product> updateProduct(@PathVariable("code") int code, @RequestBody Product product) {

        if (product.getId() == 0)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        if (!checkIfCodeExist(product, code)) {

            DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

            Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
            Query query = new Query("Products").setFilter(codeFilter);
            Entity entity = datastoreService.prepare(query).asSingleEntity();

            if (entity != null) {

                productToEntity(product, entity);

                datastoreService.put(entity);

                product.setId(entity.getKey().getId());

                return new ResponseEntity<Product>(product, HttpStatus.CREATED);

            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        /*
        product.setProductId(Integer.toString(product.getCode()));
        product.setName("New name");
        */
    }

    @GetMapping("/{code}/{model}")
    public ResponseEntity<Product> getProductByCodeAndModel(@PathVariable int code, @PathVariable String model) {

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
        Query.Filter modelFilter = new Query.FilterPredicate("Model", Query.FilterOperator.EQUAL, model);

        Query.CompositeFilter codeAndModelFilter = Query.CompositeFilterOperator.and(codeFilter, modelFilter);

        Query query = new Query("Products").setFilter(codeAndModelFilter);

        Entity entity = datastoreService.prepare(query).asSingleEntity();

        if (entity != null) {
            Product product = entityToProduct(entity);
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /*private Product createProduct(int code) {
        Product product = new Product();
        product.setProductId(Integer.toString(code));
        product.setCode(code);
        product.setModel("Model" + code);
        product.setName("Product" + code);
        product.setPrice(10 * code);
        return product;
    }*/

    private void productToEntity(Product product, Entity entity) {
        entity.setProperty("ProductId", product.getProductId());
        entity.setProperty("Name", product.getName());
        entity.setProperty("Code", product.getCode());
        entity.setProperty("Model", product.getModel());
        entity.setProperty("Price", product.getPrice());
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

    private boolean checkIfCodeExist(Product product, int code) {

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, product.getCode());
        Query query = new Query("Products").setFilter(codeFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();

        if (productEntity == null) {
            return false;
        } else {
            if (productEntity.getKey().getId() == product.getId() && product.getCode() == code) {
                return false;
            } else {
                return true;
            }
        }
    }

}
