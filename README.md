## mac tdlib jni使用

用于开发java电报客户端的脚手架

## 目录说明

- docs/ 用于存放javaDocs，方便查阅TdApi
- lib/ 存放动态调用库文件。运行前需要在重新编译libtdjni.dylib文件 在td/build.html查看教程

## 运行说明
**0**. api.http 是一个完整的请求例子。

**1**. 添加lib路径 在VM options中增加：`-Djava.library.path=lib`

**2**. 运行Application.java，然后通过http请求指定登录号码、验证码、指令。

PS：为了China民众能够使用该应用，我添加了如下代码：

```shell
client.send(new TdApi.AddProxy("127.0.0.1",1080,true,new TdApi.ProxyTypeSocks5()),null);
```

