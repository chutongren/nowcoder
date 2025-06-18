package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("chutong.ren@tum.de", "TEST", "Welcome.");
    }
    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "renchutongdhu@163.com");
//        context.setVariable("url", "http://localhost:8082/community/activation/161/5d33e33fb2c0487f879a84b393bf3f5e");

        String content = templateEngine.process("/mail/demo", context);
        System.out.println("111");

        mailClient.sendMail("renchutongdhu@163.com", "HTML", content);
    }

    @Test
    public void testSendActivationMail() {
        String activationLink = "http://localhost:8080/community/activation/101/code123";
        Context context = new Context();
        context.setVariable("email", "chutong.ren@tum.de");
        context.setVariable("url", activationLink);

//        String content = templateEngine.process("/mail/activation", context);
//        System.out.println("111" );
//        mailClient.sendMail("chutong.ren@tum.de", "TEST - Activation", content);
//        System.out.println("222" );

        System.out.println("开始渲染模板");
        String content = templateEngine.process("mail/activation", context);  // 注意去掉开头的 /
        System.out.println("模板渲染完成，内容长度：" + content.length());

        System.out.println("开始发送邮件");
        mailClient.sendMail("chutong.ren@tum.de", "TEST - Activation", content);
        System.out.println("邮件发送方法调用完成");
    }

}
