package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;


@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

//  1) url: /register, 2) register failed
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

//  upload registration form
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        user.setId(null);
        logger.info("Register request: " + user.toString());

        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "Your registration is almost successful! We have sent an activation email to your mailbox. Please activate it as soon as possible!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/101/code
//    @PathVariable 是从 URL 路径模板 中提取变量值，并绑定到方法的参数上
    //点击 邮件中的 链接时，触发/调用这个函数
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "Activation is successful, your account can be used now!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "Invalid operation, the account has been activated!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "Activation failed. The activation code you provided is incorrect!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path="/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码的数字和返回给浏览器显示的图片
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将图片（验证码）存入session！！！
        // session.setAttribute("kaptcha", text);

        // 验证码的归属者 给这个用户的短暂凭证
        String kaptchaOwner = CommunityUtil.generateUUID(); // 随机字符串标识当前的用户
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);// cookie里面存的kaptchaOwner就是个随机字符串
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        // 将当前用户临时凭证redisKey 和 验证码 存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);//超过60s失效

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }

    }

    @RequestMapping(path="/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme, Model model/*, HttpSession session*/, HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // code是用户前端表单input的，kaptcha是redis库里面存的get出来的，但是kaptcha是cookie里面kaptchaOwner随机字符串经过RedisKeyUtil转换成rediskey再去redis库里面get到的
//        // 先检查验证码是否正确
//        String kaptcha= session.getAttribute("kaptcha").toString();
//        if(StringUtils.isBlank(kaptcha)){
//            model.addAttribute("codeMsg","验证码不正确！");
//            return "/site/login";
//        }
        // 检查验证码
//        String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        if(StringUtils.isNotBlank(kaptchaOwner)){ //没有失效
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        // 先检查验证码是否正确
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            logger.info("Login request: " + code + ", " + kaptcha);
            model.addAttribute("codeMsg", "The verification code is wrong!");
            return "/site/login";
        }
        //
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS; // remember me 是否勾选
        Map<String, Object>map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie); // response.add cookie发送给浏览器页面
            System.out.println("Ticket: " + map.get("ticket")); // loginTicket.getTicket()
            System.out.println("Expired Seconds: " + expiredSeconds);
            return "redirect:/index";
        }else{
            // 错误信息往model里面存
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }

    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
//        System.out.println(ticket);
        userService.logout(ticket);

        // 清理SecurityContextHollder中的权限！！！
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
