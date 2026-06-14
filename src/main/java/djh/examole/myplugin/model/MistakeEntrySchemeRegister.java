package djh.examole.myplugin.model;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.event.PluginStartedEvent;

/**
 * 注册错题本自定义资源 Scheme 和索引。
 */
@Component
public class MistakeEntrySchemeRegister {

    private final SchemeManager schemeManager;

    public MistakeEntrySchemeRegister(SchemeManager schemeManager) {
        this.schemeManager = schemeManager;
    }

    @EventListener(PluginStartedEvent.class)
    void onPluginStarted() {
        schemeManager.register(MistakeEntry.class,
            (IndexSpecs<MistakeEntry> indexSpecs) -> {
                indexSpecs.add(IndexSpecs.single("metadata.name", String.class));
            }
        );
        System.out.println("✅ MistakeEntry scheme + indices registered");
    }
}
