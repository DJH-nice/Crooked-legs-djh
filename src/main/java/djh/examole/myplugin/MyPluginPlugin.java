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
        // 注册错题本自定义资源
        schemeManager.register(MistakeEntry.class);
        System.out.println("错题本插件启动成功！");
        System.out.println("API 路径: /api/plugins/my-plugin/mistakes");
    }

    @Override
    public void stop() {
        System.out.println("错题本插件停止！");
    }
}
