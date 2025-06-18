package com.nowcoder.community.service;

import com.nowcoder.community.controller.LoginController;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.security.core.GrantedAuthority;


import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {
        User user = getCache(id);
        if(user == null){
            user = initCache(id);
        }
        return user;
//        return userMapper.selectById(id);
    }
//    user是传入的参数（前端注册表单输入的值）map是返回给前端的信息（注册成功/失败信息）
//    model.addAttribute("user", user)
    public Map<String, Object> register(@ModelAttribute("user") User user) {
        // 显式将 id 设为 null
        user.setId(null);
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);  // 1 = normal user, 2 = admin, 3 = author/poster
        user.setStatus(0); // 0 = unactivated, 1 = activated
        user.setActivationCode(CommunityUtil.generateUUID());//generate activationcode -> send email-> registration successfully!
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送的邮件模版是一个html文件，需要用户邮箱和一个激活链接，放入context
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);

        // 模版引擎templateEngine把context放入html里面
        String content = templateEngine.process("/mail/activation", context);

        System.out.println("发往: " + user.getEmail());
        System.out.println("内容: " + content);
        // mailClient 帮你做发送邮件这个动作
        mailClient.sendMail(user.getEmail(), "Please activate your account - NowCoder", content);

        return map; // 返回报错信息到LoginController，然后再返回到前端页面
    }

    // return的都是提示信息，成功/错误/什么问题。如果code对的，那就是成功激活

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }


    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>(); // 放入model的错误信息

        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg","账号不能为空！");
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg","密码不能为空！");
        }

        //对合法性进行验证，验证username是否存在是否已激活
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usernameMsg","该账号未激活！");
            return map;
        }

        //验证密码
        logger.info("password input: " + password);
        password = CommunityUtil.md5(password + user.getSalt());
        logger.info("password compare: " + password + " " + user.getPassword());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        //登录成功，生成登录凭证！
        LoginTicket loginTicket  = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); // 核心
        loginTicket.setStatus(0); // 有效
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 10000*60)); //ms

//        loginTicketMapper.insertLoginTicket(loginTicket);
//        map.put("ticket","loginTicket.getTicket()");

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);// 把这个对象序列化为一个json格式的字符串


        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);

//        LoginTicket loginTicket = loginTicketMapper.selectByTicket(ticket);
//        System.out.println(loginTicket);
//        loginTicketMapper.updateStatus(ticket, 1);
//        LoginTicket loginTicket = loginTicketMapper.selectByTicket(ticket);
//        System.out.println(loginTicket);
    }

    //查询login凭证 login ticket
    public LoginTicket findLoginTicket(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
//        return loginTicketMapper.selectByTicket(ticket);
    }

    //
    public int updateHeader(int userId, String headerUrl){
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }


    /**
     * Gets authorities.
     * 查询某个用户的权限（用于给Spring Security返回用户权限）
     *
     * @param userId the user id
     * @return the authorities
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new LinkedList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}