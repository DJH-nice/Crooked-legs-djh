package djh.examole.myplugin.router;

import djh.examole.myplugin.model.MistakeEntry;
import djh.examole.myplugin.service.MistakeEntryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * 错题本 - REST API 路由
 */
@Slf4j
@Configuration
public class MistakeEntryRouter {

    private final MistakeEntryService mistakeEntryService;

    public MistakeEntryRouter(MistakeEntryService mistakeEntryService) {
        this.mistakeEntryService = mistakeEntryService;
    }

    @Bean
    RouterFunction<ServerResponse> mistakeEntryRoutes() {
        return RouterFunctions.route()
                // 注意：/stats 路由必须在 /{name} 之前，否则 "stats" 会被当成 name 参数
                .GET("/api/plugins/my-plugin/mistakes/stats/daily",
                        accept(MediaType.APPLICATION_JSON), this::getDailyStats)
                .GET("/api/plugins/my-plugin/mistakes",
                        accept(MediaType.APPLICATION_JSON), this::listMistakeEntries)
                .GET("/api/plugins/my-plugin/mistakes/{name}",
                        accept(MediaType.APPLICATION_JSON), this::getMistakeEntry)
                .POST("/api/plugins/my-plugin/mistakes",
                        accept(MediaType.APPLICATION_JSON), this::createMistakeEntry)
                .PUT("/api/plugins/my-plugin/mistakes/{name}",
                        accept(MediaType.APPLICATION_JSON), this::updateMistakeEntry)
                .DELETE("/api/plugins/my-plugin/mistakes/{name}", this::deleteMistakeEntry)
                .PATCH("/api/plugins/my-plugin/mistakes/{name}/status",
                        accept(MediaType.APPLICATION_JSON), this::updateStatus)
                .build();
    }

    /**
     * GET /api/plugins/my-plugin/mistakes/stats/daily
     * 每日学习统计（近 365 天）
     */
    Mono<ServerResponse> getDailyStats(ServerRequest request) {
        return mistakeEntryService.getDailyStats()
                .flatMap(stats -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(stats));
    }

    /**
     * GET /api/plugins/my-plugin/mistakes
     * 分页查询错题列表
     */
    Mono<ServerResponse> listMistakeEntries(ServerRequest request) {
        int page = request.queryParam("page").map(Integer::parseInt).orElse(1);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(20);
        String subject = request.queryParam("subject").orElse(null);
        String status = request.queryParam("status").orElse(null);

        return mistakeEntryService.listMistakeEntries(page, size, subject, status)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result));
    }

    /**
     * GET /api/plugins/my-plugin/mistakes/{name}
     * 获取单条错题详情
     */
    Mono<ServerResponse> getMistakeEntry(ServerRequest request) {
        String name = request.pathVariable("name");
        return mistakeEntryService.getMistakeEntry(name)
                .flatMap(entry -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(entry))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * POST /api/plugins/my-plugin/mistakes
     * 创建新错题
     */
    Mono<ServerResponse> createMistakeEntry(ServerRequest request) {
        return request.bodyToMono(MistakeEntry.class)
                .flatMap(mistakeEntryService::createMistakeEntry)
                .flatMap(entry -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(entry));
    }

    /**
     * PUT /api/plugins/my-plugin/mistakes/{name}
     * 更新错题
     */
    Mono<ServerResponse> updateMistakeEntry(ServerRequest request) {
        String name = request.pathVariable("name");
        return request.bodyToMono(MistakeEntry.class)
                .flatMap(updated -> mistakeEntryService.updateMistakeEntry(name, updated))
                .flatMap(entry -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(entry))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * DELETE /api/plugins/my-plugin/mistakes/{name}
     * 删除错题
     */
    Mono<ServerResponse> deleteMistakeEntry(ServerRequest request) {
        String name = request.pathVariable("name");
        return mistakeEntryService.deleteMistakeEntry(name)
                .then(ServerResponse.noContent().build());
    }

    /**
     * PATCH /api/plugins/my-plugin/mistakes/{name}/status
     * 更新掌握状态
     */
    Mono<ServerResponse> updateStatus(ServerRequest request) {
        String name = request.pathVariable("name");
        return request.bodyToMono(MistakeStatusRequest.class)
                .flatMap(body -> mistakeEntryService.updateStatus(name, body.status()))
                .flatMap(entry -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(entry))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", e.getMessage())))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * 状态更新请求体
     */
    record MistakeStatusRequest(String status) {
    }
}
