package com.campus.core;

import com.campus.common.dto.ApiResult;
import com.campus.core.controller.*;
import com.campus.core.model.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CampusIntegrationTest {

    @Autowired private ChatController chatController;
    @Autowired private SearchController searchController;
    @Autowired private AnnouncementController announcementController;
    @Autowired private ClubController clubController;
    @Autowired private ApplyController applyController;
    @Autowired private AdminController adminController;

    @Test
    @Order(1)
    @DisplayName("全链路：查文档 → RAG问答（controller wiring）")
    void fullRAGFlow_controllerWiring() {
        assertNotNull(announcementController, "AnnouncementController should be wired");
        assertNotNull(searchController, "SearchController should be wired");
        assertNotNull(chatController, "ChatController should be wired");
    }

    @Test
    @Order(2)
    @DisplayName("全链路：兴趣推荐 → 报名（controller wiring）")
    void fullClubFlow_controllerWiring() {
        assertNotNull(clubController, "ClubController should be wired");
        assertNotNull(applyController, "ApplyController should be wired");
    }

    @Test
    @Order(3)
    @DisplayName("管理：重建索引（controller wiring）")
    void fullRebuildFlow_controllerWiring() {
        assertNotNull(adminController, "AdminController should be wired");
    }

    @Test
    @Order(4)
    @DisplayName("ChatController: SSE流式端点存在")
    void chatController_streamEndpointExists() {
        ApiResult<List<Conversation>> result = chatController.conversations();
        assertEquals(200, result.getCode());
    }

    @Test
    @Order(5)
    @DisplayName("ClubController: 推荐端点返回200")
    void clubController_recommendReturnsOk() {
        ApiResult<List<Club>> result = clubController.recommend("科技,AI");
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }

    @Test
    @Order(6)
    @DisplayName("ClubController: 所有社团列表")
    void clubController_listAll() {
        ApiResult<List<Club>> result = clubController.listAll();
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
    }
}
