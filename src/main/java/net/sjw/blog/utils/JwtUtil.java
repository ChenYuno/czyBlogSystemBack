package net.sjw.blog.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    //盐值
    private static String key = "47da4f9c96730bda1407a285a1064b4d";

    //3小时
    private static long ttl =Constants.TimeMillions.HOUR_2;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    /**
     * @param claims 载荷内容
     * @param ttl    有效时长
     * @return
     */
    public static String createToken(Map<String, Object> claims, long ttl) {
        JwtUtil.ttl = ttl;
        return createToken(claims);
    }


    public static String createRefreshToken(String userId, long ttl) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        JwtBuilder builder = Jwts.builder().setId(userId)
                .setIssuedAt(now)
                .signWith(SignatureAlgorithm.HS256, key);
        if (ttl > 0) {
            builder.setExpiration(new Date(nowMillis + ttl));
        }
        return builder.compact();
    }

    /**
     *
     * @param claims 载荷
     * @return token
     */
    public static String createToken(Map<String, Object> claims) {

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .signWith(SignatureAlgorithm.HS256, key);

        if (claims != null) {
            builder.setClaims(claims);
        }

        if (ttl > 0) {
            builder.setExpiration(new Date(nowMillis + ttl));
        }
        return builder.compact();
    }

    public static Claims parseJWT(String jwtStr) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(jwtStr)
                .getBody();
    }

    public static void main(String[] args) {
//        String token = JwtUtil.createJWT("1316668431843495937",
//                "测试用户注册",
//                null);
//        System.out.println(token);
        // eyJhbGciOiJIUzI1NiJ9.
        // eyJqdGkiOiIxMzE2NjY4NDMxODQzNDk1OTM3Iiwic3ViIjoi5rWL6K-V55So5oi35rOo5YaMIiwiaWF0IjoxNjAyNzU2MTAwLCJleHAiOjE2MDI3NjY5MDB9.
        // uDpuO7L8YeVY_wAXeKJsdKcVr7RlKr6gKLivWJECTqI
//        System.out.println(DigestUtils.md5DigestAsHex("czy_blog_system-=".getBytes()));

        Claims claims = JwtUtil.parseJWT("eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxMzE2NjY4NDMxODQzNDk1OTM3Iiwic3ViIjoi5rWL6K-V55So5oi35rOo5YaMIiwiaWF0IjoxNjAyNzU2MTAwLCJleHAiOjE2MDI3NjY5MDB9.uDpuO7L8YeVY_wAXeKJsdKcVr7RlKr6gKLivWJECTqI");
        System.out.println("userId ==> " + claims.getId());
        System.out.println("userName ==> " + claims.getSubject());

    }
}
