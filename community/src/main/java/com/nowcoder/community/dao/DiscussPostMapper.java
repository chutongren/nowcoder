package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface DiscussPostMapper {
    // 返回用户所有的帖子
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    // @Param注解用于给参数取别名,
    // 如果该方法只有一个参数,并且在<if>里使用（动态拼条件）,则必须加别名.
    // 查发布的帖子总数
    int selectDiscussPostRows(@Param("userId") int userId);

    // 增：用户发布帖子
    int insertDiscussPost(DiscussPost discussPost);

    // 根据帖子 ID 查询并返回一个 DiscussPost 对象
    DiscussPost selectDiscussPostById(int id);

    // 更新帖子的评论数量
    int updateCommentCount(int id, int commentCount);

    // 置顶、加精、删除功能
    int updateType(int id, int type); // 1 置顶

    int updateStatus(int id, int status); // 1 加精， 2 删除

    int updateScore(int id, double score);

    List<DiscussPost> selectUserDiscussPosts(int userId, int offset, int limit);
}
