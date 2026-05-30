# Pigmentum API 文档

欢迎使用 Pigmentum 模组的 API！本文档介绍如何扩展或集成 Pigmentum 的画布系统。您可以使用这些接口和类来创建自定义画笔、渲染器、纹理生成器、混合函数和画布物品。

---

## 目录
1. [画笔提供者](#画笔提供者)
2. [画笔注册表](#画笔注册表)
3. [画布渲染器](#画布渲染器)
4. [图像提供者](#图像提供者)
5. [混合函数](#混合函数)
6. [画布数据模型](#画布数据模型)
7. [数据组件](#数据组件)
8. [画布提取物品](#画布提取物品)
9. [示例](#示例)

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
- `getPattern`: 可选地返回一个 `PaintPattern`，用于批量绘制。重写此方法可定义自定义画笔形状。
- `getStep`: 鼠标位置插值时使用的步长（默认 `0.01` 方块）。
- `onPaintTick`: 画笔激活时每渲染帧调用一次。返回 `false` 会跳过当前帧的绘制。
- `getPaintInterval`: 两次绘制之间的渲染帧间隔（1 表示每帧都绘制）。
- `getCustomBlendFunction`: 提供自定义的 `BlendFunction` 以实现高级颜色/效果混合。返回 `null` 则使用基于画笔混合模式的标准混合。

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

---

## 画布渲染器

替换或增强画布面的渲染方式。创建自定义 `CanvasPixelRenderer` 并按优先级注册。

```java
public interface CanvasPixelRenderer {
    default boolean canRender(RenderContext context) { return true; }
    boolean renderFace(RenderContext context);
}

public class CanvasRendererRegistry {
    public static synchronized void registerPixelRenderer(CanvasPixelRenderer renderer, int priority);
    public static CanvasPixelRenderer resolve(RenderContext context);
    public static synchronized void setDefaultRenderer(CanvasPixelRenderer renderer);
}
```

- `canRender`: 检查 `RenderContext`，返回 `false` 可跳过特定面的此渲染器。
- `renderFace`: 执行自定义渲染。直接向上下文中的 `MultiBufferSource` 绘制几何体。返回 `true` 表示该面已渲染完毕，不再调用后续渲染器。返回 `false` 则继续尝试下一个渲染器或默认实现。

优先级越高的渲染器越早被调用。默认渲染器绘制一个带纹理的四边形。

---

## 图像提供者

重写画布像素转换为 `NativeImage` 的过程，以自定义动态纹理的生成。

```java
public interface CanvasImageProvider {
    @Nullable
    NativeImage createImage(CanvasFace face);
}

public class CanvasTextureManager {
    public static void setImageProvider(CanvasImageProvider provider);
    public static NativeImage createImage(CanvasFace face);
    // ……其他纹理生命周期管理方法
}
```

- `setImageProvider`: 替换全局图像生成器。传入 `null` 可重置为默认实现。
- `createImage`: 根据给定的面生成 `NativeImage`。在纹理需要更新时调用。

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
    public final int existingColor;
    public final int newColor;
    public final BlendMode mode;
    public final ItemStack brushStack;
    public final Map<String, Integer> effectValues;

    public void setEffect(String key, int value);
    public int getEffect(String key);
}
```

- `apply`: 处理混合逻辑。可直接修改 `face` 的像素或效果层。返回 `true` 表示像素已被更改。
- `BlendContext`: 包含当前绘制操作的全部信息，包括画笔提供的效果值。

---

## 画布数据模型

### PixelMatrix

可变二维 ARGB 像素数组。

```java
public class PixelMatrix {
    public PixelMatrix(int width, int height);
    public int getWidth();
    public int getHeight();
    public int getPixel(int x, int y);
    public boolean setPixel(int x, int y, int color);
    public void fill(int color);
    public int[] getPixels();
    public boolean isEmpty();
}
```

### CanvasFace

描述方块一个被绘制的表面，包含四个角点、像素矩阵以及可选的效果层。

```java
public class CanvasFace {
    public Direction primaryFace();
    public Vec3 corner0(), corner1(), corner2(), corner3();
    public Vec3 centerOffset();
    public PixelMatrix pixels();
    public Vec3[] cornerWithOffset();   // 沿法线方向略微偏移的角点

    // 效果层
    public byte[] getEffectLayer(String key);
    public void setEffectLayer(String key, byte[] data);
    public Map<String, byte[]> getEffectLayers();
    public int getEffectValue(String key, int x, int y);
    public void setEffectValue(String key, int x, int y, int value);
    
    public boolean isSameSurface(CanvasFace other);
}
```

- `corner0` 至 `corner3`: 相对于方块中心的局部坐标。
- 效果层以字节数组形式存储每像素值（例如发光强度）。每个像素使用层数组中的一个字节。

### CanvasData

包含方块所有被绘制面的 `CanvasFace` 列表。

```java
public class CanvasData {
    public static CanvasData empty();
    public List<CanvasFace> faces();
    public CanvasFace addOrGetFace(CanvasFace newFace);
    public CanvasFace tryGetFace(CanvasFace newFace);
    public Optional<CanvasFace> getFace(Direction primaryFace, Vec3 centerOffset);
    public List<CanvasFace> getFaceAtHit(BlockPos pos, Vec3 hitLoc);
    public CanvasFace getFaceAtHit(BlockPos pos, BlockHitResult hitResult);
    public static CanvasFace calculateCanvasFace(Level level, BlockPos pos, BlockState state, Vec3 hitLoc, Direction face);
    public static Pair<CanvasData, CanvasFace> getOrCreateCanvasFace(Level level, BlockPos pos, BlockState state, Vec3 hitLoc, Direction face);
    public long getVersion();
    public void incrementVersion();
    public boolean isEmpty();
}
```

- `addOrGetFace`: 如果不存在相同表面则添加面，否则返回已存在的面。
- `calculateCanvasFace`: 根据命中位置构造一个与方块碰撞箱对齐的新 `CanvasFace`。
- `getOrCreateCanvasFace`: 便捷方法，要么获取现有画布，要么将方块转换为画布方块并创建新面。

---

## 数据组件

预定义的用于在物品上存储画布/画笔数据的数据组件。

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
    public static final Supplier<DataComponentType<CanvasFace>> STORED_FACE;
    public static final Supplier<DataComponentType<String>> CANVAS_TEXTURE;
}
```

- `CURRENT_COLOR`: 画笔当前选取的 ARGB 颜色。
- `BRUSH_SIZE`, `FEATHER_STRENGTH`, `BLEND_MODE`, `STEP_SIZE`, `OPACITY`: 画笔配置参数。
- `STORED_FACE`: 存储在 `CanvasSheet` 物品上的单个 `CanvasFace`。
- `CANVAS_TEXTURE`: 纹理资源路径字符串（客户端内部使用）。

---

## 画布提取物品

`CanvasSheet` 物品允许玩家将画布的一个面复制到物品上，并可在物品展示框中展示。

```java
public class CanvasSheet extends Item {
    public static void ensureTexture(ItemStack stack);
}
```

- `ensureTexture`: (仅客户端) 生成或重新生成该画布物品的动态纹理。会在需要时自动调用。

当玩家潜行右击一个已绘制的方块时，被击中的面会被克隆到 `STORED_FACE` 组件中，同时生成纹理。物品存储有面数据时，其模型会自动切换到 `custom_model_data=1` 的样式。

---

## 示例

### 注册一个自定义红色画笔

```java
@SubscribeEvent
public static void setup(FMLCommonSetupEvent event) {
    PaintProviders.register(MY_RED_BRUSH.get(), new IPaintProvider() {
        @Override
        public Integer getColor(...) {
            return 0xFFFF0000; // 纯红色
        }
    });
}
```

### 使用自定义混合函数添加发光效果层

```java
public class GlowBrush implements IPaintProvider {
    @Override
    public Integer getColor(...) { return 0xFFFFFFFF; }

    @Override
    public BlendFunction getCustomBlendFunction(ItemStack stack) {
        return context -> {
            // 将发光强度写入效果层
            context.setEffect("glow", 255);
            // 同时绘制白色像素
            context.face.pixels().setPixel(context.px, context.py, 0xFFFFFFFF);
            return true;
        };
    }
}
```

### 自定义渲染器高亮显示发光像素

```java
CanvasRendererRegistry.registerPixelRenderer(new CanvasPixelRenderer() {
    @Override
    public boolean renderFace(RenderContext context) {
        // 检查该面是否有发光效果，并调整光照或添加自发光通道
        return false; // 让默认渲染器也继续执行
    }
}, 10);
```

---

此文档由deepseek生成，如果有任何不实，欢迎到issues中反馈。