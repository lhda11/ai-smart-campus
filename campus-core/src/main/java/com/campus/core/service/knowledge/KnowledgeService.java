package com.campus.core.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.core.model.entity.Knowledge;
import com.campus.core.repository.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;

    /**
     * 根据分类获取启用状态的所有知识条目，返回 name→content 的 Map
     */
    public Map<String, String> getKnowledgeMapByCategory(String category) {
        List<Knowledge> list = knowledgeMapper.findByCategory(category);
        return list.stream()
                .collect(Collectors.toMap(
                        Knowledge::getName,
                        Knowledge::getContent,
                        (a, b) -> b  // 同名时取后者
                ));
    }

    /**
     * 根据分类和名称模糊匹配查询知识条目
     * @param category 分类
     * @param name 输入的名称（支持模糊匹配）
     * @return 匹配到的内容，未找到返回 null
     */
    public String findByName(String category, String name) {
        if (name == null || name.isBlank()) return null;

        // 1. 精确匹配
        LambdaQueryWrapper<Knowledge> exactQw = new LambdaQueryWrapper<>();
        exactQw.eq(Knowledge::getCategory, category)
               .eq(Knowledge::getName, name)
               .eq(Knowledge::getStatus, 1);
        Knowledge exact = knowledgeMapper.selectOne(exactQw);
        if (exact != null) {
            return exact.getContent();
        }

        List<Knowledge> all = knowledgeMapper.findByCategory(category);

        // 2. 包含匹配
        for (Knowledge k : all) {
            if (k.getName().contains(name) || name.contains(k.getName())) {
                return k.getContent();
            }
        }

        // 3. 逐字匹配
        for (Knowledge k : all) {
            String kName = k.getName();
            int matchLen = 0;
            for (int i = 0; i < kName.length(); i++) {
                if (name.contains(String.valueOf(kName.charAt(i)))) {
                    matchLen++;
                }
            }
            if (matchLen >= kName.length() * 0.7 && matchLen >= 2) {
                return k.getContent();
            }
        }

        return null;
    }

    /**
     * 获取某一分类下所有条目的名称
     */
    public List<String> getNamesByCategory(String category) {
        return knowledgeMapper.findByCategory(category)
                .stream()
                .map(Knowledge::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取某一分类下所有完整内容，拼接成结构化文本供AI参考
     */
    public String getAllContentText(String category) {
        List<Knowledge> list = knowledgeMapper.findByCategory(category);
        return list.stream()
                .map(k -> "【" + k.getName() + "】" + k.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 获取所有知识库内容文本，分类组织
     */
    public String getAllKnowledgeText() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 校园设施 =====\n");
        sb.append(getAllContentText("facility")).append("\n\n");
        sb.append("===== 办事流程 =====\n");
        sb.append(getAllContentText("procedure")).append("\n\n");
        sb.append("===== 院系设置 =====\n");
        sb.append(getAllContentText("department")).append("\n");
        return sb.toString();
    }
}
