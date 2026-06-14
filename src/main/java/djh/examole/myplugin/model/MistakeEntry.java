package djh.examole.myplugin.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * 错题本 - 自定义资源模型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(group = "my-plugin.examole.djh", version = "v1alpha1", kind = "MistakeEntry", plural = "mistake-entries", singular = "mistake-entry")
public class MistakeEntry extends AbstractExtension {

    private MistakeEntrySpec spec;

    @Data
    public static class MistakeEntrySpec {

        /**
         * 科目：数学、语文、英语、物理、化学等
         */
        private String subject;

        /**
         * 题目原文
         */
        private String question;

        /**
         * 错误的答案
         */
        private String wrongAnswer;

        /**
         * 正确答案
         */
        private String correctAnswer;

        /**
         * 错题分析/解析
         */
        private String analysis;

        /**
         * 掌握状态：unmastered（未掌握），reviewing（复习中），mastered（已掌握）
         */
        private String status;

        /**
         * 难度：easy（简单），medium（中等），hard（困难）
         */
        private String difficulty;

        /**
         * 题目来源：exam（考试），homework（作业），quiz（测验），other（其他）
         */
        private String source;

        /**
         * 自定义标签
         */
        private java.util.List<String> tags;

        /**
         * 知识点/考点
         */
        private String knowledgePoint;

        /**
         * 备注
         */
        private String remark;
    }
}
