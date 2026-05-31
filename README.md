# 🎨 Pigmentum - Minecraft 画布模组

一个 NeoForge 1.21.1 的底层画布 API 模组，允许玩家将任意方块表面变为可绘制的画布，支持像素级绘制、红石交互、活塞推动，并提供完整的扩展 API 供第三方模组自定义渲染和绘制行为。

---

## ✨ 核心功能

### 🖌️ 画布系统
- **任意方块变画布**：使用画笔右击任意方块表面，即可将其转换为画布方块，画布与方块融为一体。
- **像素级绘制**：支持长按鼠标右键连续绘制，采用 Bresenham 3D 插值算法确保快速移动时线条流畅不断线。
- **多面独立画布**：一个方块的每个表面（上、下、东南西北）都可以拥有独立的画布像素数据。
- **不规则方块支持**：楼梯、半砖、栅栏等非完整方块也能精确贴合表面绘制画布，自动适配碰撞箱形状。
- **动态纹理渲染**：画布像素通过 BER（BlockEntityRenderer）以动态纹理方式渲染，每个面仅 1 个四边形。
- **画笔配置菜单**：手持画笔右键空气打开 GUI，可调整画笔大小、羽化强度、混合模式、绘画间隔（步长）。

### 🎯 画笔系统
- **吸色功能**：按 `R` 键（可配置）吸取准星指向像素的颜色，自动更新画笔颜色。
- **多种混合模式**：
    - `OVERWRITE` - 覆盖模式
    - `ADD` - 叠加模式（带 Alpha 混合）
    - `MULTIPLY` - 正片叠底
    - `ERASE` - 橡皮擦
- **图案绘制**：支持通过 `IPaintProvider` 接口实现自定义图案画笔。
- **画笔注册表**：第三方模组可通过 `PaintProviders` 注册自定义画笔物品。

### 📦 画布提取与展示
- **画布物品**：`CanvasSheet` 物品，潜行右击画布方块可将单个面复制到物品中，原画布不清除。
- **展示框渲染**：画布物品放入展示框后，像地图一样显示画布像素内容，附带半透明薄方块背景。
- **纹理生命周期**：展示框纹理随物品销毁自动释放，无内存泄漏。

### 🧩 扩展 API
- **可替换纹理生成**：通过 `CanvasImageProvider` 接口自定义从 `CanvasFace` 到 `NativeImage` 的生成逻辑。
- **可替换渲染器**：通过 `CanvasPixelRenderer` 接口和 `CanvasRendererRegistry` 注册自定义画布面渲染行为。
- **像素级效果系统**：`CanvasFace` 支持每像素附加任意字节数据（效果层），如夜光强度、金属度等，画笔可通过 `PixelProvider` 和 `BlendFunction` 操作效果层。
- **混合函数注册**：`BlendFunction` 接口允许完全自定义像素颜色与效果值的混合逻辑。

---

## 🛠️ 技术架构

| 组件 | 职责 |
|------|------|
| `CanvasBlock` | 代理方块基类，负责形状、光照、破坏、交互等委托 |
| `CanvasBlockEntity` | 存储 `mimickedState`（被模仿方块）和 `CanvasData`（画布数据） |
| `CanvasData` / `CanvasFace` / `PixelMatrix` | 画布数据模型，支持 Codec/StreamCodec 序列化 |
| `CanvasRenderEventHandler` | 在区段几何构建时渲染原方块模型 |
| `CanvasBlockEntityRenderer` | 以动态纹理渲染画布像素面 |
| `CanvasTextureManager` | 管理动态纹理的生成、缓存与释放 |
| `PaintInputHandler` | 客户端每渲染帧检测画笔输入，实现流畅绘制 |
| `IPaintProvider` | 画笔行为接口，可注册自定义画笔 |
| `PaintProviders` | 全局画笔注册表 |
| `CanvasImageProvider` | 纹理生成扩展点 |
| `CanvasPixelRenderer` | 像素渲染扩展点 |
| `BlendFunction` | 像素混合扩展点 |
| `SyncCanvasPacket` | 服务端→客户端画布同步包 |
| `CanvasUploadPacket` | 客户端→服务端画布上传包 |
| `LevelMixin` | 拦截 `setBlock`，转换画布方块状态更新 |
| `PistonStructureResolverMixin` | 活塞移动时保存画布数据 |
| `AbstractContainerMenuMixin` | 修复铁砧等菜单的方块验证 |
| `DoorBlockMixin` | 修复门的双格同步 |

---

## 🎮 使用方法

### 基本绘制
1. 手持画笔，**右键点击**方块表面创建画布。
2. 按住**右键拖动**在画布上绘制像素。
3. 按 **`R` 键**吸取准星指向的颜色。
4. **潜行右键空气**打开画笔配置菜单，调整大小、羽化、混合模式等。

### 提取画布
1. 合成或获取 `Canvas Sheet` 物品。
2. 手持该物品，**潜行+右键**已绘制的画布方块，复制单个面的画布。
3. 将画布物品放入**展示框**，画布内容会自动显示。

---

## 🔧 开发者指南

### 注册自定义画笔
```java
PaintProviders.register(YOUR_ITEM, new IPaintProvider() {
    @Override
    public Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
        return 0xFFFF0000; // 红色画笔
    }
});
```

### 注册自定义渲染器
```java
CanvasRendererRegistry.registerPixelRenderer((context) -> {
    // 自定义渲染逻辑
    return true; // 返回 true 表示已处理
}, priority);
```

### 注册自定义纹理生成
```java
CanvasImageProviderRegistry.register(new CanvasImageProvider() {
    @Override
    public NativeImage createImage(CanvasFace face) {
        // 自定义纹理生成逻辑
    }
}, 0);
```
对于希望扩展 Pigmentum 或创建自定义画笔、渲染器和效果的开发者，请参阅 [Pigmentum API 文档](./API.md)。

---

## 📄 许可证

MIT License

---

## 🙏 致谢

感谢 Mojang 和 NeoForge 团队提供的模组开发平台。

---

*Pigmentum Mod - 让 Minecraft 的每一面都成为画布。*

---

_**此mod包含许多ai生成代码，虽经过作者校验，但是因为作者本身mod经验较浅，无法保证实现方式和实际效率，欢迎issues反馈**_