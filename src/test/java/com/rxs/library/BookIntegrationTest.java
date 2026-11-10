package com.rxs.library;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 书目检索与分页集成测试。 */
class BookIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("公开书目分页、筛选与详情")
    void publicBookPaginationFilteringAndDetails() throws Exception {
        mvc.perform(postJson("/IBookBiz/queryBook/page", "{\"pageSize\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(10))
                .andExpect(jsonPath("body.data[0].id").value(1))
                .andExpect(jsonPath("body.data.length()").value(2));
        mvc.perform(postJson("/IBookBiz/queryBook/page", "{\"dto\":{\"title\":\"java\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(3));
        mvc.perform(postJson("/IBookBiz/queryBook", "{\"id\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.availableCopies").value(0));
        mvc.perform(postJson("/IBookBiz/queryBook", "{\"id\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("页码从一开始且搜索字符按字面匹配")
    void oneBasedPaginationAndLiteralSearch() throws Exception {
        mvc.perform(postJson("/IBookBiz/queryBook/page", "{\"pageCurrent\":2,\"pageSize\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("code").value("0"))
                .andExpect(jsonPath("body.total").value(10))
                .andExpect(jsonPath("body.data[0].id").value(3));
        mvc.perform(postJson("/IBookBiz/queryBook/page", "{\"pageCurrent\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(10))
                .andExpect(jsonPath("body.data.length()").value(0));
        for (String title : List.of("%", "_", "!", "' OR 1=1 --")) {
            mvc.perform(
                            postJson(
                                    "/IBookBiz/queryBook/page",
                                    json.writeValueAsString(Map.of("dto", Map.of("title", title)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("body.total").value(0));
        }
        mvc.perform(
                        postJson(
                                "/IBookBiz/queryBook/page",
                                json.writeValueAsString(
                                        Map.of("dto", Map.of("title", "x".repeat(256))))))
                .andExpect(status().isBadRequest());
    }
}
