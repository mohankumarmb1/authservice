package com.rs.authservice;

import lombok.Data;

@Data
public class AuthEvent {

    String cust_id;
    String type; //SIGNUP, LOGIN...
    //...

}
