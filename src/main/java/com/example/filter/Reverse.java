package com.example.filter;

import java.util.*;

import org.apache.log4j.Logger;

import com.ctrip.ops.sysdev.baseplugin.BaseFilter;
import com.ctrip.ops.sysdev.fieldSetter.FieldSetter;
import com.ctrip.ops.sysdev.render.TemplateRender;
import scala.Tuple2;

public class Reverse extends BaseFilter {
    private static final Logger logger = Logger.getLogger(Reverse.class.getName());

    private List<Tuple2> fields;

    public Reverse(Map config) {
        // 构造函数 , 一般不需要额外的代码. 其它准备工作放在prepare方法中完成.
        // config是从hangout配置文件中取得的.
        // hangout使用yaml格式的配置文件. 在这里config可以是 {"fields":["value1", "value2"]}
        super(config);
    }

    @Override
    protected void prepare() {
        // 对于这个Filter来说, 只需要配置有哪些字段需要做翻转. 我们定义一个私有成员fields, 保存这些需要翻转的字段.
        this.fields = new ArrayList<>();
        for (String field : (List<String>) config.get("fields")) {
            TemplateRender templateRender = null;
            try {
                templateRender = TemplateRender.getRender(field, false);
            } catch (Exception e) {
                logger.info("could not build template render from " + field);
            }
            this.fields.add(new Tuple2(FieldSetter.getFieldSetter(field), templateRender));
        }
    }

    /*
    主要的实现函数.
    参数event是上游传递来的事件.
    !!! 返回值需要返回一个Map对象回去. 可以返回原对象, 也可以创建一个新的对象. 在这个例子中, 我们直接修改原对象返回就可以了.
     */
    @Override
    protected Map filter(final Map event) {
        boolean success = false;
        for (Tuple2 t2 : this.fields) {
            FieldSetter fieldSetter = (FieldSetter) t2._1();
            TemplateRender templateRender = (TemplateRender) t2._2();


            Object oldValue = templateRender.render(event);
            if (oldValue == null) {
                continue;
            }

            String newValue = new StringBuilder(oldValue.toString()).reverse().toString();
            fieldSetter.setField(event, newValue);

            success = true;
        }

        postProcess(event, success);
        return event;
    }
}
