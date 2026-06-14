package djh.examole.myplugin.service;

import djh.examole.myplugin.model.MistakeEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 错题本 - 业务逻辑服务
 */
@Slf4j
@Service
public class MistakeEntryService {

    private static final Set<String> VALID_STATUSES = Set.of("unmastered", "reviewing", "mastered");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("easy", "medium", "hard");
    private static final Set<String> VALID_SOURCES = Set.of("exam", "homework", "quiz", "other");

    private final ReactiveExtensionClient client;

    public MistakeEntryService(ReactiveExtensionClient client) {
        this.client = client;
    }

    /**
     * 分页查询错题列表，支持按科目和状态筛选
     */
    public Mono<ListResult<MistakeEntry>> listMistakeEntries(int page, int size, String subject, String status) {
        // 收集所有记录后在内存中筛选（Halo Extension 不支持 spec 级别字段选择器）
        return listAll()
                .collectList()
                .map(all -> {
                    List<MistakeEntry> filtered = all.stream()
                            .filter(e -> subject == null || subject.isBlank() ||
                                    subject.equals(e.getSpec().getSubject()))
                            .filter(e -> status == null || status.isBlank() ||
                                    status.equals(e.getSpec().getStatus()))
                            .collect(Collectors.toList());

                    int total = filtered.size();
                    int from = (page - 1) * size;
                    int to = Math.min(from + size, total);
                    List<MistakeEntry> pageItems = from < total
                            ? filtered.subList(from, to)
                            : List.of();

                    return new ListResult<>(page, size, (long) total, pageItems);
                });
    }

    /**
     * 获取所有错题（不过滤）
     */
    public Flux<MistakeEntry> listAll() {
        return client.list(MistakeEntry.class, null, null);
    }

    /**
     * 根据 ID 获取单条错题
     */
    public Mono<MistakeEntry> getMistakeEntry(String name) {
        return client.get(MistakeEntry.class, name);
    }

    /**
     * 创建错题
     */
    public Mono<MistakeEntry> createMistakeEntry(MistakeEntry entry) {
        if (entry.getSpec() == null) {
            entry.setSpec(new MistakeEntry.MistakeEntrySpec());
        }
        if (entry.getSpec().getStatus() == null || entry.getSpec().getStatus().isBlank()) {
            entry.getSpec().setStatus("unmastered");
        }
        if (entry.getMetadata() == null) {
            entry.setMetadata(new run.halo.app.extension.Metadata());
        }
        if (entry.getApiVersion() == null || entry.getApiVersion().isBlank()) {
            entry.setApiVersion("my-plugin.examole.djh/v1alpha1");
        }
        if (entry.getKind() == null || entry.getKind().isBlank()) {
            entry.setKind("MistakeEntry");
        }
        System.out.println("CREATE: spec=" + (entry.getSpec() != null) 
            + " meta=" + (entry.getMetadata() != null)
            + " apiVer=" + entry.getApiVersion() + " kind=" + entry.getKind());
        return client.create(entry)
            .doOnNext(e -> System.out.println("CREATE OK: " + e.getMetadata().getName()))
            .doOnError(e -> System.err.println("CREATE ERR: " + e.getMessage()));
    }

    /**
     * 更新错题
     */
    public Mono<MistakeEntry> updateMistakeEntry(String name, MistakeEntry updated) {
        return client.get(MistakeEntry.class, name)
                .flatMap(existing -> {
                    if (updated.getSpec() != null) {
                        existing.setSpec(updated.getSpec());
                    }
                    return client.update(existing);
                })
                .switchIfEmpty(Mono.error(
                        new NoSuchElementException("错题不存在: " + name)
                ));
    }

    /**
     * 删除错题
     */
    public Mono<Void> deleteMistakeEntry(String name) {
        return client.get(MistakeEntry.class, name)
                .flatMap(client::delete)
                .switchIfEmpty(Mono.error(
                        new NoSuchElementException("错题不存在: " + name)
                ))
                .then();
    }

    /**
     * 更新掌握状态（带校验）
     */
    public Mono<MistakeEntry> updateStatus(String name, String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            return Mono.error(new IllegalArgumentException(
                    "无效的状态值: " + status + "，合法值: " + VALID_STATUSES));
        }
        return client.get(MistakeEntry.class, name)
                .flatMap(entry -> {
                    entry.getSpec().setStatus(status);
                    return client.update(entry);
                })
                .switchIfEmpty(Mono.error(
                        new NoSuchElementException("错题不存在: " + name)
                ));
    }

    /**
     * 获取每日学习统计（近 365 天）
     * 返回按日期聚合的：新增数、复习数、掌握数
     */
    public Mono<List<DailyStats>> getDailyStats() {
        return listAll()
                .collectList()
                .map(entries -> {
                    LocalDate today = LocalDate.now();
                    LocalDate oneYearAgo = today.minusDays(365);

                    // 按日期分桶统计
                    Map<LocalDate, int[]> dateMap = new LinkedHashMap<>();

                    for (MistakeEntry entry : entries) {
                        Instant created = entry.getMetadata().getCreationTimestamp();
                        if (created == null) continue;

                        LocalDate date = created.atZone(ZoneId.systemDefault()).toLocalDate();
                        if (date.isBefore(oneYearAgo) || date.isAfter(today)) continue;

                        int[] counts = dateMap.computeIfAbsent(date, k -> new int[2]);
                        counts[0]++; // 新增

                        String status = entry.getSpec().getStatus();
                        if ("mastered".equals(status)) {
                            counts[1]++; // 已掌握
                        }
                    }

                    // 填充所有日期（包括没有记录的日期为 0）
                    List<DailyStats> result = new ArrayList<>();
                    for (LocalDate d = oneYearAgo; !d.isAfter(today); d = d.plusDays(1)) {
                        int[] counts = dateMap.getOrDefault(d, new int[]{0, 0});
                        result.add(new DailyStats(d.toString(), counts[0], counts[1]));
                    }
                    return result;
                });
    }

    /**
     * 每日统计记录
     */
    public record DailyStats(String date, int added, int mastered) {
    }
}
