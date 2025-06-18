package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nowcoder.community.util.CommunityConstant.ENTITY_TYPE_USER;

@Controller
@RequestMapping("/user") // 设定基本路径 /user
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // 注入上传路径，域名，项目的访问路径
    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService; //用到服务层里面updateHeaderUrl方法

    @Autowired
    private HostHolder hostHolder; //从hostHolder里面取当前用户

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;
    @Autowired
    private DiscussPostService discussPostService;


    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET) // 处理 /user/setting 的 GET 请求
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error", "please select a file");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "file format error");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        File dest = new File(uploadPath + '/' + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("file upload error: "+e.getMessage());
            throw new RuntimeException("file upload failed, server error",e);
        }

        // 更新当前用户的新头像路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        // 问题出在这里！是/user/header/，不是/user/header
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        return "redirect:/index" ;
    }

//    不需要loginrequired注解，不登录也可以看其他人的头像
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放的路径
        fileName = uploadPath + "/" + fileName;
        //文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try(
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(fileName);
                ) {
//            OutputStream os = response.getOutputStream();
//            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("file upload error: "+e.getMessage());

        }
    }


    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    // userId是url路径里面来的，肯定是前端click了啥 执行了href= (), 给url后面拼了userId
    @RequestMapping(path = "/profile/{userId}/myposts", method = RequestMethod.GET)
    public String getProfilePosts(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        page.setRows(discussPostService.findDiscussPostRows(userId));// 我一共发了多少条帖子？
//        page.setPath("/profile/{userId}/myposts");
        page.setPath("/user/profile/" + userId + "/myposts");

        List<DiscussPost> list = discussPostService.findUserDiscussPosts(userId, page.getOffset(), page.getLimit());

        List<Map<String, Object>> userDiscussPosts = new ArrayList<>();
        if (list != null) {

            for(DiscussPost post : list){
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
//                User user = userService.findUserById(post.getUserId());
//                map.put("user", user);
//                userDiscussPosts.add(map);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_USER, post.getUserId());
                map.put("likeCount", likeCount);

                userDiscussPosts.add(map);
            }
        }
        model.addAttribute("user", user); // 发帖人的信息
        model.addAttribute("userDiscussPosts", userDiscussPosts);
        return "/site/my-post";
    }


//    @RequestMapping(path = "/profile/{userId}/myreplies", method = RequestMethod.GET)
//    public String getProfileReplies(@PathVariable("userId") int userId, Model model, Page page) {
//        User user = userService.findUserById(userId);
//        if (user == null) {
//            throw new RuntimeException("该用户不存在!");
//        }
//
//        page.setRows(commentService.findDiscussPostRows(userId));// 我一共发了多少条帖子？
////        page.setPath("/profile/{userId}/myposts");
//        page.setPath("/user/profile/" + userId + "/myposts");
//
//        List<DiscussPost> list = discussPostService.findUserDiscussPosts(userId, page.getOffset(), page.getLimit());
//
//        List<Map<String, Object>> userDiscussPosts = new ArrayList<>();
//        if (list != null) {
//
//            for(DiscussPost post : list){
//                Map<String, Object> map = new HashMap<>();
//                map.put("post", post);
////                User user = userService.findUserById(post.getUserId());
////                map.put("user", user);
////                userDiscussPosts.add(map);
//
//                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_USER, post.getUserId());
//                map.put("likeCount", likeCount);
//
//                userDiscussPosts.add(map);
//            }
//        }
//        model.addAttribute("user", user); // 发帖人的信息
//        model.addAttribute("userDiscussPosts", userDiscussPosts);
//        return "/site/my-post";
//    }

}
