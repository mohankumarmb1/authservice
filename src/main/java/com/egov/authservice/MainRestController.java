package com.egov.authservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class MainRestController
{
    private static final Logger logger = LoggerFactory.getLogger(MainRestController.class);

    @Autowired
    CredentialRepository credentialRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    Producer producer;

    @PostMapping("/signup")
    ResponseEntity<String> signup(@RequestBody Credential credential) throws JsonProcessingException {
        logger.info("Received signup request for user: " + credential.getUsername());
        Credential savedCredential =  credentialRepository.save(credential);
        logger.info("Saved credential for user: " + credential.getUsername());

        // create account logic
        // forward request to operations-service for account creation

        WebClient createAccountWebClient = (WebClient) ctx.getBean("createAccountWebClient");

        createAccountWebClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("internal/create/account")
                        .queryParam("cust_id", savedCredential.getId())
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(ex -> /* log it */ {logger.error("Error while creating account for user: " + credential.getUsername(), ex);})
                .subscribe( response -> logger.info("Account created successfully for user: " + credential.getUsername()+"with Account Id: "+response));

        AuthEvent authEvent = new AuthEvent();
        authEvent.setCust_id(savedCredential.getId());
        authEvent.setType("SIGNUP");
        producer.pubAuthEvent_1(authEvent);

        logger.info("Auth Event Published for user: " + credential.getUsername()+" with Account Id: "+savedCredential.getId()+"");
        logger.info("Signup Successful for user: " + credential.getUsername());

        return ResponseEntity.ok("New Signup Successful");
    }

    @GetMapping("/login")
    // should create and send back a JWT but for now we will generate a simple string token
    ResponseEntity<String> login(@RequestBody Credential credential) throws JsonProcessingException {
        // login logic
        logger.info("Received login request for user: " + credential.getUsername());
        Optional<Credential> fetchedCredential =  credentialRepository.findByUsername(credential.getUsername());

        if(fetchedCredential.isPresent())
        {
            logger.info("Username exists for the Login request");
            if(credential.getPassword().equals(fetchedCredential.get().getPassword()))
            {
                logger.info("Login Successful for user: "+credential.getUsername());
                // generate a JWT token here
                //String token = String.valueOf((new Random().nextInt(1000000)));
                Token token = new Token();
                token.setUsername(credential.getUsername());
                token.setExpiry(3600L);
                token.setState("ACTIVE");
                token.setCreatedAt(Instant.now());
                token.setLastUsedAt(Instant.now()); // This field will updated frequently

                String token_id = (tokenRepository.save(token)).getId();

                AuthEvent authEvent = new AuthEvent();
                authEvent.setCust_id(fetchedCredential.get().getId());
                authEvent.setType("LOGIN");
                producer.pubAuthEvent_1(authEvent);

                return ResponseEntity.ok(token_id);
            }
            else
            {
                logger.info("Login Failed due to Invalid Password");

                AuthEvent authEvent = new AuthEvent();
                authEvent.setCust_id(credential.getId());
                authEvent.setType("LOGIN FAILED");
                producer.pubAuthEvent_1(authEvent);

                return ResponseEntity.badRequest().body("Invalid Password");
            }
        }
        else
        {
            logger.info("Username does not exist for the Login request");

            AuthEvent authEvent = new AuthEvent();
            authEvent.setCust_id(credential.getId());
            authEvent.setType("LOGIN FAILED");
            producer.pubAuthEvent_1(authEvent);

            return ResponseEntity.badRequest().body("Invalid Username");
        }

    }



}
