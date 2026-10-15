# Devonian Modern UI Text Fix

独立的客户端兼容补丁，修复 Devonian 绕过 Modern UI 文字处理而使 `&k` 混淆文字显示为固定方框的问题。保留 Devonian 的文字小数坐标调整，并把其调用交回原有的文字处理链。

## 适用版本

- Minecraft **26.1.2 / Fabric**
- Devonian **1.32.9 / 1.33.9 / 1.34.9**（当前 26.1.x 最新正式版）
- Modern UI **3.13.0.5**
- Fabric Loader **0.19.2 或以上**、Java **25 或以上**

依赖声明仅允许上述已验证版本。其他版本需要先检查其兼容性。

## 安装

1. 退出 Minecraft。
2. 移除旧版补丁，将 `devonian-modernui-fix-1.0.1+26.1.2.jar` 放进当前游戏实例的 `mods` 文件夹。
3. 保留原有 Devonian 与 Modern UI，保持 Modern Text Engine 开启，字体设置沿用现在的配置。
4. 重新启动游戏，查看原先出现固定方框的物品悬停说明。

这是附加补丁，无须替换原 Mod。只安装上面的 JAR，不要把源码包、完整 ZIP 或 verification 目录里的文件放进 mods。

卸载时退出游戏并移除本补丁 JAR 即可。补丁不读写存档、字体配置或原 Mod 文件，也不增加网络请求。

## 验证范围

已使用 Minecraft 26.1.2、Fabric Loader 及两个已发布 Mod 的原始 Mixin 字节码完成隔离测试：

- 未加补丁时复现 Devonian 绕过 Modern UI 的调用。
- 加补丁后确认进入 Modern UI 的处理入口，并传递完整的文本与混淆/粗体样式。
- 检查默认坐标、小数坐标、仅覆盖 X / Y、颜色、阴影、空字形标记、边界与缓存。
- Modern UI 文字引擎对应 Mixin 未启用时，补丁仍允许原版调用链执行。

测试在 Modern UI 布局/图形入口使用记录参数的替身，不启动游戏窗口、图形驱动或服务器连接。因此验证了调用链修复，尚未完成用户整套 Mod 环境中的悬停说明画面实测。

## 技术说明

Devonian 1.32.9、1.33.9、1.34.9 的 `GuiTextRenderStateMixin.devonian$textFloatPosition` 收到 `Operation`，但直接调用 `Font.prepareText`，丢弃了 Modern UI 的重定向链。此补丁只替换该直接调用，改为 `original.call(...)`，保留它计算出的坐标和其余参数。

补丁在 Mixin 合并后确认目标方法的来源、名称、完整签名与唯一直接调用，再执行变换。若目标结构不匹配，会明确报错，避免修改未知方法。它不包含 Devonian 或 Modern UI 的类，也不替换全局字体渲染器。

源码参考：
- https://github.com/Synnerz/devonian/blob/26.1/src/main/java/com/github/synnerz/devonian/mixin/GuiTextRenderStateMixin.java
- https://github.com/BloCamLimb/ModernUI-MC/blob/master/common/src/main/java/icyllis/modernui/mc/text/mixin/MixinGuiTextRenderState.java
- https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation

## 从源码编译

安装 JDK 25，设置 `JAVA_HOME` 后，在本目录运行：

```powershell
.\gradlew.bat build
```

产物位于 `build/libs`。普通构建不需要原 Mod JAR；verification 为额外的独立验证项目，不参与发行包构建。

如需复现测试，先构建补丁，然后用 Python 3 运行：

```powershell
python verification/prepare_fixtures.py "路径/ModernUI.jar" "路径/devonian.jar"
.\gradlew.bat -p verification runVerify -x downloadAssets
.\gradlew.bat -p verification runVerify -Pbaseline -x downloadAssets
.\gradlew.bat -p verification runVerify -PtextEngineDisabled -x downloadAssets
```

也可一次验证三个已发布版本及较新 Loader：

```powershell
python verification/run_matrix.py "路径/ModernUI.jar" "路径/devonian-1.32.9.jar" "路径/devonian-1.33.9.jar" "路径/devonian-1.34.9.jar"
python -m unittest discover -s verification -p "test_*.py"
```

1.0.1 的官方版本来源、验证范围和结果见 [TESTING.md](TESTING.md)。

测试只提取原始类并替换测试用元数据，不运行原 Mod 入口，不修改输入 JAR。原 Mod 与测试依赖不随本补丁分发。
