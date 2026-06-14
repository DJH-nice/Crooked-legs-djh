package djh.examole.myplugin.model;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.event.PluginStartedEvent;

/**
 * 注册错题本自定义资源 Scheme 和索引，使 Halo 能识别、存储和索引 MistakeEntry 类型。
 */
@Component
public class MistakeEntrySchemeRegister {

    private final SchemeManager schemeManager;

    public MistakeEntrySchemeRegister(SchemeManager schemeManager) {
        this.schemeManager = schemeManager;
    }

    @EventListener(PluginStartedEvent.class)
    void onPluginStarted() {
        // 必须使用带 IndexSpecs 的 register，否则 DefaultIndicesManager.get() 返回 null
        schemeManager.register(MistakeEntry.class,
            (IndexSpecs<MistakeEntry> specs) -> {
                // 至少要有一个 index spec，否则 indicesMap 不初始化
                specs.add("metadata.name");
            }
        );
        System.out.println("✅ MistakeEntry scheme + indices registered");
    }
}
