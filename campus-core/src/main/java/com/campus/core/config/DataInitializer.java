package com.campus.core.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.core.model.entity.User;
import com.campus.core.repository.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 初始化默认用户数据（密码 123456 的 BCrypt 哈希）
 * 仅在用户表为空时插入
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initUsers() {
        // 先获取已有用户数
        long count = userMapper.selectCount(new LambdaQueryWrapper<>());
        String defaultPassword = passwordEncoder.encode("123456");
        log.info("默认密码 BCrypt: {}, 当前用户数: {}", defaultPassword, count);

        User[] users = {
            createUser("2307020101", defaultPassword, "张三", 1, 0),
            createUser("2307020102", defaultPassword, "李四", 1, 1),
            createUser("2307020103", defaultPassword, "王五", 1, 1),
            createUser("2005001", defaultPassword, "赵老师", 2, 0),
            createUser("2005002", defaultPassword, "孙老师", 2, 1),
        };

        int inserted = 0, updated = 0;
        for (User u : users) {
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getStudentId, u.getStudentId()));
            if (existing == null) {
                userMapper.insert(u);
                inserted++;
            } else {
                // 更新密码（确保密码一致）和昵称
                existing.setPassword(defaultPassword);
                existing.setNickname(u.getNickname());
                existing.setRole(u.getRole());
                userMapper.updateById(existing);
                updated++;
            }
        }
        log.info("用户初始化完成: 新增 {} 个, 更新 {} 个", inserted, updated);
    }

    private User createUser(String studentId, String password, String nickname, Integer role, Integer firstLogin) {
        User user = new User();
        user.setStudentId(studentId);
        user.setPassword(password);
        user.setFirstLogin(firstLogin);
        user.setNickname(nickname);
        user.setRole(role);
        return user;
    }
}
