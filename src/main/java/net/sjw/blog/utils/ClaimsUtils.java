package net.sjw.blog.utils;

import io.jsonwebtoken.Claims;
import net.sjw.blog.entity.User;

import java.util.HashMap;
import java.util.Map;

public class ClaimsUtils {
    private static final String ID = "id";
    private static final String USER_NAME = "user_name";
    private static final String ROLES = "roles";
    private static final String AVATAR = "avatar";
    private static final String EMAIL = "email";
    private static final String SIGN = "sign";
    private static final String FROM = "from";

    public static Map<String, Object> userToClaims(User user,String from) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ID, user.getId());
        claims.put(USER_NAME, user.getUserName());
        claims.put(ROLES, user.getRoles());
        claims.put(AVATAR, user.getAvatar());
        claims.put(EMAIL, user.getEmail());
        claims.put(SIGN, user.getSign());
        claims.put(FROM, from);
        return claims;
    }

    public static User claimsToUser(Claims claims) {
        User user = new User();
        String id = (String) claims.get(ID);
        user.setId(id);
        String userName = (String) claims.get(USER_NAME);
        user.setUserName(userName);
        String roles = (String) claims.get(ROLES);
        user.setRoles(roles);
        String avatar = (String) claims.get(AVATAR);
        user.setAvatar(avatar);
        String email = (String) claims.get(EMAIL);
        user.setEmail(email);
        String sign = (String) claims.get(SIGN);
        user.setSign(sign);
        return user;
    }

    public static String getFrom(Claims claims) {
        return (String) claims.get(FROM);
    }
}
