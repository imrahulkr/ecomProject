package com.ecommerce.project.security.handler;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OneTimeExchangeCodeStore {

    private record Entry(String accessToken, Instant expiresAt){}

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private static final long TTL_SECONDS = 30;

    public OneTimeExchangeCodeStore() {}

    public  String store(String accessToken){
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(accessToken, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    /*
    * single-use : removes the code on read regardless of outcome.
    * */

    public String consume(String code){
        Entry entry = codes.remove(code);
        if(entry == null || Instant.now().isAfter(entry.expiresAt)){
            throw new IllegalArgumentException("Invalid or expired exchange code");
        }
        return entry.accessToken();
    }
}
