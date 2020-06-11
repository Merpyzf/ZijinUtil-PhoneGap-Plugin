# ZijinUtil-PhoneGap-Plugin
该分支的插件为CZ8800的设备开发。

在引入插件后需要进行一下配置：

向项目根目录下的 config.xml 文件中添加如下配置：

```
<edit-config file="app/src/main/AndroidManifest.xml" mode="merge" target="/manifest/application" xmlns:android="http://schemas.android.com/apk/res/android">
      <application android:name="nl.xservices.plugins.MyApp" android:networkSecurityConfig="@xml/network_security_config" />
</edit-config>
<resource-file src="resources/android/xml/network_security_config.xml" target="app/src/main/res/xml/network_security_config.xml" />
```
