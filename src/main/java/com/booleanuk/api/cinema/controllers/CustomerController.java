package com.booleanuk.api.cinema.controllers;

import com.booleanuk.api.cinema.models.Customer;
import com.booleanuk.api.cinema.models.Response;
import com.booleanuk.api.cinema.repositories.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping("customers")
public class CustomerController {
    private final CustomerRepository customerRepository;

    // @Autowired Prefer constructor injection to field injection. Done automatically by Spring
    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public ResponseEntity<Response<?>> add(@RequestBody Customer customer) {
        if (lacksRequiredFields(customer) || !isValidObject(customer)) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Response.success(this.customerRepository.save(customer)));
        }
        return ResponseEntity.badRequest().body(Response.BAD_REQUEST);
    }

    @GetMapping
    public Response<List<Customer>> getAll() {
        return Response.success(this.customerRepository.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<Response<?>> getById(@PathVariable int id) {
        if (this.customerRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.NOT_FOUND);
        }
        return ResponseEntity.ok(Response.success(this.customerRepository.findById(id).get()));
    }

    @PutMapping("{id}")
    public ResponseEntity<Response<?>> update(@PathVariable int id, @RequestBody Customer customer) {
        if (!isValidObject(customer)) {
            return ResponseEntity.badRequest().body(Response.BAD_REQUEST);
        }
        if (this.customerRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.NOT_FOUND);
        }

        Customer customerToUpdate = this.customerRepository.findById(id).get();
        // Update customer object with new values
        // If any field is not provided, the original value should not be changed. Any combination of fields can be updated.
        customerToUpdate.setName(customer.getName() != null ? customer.getName() : customerToUpdate.getName());
        customerToUpdate.setEmail(customer.getEmail() != null ? customer.getEmail() : customerToUpdate.getEmail());
        customerToUpdate.setPhone(customer.getPhone() != null ? customer.getPhone() : customerToUpdate.getPhone());
        customerToUpdate.setUpdatedAt(ZonedDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Response.success(this.customerRepository.save(customerToUpdate)));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Response<?>> deleteById(@PathVariable int id) {
        if (this.customerRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.NOT_FOUND);
        }

        Customer customerToDelete = this.customerRepository.findById(id).get();
        try {
            this.customerRepository.delete(customerToDelete);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("violation of foreign key constraint"));
        }

        return ResponseEntity.ok(Response.success(customerToDelete));
        // TODO Deleting customer should delete all tickets for that customer
    }

    private boolean lacksRequiredFields(Customer customer) {
        return Stream.of(customer.getName(), customer.getEmail(), customer.getPhone())
                .anyMatch(Objects::isNull);
    }

    private boolean isValidObject(Customer customer) {
        // TODO Validate email, phonenumber, etc if needed
        return true;
    }
}
