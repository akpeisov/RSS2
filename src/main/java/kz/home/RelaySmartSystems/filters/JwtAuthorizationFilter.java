package kz.home.RelaySmartSystems.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import kz.home.RelaySmartSystems.model.TokenData;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class JwtAuthorizationFilter extends OncePerRequestFilter {
    @Value("${jwt.public.keyFront}")
    private String KeyForFront;// = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyd0ug493uWYFYD1fhWXqufC5V5mfCQNuLUuKNUnKTWtjJ3P+V3FIOMXqaluEiRmmcSuPkG/4mWweV9Xt0U91BEY5Pt2igc0PWrSdc19Xx0j95WjcRvac0ZEn9QMM7e0QB2b6ylE1GCYbZW4pyRNoy48BIg5tEKGOXIwOPfZV+zLIcTJm85ZopNfyVlIemoks190rU8EFanSX5f7IQpYqKYInE8KypCrhjBrfSuv6ICGxq1tImFo57NpGCax9FhzA0EwmYUnoXciyXUbq+fjt7c+646NlO+P6U5hkMcehOgSmm2BwnVXCdSTX704Hf2zbqxVQk0M7VBAftnPge3DEJwIDAQAB";
    @Value("${jwt.public.keyController}")
    private String KeyForESP;// =   "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzKJvjOk84SPdY+bhzCaEdQNQcY9ekCWGHpwopXtEFoRi0kbTP+5hBN+nPCftuD2VGnxpW1qQwegWa4TkyL/CVDZE2BiotXHuMyfVQKC+YHyfJ/LgRkG119RD1YB9swn0UCO/recaGHcOd4UMaJOvuJf85zNs5CCIN85b6oIEMzbzRvZNvJqya2ZmG6/+nWW2RooKhHTwpNkKAjBaPygUjkr5744yxb6Q1tbow/CJlofPz8YmerkWmUEB3nbvqEsJp0wfgIU51WZL2OW1e0ndABHqv0tfqJbfFgmWjHY02YE9r01YuWOVfn2fQ0xI7pQO0FlNCzI0MpiCPk2/IB4XWQIDAQAB";
    @Value("${env}")
    private String env;

    public TokenData validateToken(String token, String type) {
        String publicKey = KeyForESP;
        if ("web".equalsIgnoreCase(type)) {
            publicKey = KeyForFront;
        }
        TokenData tokenData = new TokenData();
        if (token != null) {
            try {
                Claims claims;
                if ("dev".equals(env) && "web1".equalsIgnoreCase(type)) { // TODO : убрать после теста!!!
                    //claims = Jwts.parser().unsecured().build().parseSignedClaims(token).getPayload();
                    tokenData.setUsername("user");
                    return tokenData;
                } else if ("dev".equals(env) && "RC1".equalsIgnoreCase(type)) { // TODO : убрать после теста!!!
                    tokenData.setMac(token);
                    return tokenData;
                } else {
                    claims = Jwts.parser().verifyWith(generateJwtKeyDecryption(publicKey))
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                }

                tokenData.setUsername(claims.get("preferred_username", String.class));
                tokenData.setMac(claims.get("mac", String.class));
                tokenData.setModel(claims.get("model", String.class));
                //claims.getSubject(); // Имя пользователя preferred_username
            } catch (SignatureException e) {
                tokenData.setErrorText("Invalid signature");
                tokenData.setException(e);
                //logger.error("--- SignatureException");
            } catch (NoSuchAlgorithmException e) {
                //logger.error("--- NoSuchAlgorithmException")
                tokenData.setErrorText("NoSuchAlgorithmException");
                tokenData.setException(e);
                //throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                //logger.error("--- InvalidKeySpecException");
                tokenData.setErrorText("InvalidKeySpecException");
                tokenData.setException(e);
                //throw new RuntimeException(e);
            } catch (Exception e) {
                tokenData.setErrorText(e.getLocalizedMessage());
                tokenData.setException(e);
                //logger.error("--- Exception " + e.getLocalizedMessage());
            }
        }
        return tokenData;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractJwtFromHeader(request);
        //logger.info(String.format("token %s", token));
        request.setAttribute("token", token);
        TokenData tokenData = validateToken(token, "web");
        request.setAttribute("username", tokenData.getUsername());
        if ("dev".equals(env) && tokenData.getUsername() == null) {
            request.setAttribute("username", "user");
        }
        request.setAttribute("errorText", tokenData.getErrorText());
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
//        logger.info(String.format("authorizationHeader %s", authorizationHeader));
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private PublicKey generateJwtKeyDecryption(String jwtPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.decodeBase64(jwtPublicKey);
        X509EncodedKeySpec x509EncodedKeySpec=new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }
}