package com.campus.core.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.exception.BizException;
import com.campus.core.auth.JwtUtil;
import com.campus.core.auth.UserContext;
import com.campus.core.model.dto.ChangePasswordRequest;
import com.campus.core.model.dto.LoginRequest;
import com.campus.core.model.entity.User;
import com.campus.core.model.vo.LoginVO;
import com.campus.core.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 学号工号登录
     */
    public LoginVO login(LoginRequest req) {
        // 1. 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStudentId, req.getStudentId())
        );

        if (user == null) {
            throw new BizException("学号/工号或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("学号/工号或密码错误");
        }

        // 3. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getStudentId(), user.getRole());

        // 4. 返回登录信息
        boolean firstLogin = user.getFirstLogin() != null && user.getFirstLogin() == 1;
        LoginVO vo = new LoginVO(
                token,
                user.getId(),
                user.getStudentId(),
                user.getNickname(),
                user.getRole(),
                firstLogin
        );

        log.info("用户 {} 登录成功, 首次登录: {}", user.getStudentId(), firstLogin);
        return vo;
    }

    /**
     * 首次登录修改密码
     */
    @Transactional
    public LoginVO changePassword(ChangePasswordRequest req) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BizException("未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }

        // 新密码不能和旧密码相同
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }

        // 更新密码和首次登录标志
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setFirstLogin(0);
        userMapper.updateById(user);

        // 重新生成 Token
        String newToken = jwtUtil.generateToken(user.getId(), user.getStudentId(), user.getRole());
        log.info("用户 {} 修改密码成功", user.getStudentId());

        return new LoginVO(
                newToken,
                user.getId(),
                user.getStudentId(),
                user.getNickname(),
                user.getRole(),
                false
        );
    }

    /**
     * 获取当前用户信息
     */
    public User getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BizException("未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }
}
