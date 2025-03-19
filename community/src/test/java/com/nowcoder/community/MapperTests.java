package com.nowcoder.community;


import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Test
    public void testSelectById() {
        User user = userMapper.selectById(101);
        System.out.println(user);
    }

    @Test
    public void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149, 0, 10);
        for(DiscussPost post : list) {
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

//    @Test
//    public void testInsertUser() {
//        User user = new User();
//        user.setUsername("a");
////        user.setPassword("q");
//        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
//        user.setPassword(CommunityUtil.md5("a" + user.getSalt()));
//        user.setType(0);//普通用户
////        user.setSalt("q");
//        user.setEmail("a@qq.com");
//        user.setHeaderUrl("http://www.nowcoder.com/102.png");
//        user.setStatus(1);
//        user.setCreateTime(new Date());
//
//        int rows = userMapper.insertUser(user);
//        System.out.println(rows);
//        System.out.println(user.getId());
//    }
    @Test
    public void updateHeaderUrl(){
        int rows = userMapper.updateHeader(159, "http://images.nowcoder.com/head/1t.png");
        System.out.println(rows);
//        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
    }

//    @Test
//    public void updateUser() {
//        int rows = userMapper.updateStatus(150, 1);
//        System.out.println(rows);
//
//        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
//        System.out.println(rows);
//
//        rows = userMapper.updatePassword(150, "hello");
//        System.out.println(rows);
//    }


//    @Test
//    public void testInsertLoginTicket() {
//        LoginTicket loginTicket = new LoginTicket();
//        loginTicket.setUserId(101);
//        loginTicket.setTicket("test"); //后面实现随机生成字符串
//        loginTicket.setStatus(0);
//        loginTicket.setExpired(new Date(System.currentTimeMillis() + 10000*60));
//
//        loginTicketMapper.insertLoginTicket(loginTicket);
//    }

    @Test
    public void testSelectLoginTicket() {
        loginTicketMapper.updateStatus("25e83916907341e1917474dea5f3ddce", 0);
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("25e83916907341e1917474dea5f3ddce");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("25e83916907341e1917474dea5f3ddce", 1);
        loginTicket = loginTicketMapper.selectByTicket("25e83916907341e1917474dea5f3ddce");
        System.out.println(loginTicket);
    }

}
