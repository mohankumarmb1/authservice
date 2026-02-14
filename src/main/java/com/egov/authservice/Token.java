package com.egov.authservice;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "tokens")
public class Token {

    @Id
    String id;
    String username;
    String state;
    Instant createdAt;
    Instant lastUsedAt;
    Instant expiredAt;
    Long expiry;
}
