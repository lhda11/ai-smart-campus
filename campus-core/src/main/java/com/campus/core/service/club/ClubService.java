package com.campus.core.service.club;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.core.model.entity.Application;
import com.campus.core.model.entity.Club;
import com.campus.core.model.entity.Recruitment;
import com.campus.core.repository.mapper.ApplicationMapper;
import com.campus.core.repository.mapper.ClubMapper;
import com.campus.core.repository.mapper.RecruitmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubMapper clubMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final ApplicationMapper applicationMapper;
    private final ChatClient.Builder chatClientBuilder;

    @Transactional
    public void saveClub(Club club) {
        clubMapper.insert(club);
    }

    public List<Club> listClubs() {
        return clubMapper.selectList(null);
    }

    public Club getClubById(Long id) {
        return clubMapper.selectById(id);
    }

    /**
     * 基于标签 Jaccard 相似度推荐社团
     */
    public List<Club> recommend(List<String> userTags) {
        List<Club> allClubs = clubMapper.selectList(null);
        Set<String> userTagSet = new HashSet<>(userTags);

        return allClubs.stream()
                .filter(c -> c.getTags() != null && !c.getTags().isBlank())
                .sorted((a, b) -> {
                    double scoreA = jaccardSim(userTagSet, parseTags(a.getTags()));
                    double scoreB = jaccardSim(userTagSet, parseTags(b.getTags()));
                    return Double.compare(scoreB, scoreA);
                })
                .filter(c -> jaccardSim(userTagSet, parseTags(c.getTags())) > 0)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 先用标签Jaccard粗排，再用LLM精排Top-3
     */
    public List<Club> recommendWithLLM(List<String> userTags) {
        List<Club> candidates = recommend(userTags);
        if (candidates.size() <= 3) return candidates;

        StringBuilder clubList = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Club c = candidates.get(i);
            clubList.append(String.format("%d. %s - %s [标签: %s]\n",
                i + 1, c.getName(), c.getDescription(), c.getTags()));
        }

        String prompt = String.format(
            "用户兴趣标签: %s\n\n候选社团:\n%s\n请从以上社团中选出最适合该用户的Top-3社团，只返回编号（如 3,1,5）",
            String.join(", ", userTags), clubList.toString());

        try {
            String response = chatClientBuilder.build().prompt().user(prompt).call().content();
            String[] indices = response.trim().replaceAll("[^0-9,]", "").split(",");
            List<Club> top3 = new java.util.ArrayList<>();
            for (String idx : indices) {
                int i = Integer.parseInt(idx.trim()) - 1;
                if (i >= 0 && i < candidates.size()) {
                    top3.add(candidates.get(i));
                }
            }
            return top3.isEmpty() ? candidates.subList(0, 3) : top3;
        } catch (Exception e) {
            log.warn("LLM re-rank failed, fallback to Jaccard top-3", e);
            return candidates.subList(0, Math.min(3, candidates.size()));
        }
    }

    private double jaccardSim(Set<String> userTags, Set<String> clubTags) {
        Set<String> intersection = new HashSet<>(userTags);
        intersection.retainAll(clubTags);
        Set<String> union = new HashSet<>(userTags);
        union.addAll(clubTags);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private Set<String> parseTags(String tagsJson) {
        JSONArray arr = JSONUtil.parseArray(tagsJson);
        return arr.stream().map(Object::toString).collect(Collectors.toSet());
    }

    @Transactional
    public Recruitment createRecruitment(Long clubId, String title, String requirement, int quota) {
        Recruitment rec = new Recruitment();
        rec.setClubId(clubId);
        rec.setTitle(title);
        rec.setRequirement(requirement);
        rec.setQuota(quota);
        rec.setEnrolledCount(0);
        rec.setStartTime(java.time.LocalDateTime.now());
        rec.setEndTime(java.time.LocalDateTime.now().plusDays(30));
        rec.setStatus(1);
        recruitmentMapper.insert(rec);
        return rec;
    }

    @Transactional
    public Application apply(Long recruitmentId, Long userId,
                              String name, String studentId, String phone) {
        int count = applicationMapper.countByUserAndRecruitment(userId, recruitmentId);
        if (count > 0) {
            throw new BizException(ErrorCode.DUPLICATE_APPLY);
        }

        Recruitment rec = recruitmentMapper.selectById(recruitmentId);
        if (rec == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "纳新活动不存在");
        }
        if (rec.getEndTime() != null && rec.getEndTime().isBefore(java.time.LocalDateTime.now())) {
            rec.setStatus(3);
            recruitmentMapper.updateById(rec);
            throw new BizException(ErrorCode.BAD_REQUEST, "纳新活动已结束");
        }
        if (rec.getStatus() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "纳新活动已结束");
        }
        if (rec.getEnrolledCount() >= rec.getQuota()) {
            throw new BizException(ErrorCode.CONFLICT, "名额已满");
        }

        Application app = new Application();
        app.setRecruitmentId(recruitmentId);
        app.setUserId(userId);
        app.setName(name);
        app.setStudentId(studentId);
        app.setPhone(phone);
        app.setStatus(0);
        applicationMapper.insert(app);

        // Atomic conditional update to prevent oversubscription
        rec.setEnrolledCount(rec.getEnrolledCount() + 1);
        if (rec.getEnrolledCount() >= rec.getQuota()) {
            rec.setStatus(2);
        }
        recruitmentMapper.updateById(rec);

        return app;
    }

    public List<Application> getMyApplications(Long userId) {
        return applicationMapper.findByUserId(userId);
    }

    public List<Recruitment> getRecruitmentsByClub(Long clubId) {
        return recruitmentMapper.findActiveByClubId(clubId);
    }
}
