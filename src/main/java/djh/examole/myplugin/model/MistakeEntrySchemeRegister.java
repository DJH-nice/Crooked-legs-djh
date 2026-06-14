package djh.examole.myplugin.model;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.event.PluginStartedEvent;

/**
 * 注册错题本自定义资源 Scheme，使 Halo 能识别 MistakeEntry 类型。
 * 在插件启动后通过 SchemeManager 注册。
 */
@Component
public class MistakeEntrySchemeRegister {

    private final SchemeManager schemeManager;

    public MistakeEntrySchemeRegister(SchemeManager schemeManager) {
        this.schemeManager = schemeManager;
    }

    @EventListener(PluginStartedEvent.class)
    void onPluginStarted() {
        schemeManager.register(MistakeEntry.class);
        System.out.println("✅ MistakeEntry scheme registered successfully");
    }
}
