## 一个简单的Filter, 可能并没有任何实用价值

惟一的作用是对数据中某些字段做翻转, 比如把Hello翻转成olleH

## 准备工作

1. 下载 hangout-base-plugin.jar
    编译打包后的文件在 [https://github.com/childe/hangout/releases/download/0.2.0/hangout-dist-0.2.0-release-bin.zip](https://github.com/childe/hangout/releases/download/0.2.0/hangout-dist-0.2.0-release-bin.zip) . 开发filter plugin,只需要其中一个jar包, 但开发后的东西还是要用在hangout里面,就先把整个包下载下来吧.

2. 部署到本地repository. 参考[Guide to installing 3rd party JARs](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)

    ```
    unzip hangout-dist-0.2.0-release-bin.zip
    mvn install:install-file -Dfile=hangout-baseplugin-0.2.0.jar -DgroupId=ctrip -DartifactId=hangout-baseplugin -Dversion=0.2.0 -Dpackaging=jar
    ```

## Step 1 新建pom.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <groupId>ctrip</groupId>
    <version>0.1</version>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>hangout-filters-reverse</artifactId>
    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>ctrip</groupId>
            <artifactId>hangout-baseplugin</artifactId>
            <version>0.2.0</version>
        </dependency>
    </dependencies>
</project>
```

## Step 2 实现Filter

1. `mkdir -p src/main/java/com/example/filter`

2. 创建源文件 src/main/java/com/example/filter/Reverse.java, 具体代码参见[https://github.com/childe/hangout-filter-reverse/blob/4a01443b5fc4c18260c370dd6d7ba68447409cc2/src/main/java/com/example/filter/Reverse.java](https://github.com/childe/hangout-filter-reverse/blob/4a01443b5fc4c18260c370dd6d7ba68447409cc2/src/main/java/com/example/filter/Reverse.java)

3. 代码说明

    - 构造函数一般只需要一句 `super(config);` , 其它准备工作放在prepare方法中完成. super(config)里面完成了些Step3中讲的事情
    - prepare方法. 处理每个Filter自己的额外的一些准备工作. 在这个简单的Filter中, 就是把配置文件中的fields提取出来放在自己的成员变量中
    - 主要实现函数 `Map filter(final Map event)` .这个函数接收一个参数(Map event), 是上游传递来的事件. 返回值需要返回一个Map对象回去. 可以返回原对象, 也可以创建一个新的对象. 在这个例子中, 我们直接修改原对象返回就可以了.

## Step 3 打包使用

1. 打包 `mvn package`

2. 将打包好的jar包(hangout-filters-reverse-0.1.jar)放到hangout的modules文件夹下

3. 配置文件中使用刚写好的Reverse Filter

    ```
    inputs:
        - Stdin:
            codec: plain

    filters:
        - Add:
            fields:
                value: liujia
        - com.example.filter.Reverse:
            fields:
                - message
                - value

    outputs:
        - Stdout: {}

    ```

4. 跑起来看看效果吧

## Step 4 也许你需要一些善后处理

刚刚在我们的示例代码中,如果配置的字段在消息中并没有,我们直接跳过. 现在我们有个新的需求:如果全部字段都没有出现,那么就需要给消息打个标签.

在filter函数中, 我们需要做一点小的改动, 定义一个变量success来判断消息是不是处理失败(全部字段都不存在). 在return event之前,添加一句 `postProcess(event, success);`

一共添加了三行代码, 改动见 [https://github.com/childe/hangout-filter-reverse/commit/0e98ab47005d90d8a177090c5d4c78c40fab7d3a](https://github.com/childe/hangout-filter-reverse/commit/0e98ab47005d90d8a177090c5d4c78c40fab7d3a)

拿这个配置来测试一下吧.
```
inputs:
    - Stdin:
        codec: plain

filters:
    - Add:
        if:
            - '<#if message=="hello">true</#if>'
        fields:
            value: liujia
    - com.example.filter.Reverse:
        fields:
            - value
        tag_on_failure: reverseFail

outputs:
    - Stdout: {}
```

*tag_on_failure* 这个是固定字段, 如果配置了这个字段,在postProcess里面就会添加reverseFail到tags字段中.

类似的还有add_fields,以及remove_fields.  如果成功的话, 就会添加配置的字段, 以及删除某些字段, 配置如下

```
inputs:
    - Stdin:
        codec: plain

filters:
    - Add:
        if:
            - '<#if message=="hello">true</#if>'
        fields:
            value: liujia
    - com.example.filter.Reverse:
        fields:
            - value
        tag_on_failure: reverseFail
        add_fields:
            a: 1
            b: "world"
        remove_fields: ['message']

outputs:
    - Stdout: {}
```

效果如下:

```
abcd
{@timestamp=2017-03-06T17:38:04.247+08:00, message=abcd, tags=[reverseFail]}
hello
{a=1, b=world, @timestamp=2017-03-06T17:38:06.847+08:00, value=aijuil}
```

## 单元测试

不加单元测试心里会虚.

加上单元测试的的代码见项目 [https://github.com/childe/hangout-filter-reverse](https://github.com/childe/hangout-filter-reverse)
记得pom.xml里面加上需要的依赖

我只是写了一个简单的单元测试, 真正开发新的插件,还是需要完善它.
