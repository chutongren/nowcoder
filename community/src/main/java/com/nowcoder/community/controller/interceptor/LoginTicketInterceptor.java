package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //cookie中获取凭证
        String ticket = CookieUtil.getValue(request,"ticket");

        if(ticket!=null){
            //从cookie中获取凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //检查凭证是否有效？ 凭证是否已经失效了？ 超时了？
            if(loginTicket!=null && loginTicket.getStatus()==0 && loginTicket.getExpired().after(new Date())){
                // 登录有效的状态，所以可以查user
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中的持有用户 多对一
                hostHolder.setUser(user); //user暂存到HostHolder里，暂存到线程Thread中

                // 在模版引擎之前就会用到user
                // ！！！构建用户认证的结果，并存入SecurityContext，以便于Security进行授权。
                // 认证结果 存着，类似之前 登录成功结果存着
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        user.getPassword(),
                        userService.getAuthorities(user.getId())
                );
                // 实现持久化的第一个步骤：在运行前，SecurityContextHolder从SecurityContextRepository中读取SercurityContext
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                // 实现持久化的第二个步骤：运行结束后，SecurityContextHolder将修改后的SercurityContext再存入SecurityContextRepository中，以便下次访问
                securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if(user!=null && modelAndView!=null){
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        // 清理权限
        SecurityContextHolder.clearContext();
    }
}
