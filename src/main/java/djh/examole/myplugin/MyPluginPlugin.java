package djh.examole.myplugin;

import djh.examole.myplugin.model.MistakeEntry;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * <p>错题本插件 - 主类，管理插件生命周期。</p>
 * <p>提供错题的记录、分类、复习追踪等功能。</p>
 *
 * @author dongjiahao
 * @since 1.0.0
 */
@Component
public class MyPluginPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public MyPluginPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        // 注册错题本自定义资源 + 索引（必须指定索引，否则 insert 时报错）
        schemeManager.register(MistakeEntry.class,
            (run.halo.app.extension.index.IndexSpecs<MistakeEntry> specs) -> {
                specs.add(run.halo.app.extension.index.IndexSpecs.single("metadata.name", String.class));
            }
        );
        System.out.println("✅ MistakeEntry scheme + indices registered");
        System.out.println("✅ 错题本插件启动成功！API: /api/plugins/my-plugin/mistakes");
    }

    @Override
    public void stop() {
        System.out.println("错题本插件停止！");
    }
}
