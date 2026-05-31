# Pigmentum API 文档

欢迎使用 Pigmentum 模组的 API！本文档介绍如何扩展或集成 Pigmentum 的画布系统。您可以使用这些接口和类来创建自定义画笔、渲染器、纹理生成器、混合函数和画布物品。

---

## 目录
1. [画笔提供者](#画笔提供者)
2. [画笔注册表](#画笔注册表)
3. [画笔图案与像素提供者](#画笔图案与像素提供者)
4. [画布渲染器](#画布渲染器)
5. [渲染上下文](#渲染上下文)
6. [渲染器注册表](#渲染器注册表)
7. [图像提供者](#图像提供者)
8. [图像提供者上下文](#图像提供者上下文)
9. [图像提供者注册表](#图像提供者注册表)
10. [纹理管理器](#纹理管理器)
11. [混合模式](#混合模式)
12. [混合函数](#混合函数)
13. [默认混合实现](#默认混合实现)
14. [画布数据模型](#画布数据模型)
15. [画布数据持有者](#画布数据持有者)
16. [资源包](#资源包)
17. [数据组件](#数据组件)
18. [附件类型](#附件类型)
19. [其他 API 接口](#其他-api-接口)
20. [示例](#示例)

---

## 画笔提供者

每个画笔物品必须实现 `IPaintProvider` 接口。该接口定义了画笔如何提供颜色、图案、步长、tick 行为以及可选的混合函数。

```java
public interface IPaintProvider {
    @Nullable
    Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos, CanvasFace face, int pixelX, int pixelY);

    @Nullable
    PaintPattern getPattern(ItemStack stack, Player player, Level level,
                            BlockPos pos, Vec3 hitLoc);

    default Double getStep() { return 0.01; }

    default boolean onPaintTick(ItemStack stack, Player player, Level level) { return true; }

    default int getPaintInterval() { return 1; }

    @Nullable
    default BlendFunction getCustomBlendFunction(ItemStack stack) { return null; }
}
```

- `getColor`: 返回在指定像素点绘制的 ARGB 颜色。返回 `null` 表示擦除该像素。
- `getPattern`: 可选地返回一个 `PaintPattern`，用于批量绘制。重写此方法可定义自定义画笔形状。返回 `null` 则使用默认的圆形画笔逻辑。
- `getStep`: 鼠标位置插值时使用的步长，默认 `0.01`（1/100 方块）。
- `onPaintTick`: 画笔激活时每渲染帧调用一次。返回 `false` 会跳过当前帧的绘制。
- `getPaintInterval`: 两次绘制之间的渲染帧间隔。`1` 表示每帧都绘制，`2` 表示隔一帧画一次。
- `getCustomBlendFunction`: 提供自定义的 `BlendFunction` 以实现高级颜色/效果混合。返回 `null` 则使用基于画笔混合模式的标准混合（见 [混合模式](#混合模式)）。

---

## 画笔注册表

注册您的画笔物品，使 Pigmentum 能够在绘画时识别它们。

```java
public class PaintProviders {
    public static void register(Item item, IPaintProvider provider);
    @Nullable
    public static IPaintProvider getProvider(ItemStack stack);
    public static boolean isPaintbrush(ItemStack stack);
}
```

在模组初始化时（例如 `FMLCommonSetupEvent`）调用 `PaintProviders.register(item, provider)`。画笔随后将在玩家手持时自动生效。

注册表内部使用 `ConcurrentHashMap`，线程安全。

---

## 画笔图案与像素提供者

### PaintPattern

`IPaintProvider.getPattern()` 的返回类型，描述画笔的图案形状。

```java
public record PaintPattern(double width, double height, PixelProvider provider) {}
```

- `width` / `height`: 图案的覆盖范围（以方块为单位，例如 `0.5` 表示半个方块宽）。
- `provider`: 为图案内每个像素提供颜色的 `PixelProvider`。

### PixelProvider

为图案中的每个像素位置提供颜色和效果值。

```java
public interface PixelProvider {
    BlendMode getBlendMode();
    default BlendMode getBlendMode(double dx, double dy) { return getBlendMode(); }

    @Nullable
    Integer getPixel(double dx, double dy);

    default Map<String, Integer> getEffectValues(double dx, double dy) {
        return Collections.emptyMap();
    }
}
```

- `getPixel(dx, dy)`: `dx`, `dy` 是相对于命中点的像素偏移（可以为浮点数）。返回该位置的 ARGB 颜色，返回 `null` 表示该位置不绘制。
- `getBlendMode`: 返回此图案的混合模式。重载版本可根据像素位置返回不同模式。
- `getEffectValues`: 返回此像素点的附加效果值映射，如 `{"glow": 255, "roughness": 128}`。

---

## 画布渲染器

替换或增强画布面的渲染方式。创建自定义 `CanvasPixelRenderer` 并按优先级注册。

```java
public interface CanvasPixelRenderer {
    default boolean canRender(RenderContext context) { return true; }
    boolean renderFace(RenderContext context);
}
```

- `canRender`: 检查 `RenderContext`，返回 `false` 可跳过特定面的此渲染器。
- `renderFace`: 执行自定义渲染。直接向上下文中的 `MultiBufferSource` 绘制几何体。返回 `true` 表示该面已渲染完毕，不再调用后续渲染器；返回 `false` 则继续尝试下一个渲染器或默认实现。

### 默认渲染器 (DefaultCanvasPixelRenderer)

默认实现使用 `entityTranslucent` 渲染类型绘制一个带纹理的四边形。自定义渲染器可参考其实现：

```java
public class DefaultCanvasPixelRenderer implements CanvasPixelRenderer {
    @Override
    default boolean canRender(RenderContext context) {
        // 仅处理路径中包含 "_default_" 的纹理
        return !(context.texture == null || !context.texture.getPath().contains("_default_"));
    }

    @Override
    boolean renderFace(RenderContext context);
}
```

---

## 渲染上下文

`RenderContext` 是自定义渲染器的核心参数，包含渲染一个画布面所需的全部上下文信息。

```java
public class RenderContext {
    public final CanvasFace face;             // 当前渲染的面
    public final ResourceLocation texture;    // 要绑定的纹理
    public final PoseStack poseStack;         // 变换矩阵栈
    public final MultiBufferSource bufferSource; // 顶点缓冲输出
    public final int packedLight;             // 光照值（已根据 occlusion 调整）
    public final int packedOverlay;           // 覆盖层 UV
    public final Level level;                 // 世界实例
    public final BlockPos pos;                // 方块坐标
    public final boolean isOcclusion;         // 是否为实心方块（影响 AO 光照计算）

    public RenderContext(CanvasFace face, ResourceLocation texture, PoseStack poseStack,
                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                         Level level, BlockPos pos, boolean isOcclusion);
}
```

- `packedLight`: 使用满亮度渲染时传入 `0x00F000F0`。
- `bufferSource`: 通过 `context.bufferSource.getBuffer(RenderType.entityTranslucent(texture))` 获取 `VertexConsumer` 来绘制顶点。
- `isOcclusion`: `true` 时使用 Occlusion 光照模型（AO），`false` 时使用 NoOcclusion 光照模型。

---

## 渲染器注册表

```java
public class CanvasRendererRegistry {
    public static synchronized void registerPixelRenderer(CanvasPixelRenderer renderer, int priority);
    public static CanvasPixelRenderer resolve(RenderContext context);
    public static synchronized void setDefaultRenderer(CanvasPixelRenderer renderer);
}
```

- `registerPixelRenderer`: 注册自定义像素渲染器。优先级越高的渲染器越先被调用。
- `resolve`: 遍历注册表，返回第一个 `canRender` 为 `true` 的渲染器；若无，返回默认渲染器。
- `setDefaultRenderer`: 替换默认渲染器（正常情况下无需调用，除非要完全重写默认渲染行为）。

---

## 图像提供者

重写画布像素转换为 `NativeImage` 的过程，以自定义动态纹理的生成。

```java
public interface CanvasImageProvider {
    @Nullable
    NativeImage createImage(CanvasFace face);

    default String name() {
        return "default";
    }

    default boolean canProvide(ImageProviderContext context) {
        return true;
    }
}
```

- `createImage`: 根据给定的面生成 `NativeImage`。返回 `null` 表示跳过该提供者——不影响其他提供者的纹理生成。
- `name`: 返回此图像提供者的名称，用于纹理资源路径标识。默认返回 `"default"`。
- `canProvide`: 判断此提供者是否可以为给定的上下文生成图像。返回 `false` 时该提供者将被跳过。用于按条件启用/禁用提供者（例如仅在特定维度生效）。

默认情况下，系统预注册 `DefaultCanvasImageProvider`（`name()` 返回 `"default"`），将像素矩阵直接转换为 RGBA 纹理。

---

## 图像提供者上下文

`ImageProviderContext` 是传入 `canProvide` 的参数，包含当前面以及可选的世界上下文。

```java
public class ImageProviderContext {
    public final CanvasFace face;
    @Nullable
    public final Level level;
    @Nullable
    public final BlockPos pos;

    public ImageProviderContext(CanvasFace face, @Nullable Level level, @Nullable BlockPos pos);
}
```

- `face`: 当前要生成纹理的画布面。
- `level` / `pos`: 可用于维度检测、生物群系感知等场景。当前实现中可能为 `null`。

---

## 图像提供者注册表

所有 `CanvasImageProvider` 的注册入口，内部按优先级排序。与 `CanvasRendererRegistry`（单选）不同，此注册表收集 **所有** `canProvide` 为 true 的提供者。

```java
public class CanvasImageProviderRegistry {
    /** 注册自定义图像提供者。优先级越高越先生成纹理 */
    public static synchronized void register(CanvasImageProvider provider, int priority);

    /** 移除一个提供者。默认提供者不可移除。返回 true 表示移除成功 */
    public static synchronized boolean unregister(CanvasImageProvider provider);

    /** 获取所有 canProvide 为 true 的提供者，按优先级降序排列，默认提供者始终在末尾 */
    public static List<CanvasImageProvider> resolveAll(ImageProviderContext context);
}
```

- `register`: 注册自定义提供者，默认提供者固定优先级最低（`Integer.MIN_VALUE`）。
- `unregister`: 移除提供者。注意默认提供者不可移除。
- `resolveAll`: 返回所有匹配提供者。`CanvasTextureManager` 调用此方法获取列表，逐个生成纹理后打包为 `ResourcesBundle`。

在模组初始化时注册：
```java
CanvasImageProviderRegistry.register(new GlowImageProvider(), 10);
```

---

## 纹理管理器

管理画布动态纹理的创建、缓存与释放生命周期。**不直接持有图像提供者列表**——提供者的注册与查询由 `CanvasImageProviderRegistry` 负责。

```java
public class CanvasTextureManager {
    /** 使用指定提供者从面生成 NativeImage */
    public static NativeImage createImage(CanvasFace face, CanvasImageProvider provider);

    /** 通过 CanvasImageProviderRegistry 获取所有匹配提供者，为每个生成纹理，打包为 ResourcesBundle */
    public static ResourcesBundle createOrUpdateTexture(CanvasFace face, int entityId, int faceIndex);

    /** 释放指定实体的某个面的所有纹理 */
    public static void releaseTexture(int entityId, int faceIndex);

    /** 释放指定实体的所有面的纹理 */
    public static void releaseTextures(int entityId);
}
```

- `createOrUpdateTexture`: 创建 `ImageProviderContext`，调用 `CanvasImageProviderRegistry.resolveAll()` 获取所有匹配提供者，逐一调用 `createImage`，将结果打包为 `ResourcesBundle`。单个提供者返回 `null` 会被跳过（`continue`），不影响其他提供者。
- `releaseTexture` / `releaseTextures`: 释放对应实体的纹理资源，调用 `Minecraft.getTextureManager().release()`。

> 纹理命名遵循内部约定 `{entityId}_{faceIndex}_{providerName}_{counter}`，此格式为实现细节，不应在自定义渲染器的 `canRender()` 中硬依赖具体格式。

---

## 混合模式

```java
public enum BlendMode {
    OVERWRITE,   // 覆盖：直接替换像素颜色和效果值
    ADD,         // 叠加：alpha 混合，效果值取最大
    MULTIPLY,    // 正片叠底：颜色相乘，效果值按比例混合
    ERASE;       // 擦除：清空像素和所有效果值

    public String getTranslationKey();
    public BlendFunction getDefaultFunction();
}
```

- `getTranslationKey()`: 返回形如 `painter.config.blend_mode.overwrite` 的本地化键。
- `getDefaultFunction()`: 返回对应的 `DefaultBlendFunctions` 中的标准实现。

---

## 混合函数

完全控制新颜色和效果值如何与画布上已有内容混合。

```java
@FunctionalInterface
public interface BlendFunction {
    boolean apply(BlendContext context);
}

public class BlendContext {
    public final CanvasFace face;
    public final int px, py;
    public final int existingColor;           // 画布上已有的 ARGB 颜色
    public final int newColor;                // 画笔提供的 ARGB 颜色
    public final BlendMode mode;              // 当前混合模式
    public final ItemStack brushStack;        // 画笔物品栈
    public final Map<String, Integer> effectValues;  // 画笔提供的效果值映射

    /** 便捷写入像素效果值 */
    public void setEffect(String key, int value);

    /** 便捷读取像素效果值 */
    public int getEffect(String key);
}
```

- `apply`: 处理混合逻辑。可直接通过 `context.face.pixels().setPixel(...)` 修改像素，通过 `context.setEffect(...)` 修改效果层。返回 `true` 表示像素已被更改。
- `effectValues`: 由 `PixelProvider.getEffectValues()` 提供，如 `{"glow": 255}`。

---

## 默认混合实现

`DefaultBlendFunctions` 提供四个标准实现，与 `BlendMode` 枚举一一对应：

```java
public class DefaultBlendFunctions {
    public static final BlendFunction OVERWRITE;   // 直接替换像素和效果值
    public static final BlendFunction ADD;         // alpha 合成叠加
    public static final BlendFunction MULTIPLY;    // 正片叠底
    public static final BlendFunction ERASE;       // 清空像素和效果值
}
```

如果自定义 `BlendFunction` 部分逻辑与标准行为相同，可直接委托调用：

```java
public BlendFunction getCustomBlendFunction(ItemStack stack) {
    return context -> {
        // 先执行标准覆盖混合
        DefaultBlendFunctions.OVERWRITE.apply(context);
        // 再追加额外的效果值
        context.setEffect("glow", 255);
        return true;
    };
}
```

---

## 画布数据模型

### IPixelMatrix

像素矩阵的接口抽象，`PixelMatrix` 实现了此接口。

```java
public interface IPixelMatrix {
    int SIZE = 16;
    int PIXEL_COUNT = 256;  // SIZE * SIZE

    int getWidth();
    int getHeight();
    int getPixel(int x, int y);
    boolean setPixel(int x, int y, int color);
    IPixelMatrix copy();
    void fill(int color);
    int[] getPixels();
    boolean isEmpty();

    /** 创建一个 16×16 的空白矩阵（全部透明） */
    static IPixelMatrix createEmpty();
}
```

### PixelMatrix

可变二维 ARGB 像素数组，实现 `IPixelMatrix`。支持 Codec 和 StreamCodec 序列化。

```java
public class PixelMatrix implements IPixelMatrix {
    public PixelMatrix(int width, int height);
    public PixelMatrix();  // 默认 16×16

    public int getWidth();
    public int getHeight();
    public int getPixel(int x, int y);
    public boolean setPixel(int x, int y, int color);
    public void fill(int color);
    public int[] getPixels();     // 返回内部数组的引用（非副本）
    public boolean isEmpty();

    public void fillWhite();
    public static PixelMatrix fullWhite();

    // 序列化支持
    public static final Codec<PixelMatrix> CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, PixelMatrix> STREAM_CODEC;
}
```

> **注意**: `copy()` 方法当前返回 `null`（未实现），请勿在生产代码中依赖它。`getPixels()` 返回内部数组的直接引用，修改返回值会影响矩阵本身。

### CanvasFace

描述方块一个被绘制的表面，包含四个角点、像素矩阵以及可选的效果层。

**构造器**（推荐使用基于中心偏移的版本）：

```java
// 基础构造：指定主面和四个相对于方块中心的角点
public CanvasFace(Direction primaryFace, Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3, PixelMatrix pixels);
public CanvasFace(Direction primaryFace, Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3, PixelMatrix pixels, Map<String, byte[]> effectLayers);

// 便捷构造：由中心偏移和像素矩阵尺寸自动计算四个角点
public CanvasFace(Direction primaryFace, Vec3 centerOffset, PixelMatrix pixels);
public CanvasFace(Direction primaryFace, Vec3 centerOffset, PixelMatrix pixels, Map<String, byte[]> effectLayers);
```

**字段访问**:

```java
public Direction primaryFace();  // 该面的主方向
public Vec3 corner0();           // 左下角（相对于方块中心）
public Vec3 corner1();           // 右下角（相对于方块中心）
public Vec3 corner2();           // 右上角（相对于方块中心）
public Vec3 corner3();           // 左上角（相对于方块中心）
public Vec3 centerOffset();      // 四角均值，即面中心偏移
public PixelMatrix pixels();     // 像素数据
```

**实用方法**:

```java
// 返回沿法线方向略微偏移（0.001）后的角点，用于渲染时避免 z-fighting
public Vec3[] cornerWithOffset();

// 判断两个 CanvasFace 是否在同一个表面上（方向 + 四点容差 0.001 相等）
public boolean isSameSurface(CanvasFace other);
```

**效果层**:

效果层以字节数组形式存储每像素值（每个像素一个字节，范围 0-255）。典型用途包括发光强度、粗糙度、金属度等。

```java
public byte[] getEffectLayer(String key);
public void setEffectLayer(String key, byte[] data);
public Map<String, byte[]> getEffectLayers();  // 不可变视图

// 获取/设置单个像素的效果值
public int getEffectValue(String key, int x, int y);
public void setEffectValue(String key, int x, int y, int value);
```

**序列化**: 支持 `Codec<CanvasFace>` 和 `StreamCodec<RegistryFriendlyByteBuf, CanvasFace>`，包含效果层的完整序列化。

### CanvasData

包含方块所有被绘制面的 `CanvasFace` 列表，以及版本号用于变更追踪。

```java
public class CanvasData {
    public static CanvasData empty();
    public List<CanvasFace> faces();
    public long getVersion();
    public void incrementVersion();

    /** 如果相同表面不存在则添加，否则返回已存在的面 */
    public CanvasFace addOrGetFace(CanvasFace newFace);

    /** 查找相同表面，找不到返回 null */
    public CanvasFace tryGetFace(CanvasFace newFace);

    /** 通过方向和中心偏移查找面 */
    public Optional<CanvasFace> getFace(Direction primaryFace, Vec3 centerOffset);

    /** 通过命中点查找所有相交的面（带容差参数） */
    public List<CanvasFace> getFaceAtHit(BlockPos pos, Vec3 hitLoc);
    public List<CanvasFace> getFaceAtHit(BlockPos pos, Vec3 hitLoc, double offset);

    /** 通过 BlockHitResult 查找命中的单个面，未命中返回 null */
    @Nullable
    public CanvasFace getFaceAtHit(BlockPos pos, BlockHitResult hitResult);

    /** 根据命中点与方块碰撞箱计算一个新的 CanvasFace */
    @Nullable
    public static CanvasFace calculateCanvasFace(Level level, BlockPos pos, BlockState state,
                                                  Vec3 hitLoc, Direction face);

    /** 获取或创建画布面：已有方块实体则追加，否则将方块替换为 CanvasBlock */
    @Nullable
    public static Pair<CanvasData, CanvasFace> getOrCreateCanvasFace(Level level, BlockPos pos,
                                                                      BlockState state, Vec3 hitLoc, Direction face);

    public boolean isEmpty();
}
```

- `getFaceAtHit(BlockPos, BlockHitResult)`: 使用 UV 坐标检测命中点是否落在某个面的四边形范围内，容差约 0.001。**可能返回 `null`**。
- `getFaceAtHit(BlockPos, Vec3)` 返回 **列表**：一个命中点可能同时落在多个重叠面上。
- `calculateCanvasFace`: 遍历方块 `VoxelShape` 的所有 AABB，匹配命中面方向和坐标，生成与碰撞箱对齐的 `CanvasFace`。适用于不规则方块（楼梯、半砖等）。
- `getOrCreateCanvasFace`: 组合方法，包含方块替换逻辑。内部检查 `CanvasBlacklist` 以排除基岩、命令方块等。

**序列化**: 支持 `Codec<CanvasData>` 和 `StreamCodec<RegistryFriendlyByteBuf, CanvasData>`。

---

## 画布数据持有者

`CanvasDataHolder` 是让方块实体能够承载画布数据的接口。`CanvasBlockEntity` 实现了此接口，第三方模组的方块实体也可实现它以追加画布支持。

```java
public interface CanvasDataHolder {
    /** 获取该方块实体的画布数据，通过 NeoForge Attachment 存储 */
    @Nullable
    default CanvasData painter$getCanvasData();

    /** 设置画布数据 */
    default void painter$setCanvasData(CanvasData data);

    /** 获取缓存的 (CanvasFace, ResourcesBundle) 纹理映射 */
    @Nullable
    List<Pair<CanvasFace, ResourcesBundle>> painter$getCachedFaceTextures();

    /** 根据数据重新生成所有纹理 */
    void painter$regenerateTextures(CanvasData data);

    /** 释放所有纹理资源 */
    void painter$releaseTextures();
}
```

方法名使用 `painter$` 前缀以避免与其他模组的接口方法冲突。

---

## 资源包

`ResourcesBundle` 封装多个纹理资源位置，对应一个面的多个图像提供者输出。

```java
public record ResourcesBundle(ResourceLocation[] resourceLocations) {}
```

例如，默认情况下只有一个 `DefaultCanvasImageProvider`，`ResourcesBundle` 包含一个纹理。注册了额外的 Glow 提供者后，则包含两个（分别在路径中包含 `_default_` 和 `_glow_` 后缀）。

---

## 数据组件

预定义的用于在物品上存储画布/画笔数据的数据组件。使用 NeoForge 的 `DeferredRegister` 注册。

```java
public class ModDataComponents {
    public static final Supplier<DataComponentType<CanvasData>> CANVAS;
    public static final Supplier<DataComponentType<BlockState>> BLOCK_STATE;
    public static final Supplier<DataComponentType<Integer>> CURRENT_COLOR;
    public static final Supplier<DataComponentType<Double>> BRUSH_SIZE;
    public static final Supplier<DataComponentType<Float>> FEATHER_STRENGTH;
    public static final Supplier<DataComponentType<String>> BLEND_MODE;
    public static final Supplier<DataComponentType<Double>> STEP_SIZE;
    public static final Supplier<DataComponentType<Float>> OPACITY;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CanvasFace>> STORED_FACE;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CANVAS_TEXTURE;
}
```

- `CANVAS`: 方块上存储的画布数据。支持持久化 + 网络同步。
- `BLOCK_STATE`: CanvasBlock 被模仿的原始方块状态。支持持久化 + 网络同步。
- `CURRENT_COLOR`: 画笔当前选取的 ARGB 颜色。支持持久化 + 网络同步。
- `BRUSH_SIZE` / `FEATHER_STRENGTH` / `STEP_SIZE` / `OPACITY`: 画笔配置参数。支持持久化 + 网络同步。
- `BLEND_MODE`: 混合模式字符串（如 `"add"`）。支持持久化 + 网络同步。（注：当前使用 `String` 类型，后续版本可能改用 `BlendMode` 枚举的 Codec。）
- `STORED_FACE`: 存储在 `CanvasSheet` 物品上的单个 `CanvasFace`。仅持久化（**不同步**到客户端——物品本身会被同步，所以数据会随物品到达客户端）。
- `CANVAS_TEXTURE`: 纹理资源路径字符串。仅持久化（**不同步**，客户端内部由 `CanvasTextureManager` 管理）。

> **类型差异**: `STORED_FACE` 和 `CANVAS_TEXTURE` 的声明类型是 `DeferredHolder<DataComponentType<?>, DataComponentType<T>>` 而非 `Supplier<DataComponentType<T>>`。这是由于使用了 `DeferredRegister.register()` 的不同重载（返回 `DeferredHolder` 而非通用 `Supplier`）。两者都实现了 `Supplier` 接口，大多数情况下可按 `Supplier` 使用。

---

## 附件类型

使用 NeoForge 的 Attachment 系统将 `CanvasData` 附加到任意方块实体上。

```java
public class ModAttachments {
    public static final Supplier<AttachmentType<CanvasData>> CANVAS_DATA;
}
```

- 通过 `ModAttachments.CANVAS_DATA` 获取 Attachment 类型。
- `CanvasDataHolder` 接口的默认方法内部使用此 Attachment 存取数据。
- 支持 Codec 持久化序列化和 StreamCodec 网络同步。
- 默认值为 `CanvasData.empty()`（即空画布列表）。

---

## 其他 API 接口

以下接口为进阶功能提供支持，适用于需要构建复杂画布系统的第三方模组。

### IPaintLayer

附着在方块单一表面上的绘画图层。

```java
public interface IPaintLayer {
    BlockPos getPos();
    Direction getFace();
    IPixelMatrix getPixels();
    BlockState getBlockState(Level level);
    BlockEntity getBlockEntity(Level level);
    CompoundTag save();  // 序列化到 NBT
}
```

### ExposurePredicate

判断方块面是否暴露可绘制的函数式接口。

```java
@FunctionalInterface
public interface ExposurePredicate {
    boolean isExposed(Level level, BlockPos pos, Direction face);

    /** 默认实现：相邻方块为空气或非完整渲染方块 */
    ExposurePredicate DEFAULT = (level, pos, face) -> {
        BlockState neighbor = level.getBlockState(pos.relative(face));
        return neighbor.isAir() || !neighbor.isSolidRender(level, pos.relative(face));
    };
}
```

### CompositePainting

将多个方块面上的 `IPaintLayer` 组合为一张复合画布。支持 Builder 模式。

```java
public class CompositePainting {
    public Set<BlockPos> getInvolvedPositions();
    public Map<Direction, List<IPaintLayer>> groupByFacing();

    /** 尝试将所有图层拼接为单个矩阵。当前实现抛出 UnsupportedOperationException */
    public IPixelMatrix combineToMatrix();

    public static class Builder {
        public Builder add(IPaintLayer layer);
        public CompositePainting build();
    }
}
```

> **注意**: `combineToMatrix()` 尚未实现，调用会抛出异常。

---

## 示例

---

### 注册一个自定义红色画笔

```java
@SubscribeEvent
public static void setup(FMLCommonSetupEvent event) {
    PaintProviders.register(MY_RED_BRUSH.get(), new IPaintProvider() {
        @Override
        public Integer getColor(ItemStack stack, Player player, Level level,
                                BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
            return 0xFFFF0000; // 纯红色 ARGB
        }
    });
}
```

---

### 使用自定义混合函数添加发光效果层

```java
public class GlowBrush implements IPaintProvider {
    @Override
    public Integer getColor(ItemStack stack, Player player, Level level,
                            BlockPos pos, CanvasFace face, int pixelX, int pixelY) {
        return 0xFFFFFFFF; // 白色
    }

    @Override
    public BlendFunction getCustomBlendFunction(ItemStack stack) {
        return context -> {
            // 先执行标准覆盖混合（像素写入 + 效果值）
            DefaultBlendFunctions.OVERWRITE.apply(context);
            // 再追加发光效果值
            context.setEffect("glow", 255);
            return true;
        };
    }
}
```

---

### 自定义图像提供者生成发光纹理

要实现独立的发光渲染，需要将发光像素单独输出为一张纹理。创建一个 `GlowImageProvider`，它从效果层中读取发光数据，生成只包含发光信息的 `NativeImage`。通过 `CanvasImageProviderRegistry` 注册后，`CanvasTextureManager` 会为每个面调用所有 `canProvide` 返回 true 的提供者，生成多个纹理，存储于 `ResourcesBundle` 中。

```java
public class GlowImageProvider implements CanvasImageProvider {
    public static final String NAME = "glow";

    @Override
    public NativeImage createImage(CanvasFace face) {
        byte[] glowLayer = face.getEffectLayer("glow");
        if (glowLayer == null) {
            return null;  // 没有发光数据，不生成纹理
        }
        int w = face.pixels().getWidth();
        int h = face.pixels().getHeight();
        NativeImage img = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int glow = glowLayer[y * w + x] & 0xFF;
                // 发光强度作为 alpha 通道，颜色使用白色 (ABGR 格式)
                int abgr = (glow << 24) | 0xFFFFFF;
                img.setPixelRGBA(x, y, abgr);
            }
        }
        return img;
    }

    @Override
    public String name() {
        return NAME;
    }
}

// 在模组初始化时注册
CanvasImageProviderRegistry.register(new GlowImageProvider(), 0);
```

---

### 自定义渲染器渲染发光纹理

默认渲染器只会使用基础画布纹理。要渲染发光层，需要实现自定义 `CanvasPixelRenderer`，从 `ResourcesBundle` 中选取发光纹理，并使用满亮度、无环境光遮蔽的方式叠加绘制。

```java
CanvasRendererRegistry.registerPixelRenderer(new CanvasPixelRenderer() {
    @Override
    public boolean canRender(RenderContext context) {
        // 通过 imageProvider 名称来识别发光纹理，而非硬编码路径格式
        return !(context.texture == null || !context.texture.getPath().contains("_glow_"));
    }

    @Override
    public boolean renderFace(RenderContext context) {
        var face = context.face;
        var texture = context.texture;
        if (texture == null) return false;

        // 使用满亮度渲染
        int fullBright = 0x00F000F0;

        Vec3[] corners = face.cornerWithOffset();
        var vc = context.bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        var pose = context.poseStack.last();
        Direction dir = face.primaryFace();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;

        // 绘制四边形，使用满亮度
        addVertex(vc, pose, corners[0], 0, 0, nx, ny, nz, fullBright, context.packedOverlay);
        addVertex(vc, pose, corners[1], 1, 0, nx, ny, nz, fullBright, context.packedOverlay);
        addVertex(vc, pose, corners[2], 1, 1, nx, ny, nz, fullBright, context.packedOverlay);
        addVertex(vc, pose, corners[3], 0, 1, nx, ny, nz, fullBright, context.packedOverlay);
        return true;
    }
}, 10); // 优先级 10，晚于默认渲染器执行
```

辅助方法：

```java
private static void addVertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 pos, float u, float v,
                               float nx, float ny, float nz, int light, int overlay) {
    vc.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
      .setColor(255, 255, 255, 255)
      .setUv(u, v)
      .setOverlay(overlay)
      .setLight(light)
      .setNormal(pose, nx, ny, nz);
}
```

这样，拥有发光效果层的画布面就会额外渲染一层始终明亮的发光纹理，实现夜光或高亮效果。

---

## 最佳实践

1. **纹理区分**: 在自定义渲染器的 `canRender()` 中，应通过 `CanvasImageProvider.name()` 的命名约定来识别纹理，而非硬编码内部路径生成格式。
2. **效果层绑定**: 自定义 `BlendFunction` + `CanvasImageProvider` + `CanvasPixelRenderer` 三者通过效果层的 key（如 `"glow"`）隐式绑定，确保三端使用一致的 key 名。
3. **委托默认行为**: 当自定义 `BlendFunction` 仅需追加效果值时，先调用 `DefaultBlendFunctions` 中的对应实现，再处理额外效果值——避免重复实现标准混合逻辑。
4. **方块替换**: `getOrCreateCanvasFace` 内部会自动将方块替换为 `CanvasBlock`。若需在替换后执行额外逻辑，可自行调用 `calculateCanvasFace` + 手动方块操作，而非使用便捷方法。
5. **纹理生命周期**: 自定义 `CanvasDataHolder` 实现必须正确实现 `painter$releaseTextures()`，在方块实体卸载时调用 `CanvasTextureManager.releaseTextures(entityId)`，防止显存泄漏。
6. **注册入口**: 使用 `CanvasImageProviderRegistry.register()` 注册图像提供者，而非直接操作 `CanvasTextureManager` 的旧 API。两套注册表（渲染器 / 图像提供者）职责分明：一个负责"如何渲染"，一个负责"生成什么纹理"。
