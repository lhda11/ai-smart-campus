package com.campus.core.auth;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    public static void set(UserInfo user) {
        CONTEXT.set(user);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static Long getCurrentUserId() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.getUserId() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 用户上下文信息
     */
    public static class UserInfo {
        private Long userId;
        private String studentId;
        private Integer role;

        public UserInfo() {}

        public UserInfo(Long userId, String studentId, Integer role) {
            this.userId = userId;
            this.studentId = studentId;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }
    }
}
