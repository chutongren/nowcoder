package com.nowcoder.community.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;




public class CookieUtil {

    public static String getValue(HttpServletRequest request, String name) {
        if(request==null||name==null){
            throw new IllegalArgumentException("The request parameter name can not be null");
        }
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
