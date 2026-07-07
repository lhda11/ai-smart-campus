package com.campus.core.ai;

import com.campus.core.service.knowledge.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CampusFunctions {

    private final KnowledgeService knowledgeService;

    public record FacilityRequest(String name) {}
    public record ProcedureRequest(String name) {}
    public record DepartmentRequest(String name) {}

    @Bean("queryFacilityLocation")
    @Description("查询校园设施信息（食堂、图书馆、体育馆、快递站等的位置和联系方式）")
    public Function<FacilityRequest, String> queryFacilityLocation() {
        return request -> {
            log.info("Tool called: queryFacilityLocation({})", request.name());
            return knowledgeService.getAllContentText("facility");
        };
    }

    @Bean("queryProcedure")
    @Description("查询办事流程指南（校园卡补办、成绩单打印、宿舍报修、校园网办理等）")
    public Function<ProcedureRequest, String> queryProcedure() {
        return request -> {
            log.info("Tool called: queryProcedure({})", request.name());
            return knowledgeService.getAllContentText("procedure");
        };
    }

    @Bean("queryDepartment")
    @Description("查询院系设置和专业介绍")
    public Function<DepartmentRequest, String> queryDepartment() {
        return request -> {
            log.info("Tool called: queryDepartment({})", request.name());
            return knowledgeService.getAllContentText("department");
        };
    }
}
