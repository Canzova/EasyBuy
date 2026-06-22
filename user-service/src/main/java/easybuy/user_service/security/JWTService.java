package easybuy.user_service.security;

import easybuy.user_service.dto.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JWTService {

    @Value("${jwt.secret-key:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI4ZjQxYzY0Yi0yYjY0LTQ2NWEtYjM4Ny0zYjQ5YjQ5MjA1YzAiLCJuYW1lIjoiVGVzdCBVc2VyIiwiaWF0IjoxNzgyMTQ1NjAwfQ.v4sV1v6x4z9H7N4uK8cQmJ5Wf2YtR1nP3sE6dL0aB9Q}")
    private String secretKey;

    @Value("${jwt.access-token-validation-milliseconds:3600000}")  // 1 Hour
    private long accessTokenValidityMilliseconds;

    @Value("${jwt.refresh-token-validation-milliseconds:604800000}")  // 7 Days
    private long refreshTokenValidityMilliseconds;

    // Convert secretKey from string to SecretKey class
    /*
        Convert secretKey from string to SecretKey class
        We are converting our Base64 String secret key into an array of bytes
        Then we are encoding it with hmacShah (Cryptographic alogo) to convert it to SecretKey object
     */
    public SecretKey getSecretKey() {
        byte[] secretKeyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(secretKeyBytes);
    }

    // Generate new access-token
    public String generateNewAccessToken(String userId, String email, Role role){

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("token-type", "access-token");

        return getToken(email, claims);
    }

    public String generateRefreshToken(String userId, String email, Role role){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("token-type", "refresh-token");
        
        return getToken(email, claims);
    }

    public boolean isTokenValid(String token, String userName){
        if(userName==null || !userName.equals(extractUsername(token))) return false;
        return !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpirationTime(token).before(new Date());
    }

    public Date extractExpirationTime(String token){
        return extractToken(token, claims -> claims.getExpiration());
    }

    public String extractUsername(String token){
        return extractToken(token, Claims::getSubject);
    }

    public String extractTokenType(String token){
        final Claims claims = extractAllJwtClaims(token);
        return claims.get("token-type").toString();
    }

    public String extractRole(String token){
        final Claims claims = extractAllJwtClaims(token);
        return claims.get("role").toString();
    }

    public <T> T extractToken(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllJwtClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllJwtClaims(String token){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getToken(String email, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidityMilliseconds))
                .signWith(getSecretKey())
                .compact();
    }
}
