如果你已经看完了第一个小例子, 那么想一下, 我要对json里面的第二层数据做改变, 应该怎么办.  
直接调用 `event.get(), event.put()` 已经不太方便了.

hangout在basefilter里面封装了两个方法, 可以拿来用一下. 如果要使用这两个方法, 我们有以下的约定:

```
fields:
    - name
    - '[metric][value1]'
```

第一个name不多解释, 第二种 `[metric][value1]` 我们认为是要处理日志中的metric.value1中的内容. (两个单引号不是我们约定的内容,只是yaml解析需要.)

import两个新东西:

```
import com.ctrip.ops.sysdev.fieldSetter.FieldSetter;
import com.ctrip.ops.sysdev.render.TemplateRender;
```

在prepare函数中, 我们需要事先把FieldSetter和TemplateRender准备好.

```
for (String field : (List<String>) config.get("fields")) {
    TemplateRender templateRender = null;
    try {
        templateRender = TemplateRender.getRender(field, false);
    } catch (Exception e) {
        logger.info("could not build template render from " + field);
    }
    this.fields.add(new Tuple2(FieldSetter.getFieldSetter(field), templateRender));
}
```

在处理日志事件时:

```
for (Tuple2 t2 : this.fields) {
    FieldSetter fieldSetter = (FieldSetter) t2._1();
    TemplateRender templateRender = (TemplateRender) t2._2();


    Object oldValue = templateRender.render(event);
    if (oldValue == null) {
        continue;
    }

    String newValue = new StringBuilder(oldValue.toString()).reverse().toString();
    fieldSetter.setField(event, newValue);
}
```

实现的方法可能不太好, 如果继承Map方法并重写get, put方法, 代码看起来更通畅. 但限于水平目前先这样了.

对templateRender方法要多解释一下:  
TemplateRender.getRender 接收两个参数, `TemplateRender.render(String template, boolean ignoreOneLevelRender)` 第二个参数默认为true.  
在我们这个例子中, render("name") 就是说取日志事件中的name属性.  
但是在一些其它场景中, 比如说, 在Elasticsearch output中, 定义文档的type, document_type: 'logs', 这里的logs就不是指日志事件中的logs属性,而是字面上的'logs'. 如果需要取 logs属性, 需要写 `[logs]` 或者是 `${logs}`  
至于第二个参数默认设置为true, 是为了向前兼容, 因为之前并没有考虑到多层json结构


最后的代码在[https://github.com/childe/hangout-filter-reverse/tree/multilevel](https://github.com/childe/hangout-filter-reverse/tree/multilevel), 测试用例我也添加了一个.
