package com.egov.authservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("api/v1/internal")
public class InternalRestController {

    private static final Logger logger = LoggerFactory.getLogger(InternalRestController.class);

    @Autowired
    CredentialRepository credentialRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    Producer producer;

    @GetMapping("/validate")
    ResponseEntity<Principal> validate(@RequestHeader("Authorization") String token) throws JsonProcessingException {
        logger.info("Received validate request for token: " + token);
        Optional<Token> fetchedToken =  tokenRepository.findById(token);

        if(fetchedToken.isPresent())
        {
            logger.info("Token is Present in the Database");
            if(fetchedToken.get().getState().equals("ACTIVE"))
            {
                logger.info("Token is Valid");

                Optional<Credential> fetchedCredential = credentialRepository.findByUsername(fetchedToken.get().getUsername());
                if(fetchedCredential.isEmpty())
                {
                    Principal principal = new Principal();
                    principal.setCust_id("");
                    principal.setState("INVALID");

                    AuthEvent authEvent = new AuthEvent();
                    authEvent.setCust_id(fetchedCredential.get().getId());
                    authEvent.setType("CUSTOMER INVALIDATED");
                    producer.pubAuthEvent_1(authEvent);

                    return ResponseEntity.ok(principal);
                }

                Principal principal = new Principal();
                principal.setCust_id(fetchedCredential.get().getId());
                principal.setState("VALID");

                AuthEvent authEvent = new AuthEvent();
                authEvent.setCust_id(fetchedCredential.get().getId());
                authEvent.setType("CUSTOMER VALIDATED");
                producer.pubAuthEvent_1(authEvent);

                return ResponseEntity.ok(principal);
            }
            else
            {

                Optional<Credential> fetchedCredential = credentialRepository.findByUsername(fetchedToken.get().getUsername());

                logger.info("Token is Invalid");
                Principal principal = new Principal();
                principal.setCust_id(fetchedToken.get().getId());
                principal.setState("INVALID");

                AuthEvent authEvent = new AuthEvent();
                authEvent.setCust_id(fetchedCredential.get().getId());
                authEvent.setType("CUSTOMER VALIDATED");
                producer.pubAuthEvent_1(authEvent);

                return ResponseEntity.ok(principal);
            }
        }
        else {

            AuthEvent authEvent = new AuthEvent();
            authEvent.setCust_id("NON-EXISTENT CUSTOMER");
            authEvent.setType("CUSTOMER VALIDATED");
            producer.pubAuthEvent_1(authEvent);

            logger.info("Token is Invalid");
            Principal principal = new Principal();
            principal.setCust_id(fetchedToken.get().getId());
            principal.setState("INVALID");
            return ResponseEntity.ok(principal);
        }
    }
}
