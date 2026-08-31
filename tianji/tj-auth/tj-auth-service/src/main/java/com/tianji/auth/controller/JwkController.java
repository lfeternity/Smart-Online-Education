package com.tianji.auth.controller;

import cn.hutool.core.codec.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("jwks")
@ApiIgnore
public class JwkController {

    private final KeyPair keyPair;

    @Autowired
    public JwkController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @GetMapping
    public String getJwk(){
        // TODO 可以加入clientId和clientSecret校验
        // 获取公钥并转码
        return Base64.encode(keyPair.getPublic().getEncoded());
    }

    /** Standard RFC 7517 JWKS endpoint for resource servers such as agent-service. */
    @GetMapping("/set")
    public Map<String, Object> getJwkSet() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", "tj-auth-rsa");
        jwk.put("n", encoder.encodeToString(unsigned(publicKey.getModulus().toByteArray())));
        jwk.put("e", encoder.encodeToString(unsigned(publicKey.getPublicExponent().toByteArray())));
        return Collections.singletonMap("keys", Collections.singletonList(jwk));
    }

    private byte[] unsigned(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            return java.util.Arrays.copyOfRange(value, 1, value.length);
        }
        return value;
    }
}
