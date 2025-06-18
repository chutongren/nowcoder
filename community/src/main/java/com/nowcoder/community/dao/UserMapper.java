package com.nowcoder.community.dao;

import com.nowcoder.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
//UserMapper是一个数据访问对象（DAO，Data Access Object），它的主要作用是 与数据库进行交互，执行与 User 实体相关的数据库操作（增删改查的函数）
    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(int id, int status);

    int updateHeader(int id, String headerUrl);

    int updatePassword(int id, String password);

    int deleteUser(int id);


}
