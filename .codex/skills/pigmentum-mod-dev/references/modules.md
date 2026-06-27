# Pigmentum Module Usage

This file summarizes each module's purpose and the basic way to use it.

## Project Facts

- Mod name: Pigmentum
- Mod id: `painter`
- Base package: `com.astune.painter`
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- Public API root: `com.astune.painter.api`

## Entry Points

`Painter` is the common mod entrypoint. It registers blocks, items, block entities, data components, creative tabs, attachments, config, payload handlers, commands, and common event listeners.

`PainterClient` is client-dist only. It registers the NeoForge config screen extension point and client setup hooks.

## API Module

Use this module for downstream integrations.

- `IPaintProvider`: implement this for custom brush behavior.
- `PaintProviders`: register item-to-brush provider mappings.
- `PaintPattern`: define area brush dimensions and pixel provider.
- `PixelProvider`: provide ARGB pixels and optional effect values for each pattern point.
- `BlendMode`: select built-in blend behavior.
- `CanvasData`: collection of painted faces for a block.
- `CanvasFace`: one physical painted surface with face direction, corners, pixel matrix, and effect layers.
- `PixelMatrix`: packed pixel storage with codec and stream codec.
- `CanvasDataHolder`: bridge used by block entities that can carry canvas data.

Basic custom brush pattern:

```java
PaintProviders.register(MY_BRUSH_ITEM.get(), new IPaintProvider() {
    @Override
    public Integer getColor(ItemStack stack, Player player, Level level, BlockPos pos,
                            CanvasFace face, int pixelX, int pixelY) {
        return 0xFFFF0000;
    }

    @Override
    public PaintPattern getPattern(ItemStack stack, Player player, Level level,
                                   BlockPos pos, Vec3 hitLoc) {
        return null;
    }
});
```

Return `null` from `getColor` to erase. Return a `PaintPattern` for non-default shapes.

## Blend Module

Use `BlendFunction` for custom paint application. `BlendContext` gives access to `face`, `px`, `py`, `existingColor`, `newColor`, `mode`, `brushStack`, and `effectValues`.

Call `context.setEffect(key, value)` to write per-pixel effect layers. Call `context.getEffect(key)` to read an existing effect value.

Registering a custom blend is normally done by returning it from `IPaintProvider.getCustomBlendFunction(ItemStack)`.

## Image Provider Module

Use this when a canvas face needs generated texture data beyond the default pixel-to-image conversion.

- Implement `CanvasImageProvider.createImage(CanvasFace)`.
- Optionally override `name()` to produce stable texture resource path identifiers.
- Optionally override `canProvide(ImageProviderContext)`.
- Register with `CanvasImageProviderRegistry.register(provider, priority)`.

The registry keeps the default provider at the lowest priority. User providers are sorted by priority in ascending order; generated resources can be rendered as stacked textures.

## Render Module

Use this when generated textures are not enough and the face needs custom geometry or render pipeline behavior.

- Implement `CanvasPixelRenderer`.
- Use `canRender(RenderContext)` to restrict by texture, face, world, position, or occlusion mode.
- Draw through `RenderContext.poseStack` and `RenderContext.bufferSource`.
- Register with `CanvasRendererRegistry.registerPixelRenderer(renderer, priority)`.

Higher priority renderers are considered first. `CanvasRendererRegistry.resolve(context)` returns the first renderer whose `canRender` is true.

## Block Module

This is internal unless modifying Pigmentum.

- `CanvasBlock`: proxy block representing a painted original block.
- `OcclusionCanvasBlock` and `NoOcclusionCanvasBlock`: variants selected from original render occlusion.
- `CanvasBlockEntity`: stores mimicked block state and `CanvasData`.
- `CanvasBlockModel`: delegates model data to the mimicked block.
- `CanvasBlockItem`: item representation for canvas blocks.
- `CanvasBlockHelper`: helper behavior for canvas block operations.

Do not use this module from a downstream mod unless public API cannot express the feature.

## Item Module

- `Paintbrush`: normal player brush.
- `DebugPaintbrush`: reference implementation of `IPaintProvider`.
- `EffectCreator`: reference brush for effect-layer writes.
- `CanvasSheet`: copies a single canvas face into item components and supports item-frame display.

Use item classes as examples. For third-party brushes, implement your own item and register an `IPaintProvider`.

## Registry Module

Internal registration holders:

- `ModBlocks`
- `ModItems`
- `ModBlockEntities`
- `ModDataComponents`
- `ModCreativeTabs`
- `ModAttachments`
- `ModPaintProviders`

Useful data components include canvas data, mimicked block state, current brush color, brush size, feather strength, blend mode, step size, opacity, stored face, and canvas texture id.

## Network Module

Internal sync layer:

- `SyncCanvasPacket`: server to client canvas sync.
- `CanvasUploadPacket`: client to server canvas upload/action with stroke ids for history grouping.
- `CanvasBlockReplacePacket`: server to client forced canvas-block replacement sync used by undo restore paths.
- `CanvasStrokeHistory`: server-side global stroke undo/redo history and conflict checks.
- `ItemSyncPacket`: item stack/component sync.
- `CanvasAction`: action enum used by uploads.
- `ClientCanvasCache`: client-side canvas cache.
- `CanvasPistonDataCache`: server-side piston movement cache.

Use this module when changing sync semantics. Downstream mods should prefer public API and event hooks rather than sending Pigmentum packets directly.

## Client Module

Client-only implementation:

- `PaintInputHandler`: continuous painting input and stroke sampling.
- `BrushConfigScreen`: brush settings UI.
- `CanvasTextureManager`: generated texture lifecycle and cache.
- `CanvasBlockEntityRenderer`: in-world canvas face rendering.
- `CanvasRenderEventHandler`: render event integration.
- `CanvasBlockClientExtensions`: client block extensions.
- `ClientPistonCache`: client-side moved canvas cache.
- `PixelQuadBuilder` and `CompositeRenderer`: rendering helpers.

Keep all imports from this module behind client-only code paths.

## Event Module

Event classes:

- `CanvasBlockReplacedEvent`: fired when a block is converted to a canvas proxy.
- `ServerCanvasUpdateEvent`: server-side canvas updates.
- `ClientCanvasTickEvent`: client tick/update point for canvas behavior.
- `ClientCanvasFrameEvent`: client frame/render point for canvas behavior.
- `PaintEvents` and `ModBusEvents`: event registration/handlers.

Use events when extending behavior without coupling to block or network internals.

`ServerCanvasUpdateEvent.Pre` fires before server canvas updates are applied. Undo/redo fires the same server update events for canvas data changes, but restoring the original block from a first-stroke canvas conversion is treated as a block replacement and does not fire `ServerCanvasUpdateEvent`.

## Mixin Module

Mixin targets are implementation details:

- Block/entity storage and block replacement compatibility.
- Block rendering and item-frame rendering hooks.
- Container/menu validation fixes.
- Door behavior fixes.
- Piston movement preservation.
- Level/render update handling. `LevelMixin` delegates canvas `setBlock` decisions to `CanvasBlockSetController`, which keeps normal mimicked-state proxy behavior while allowing scoped internal real block replacement.

Read mixins only when a vanilla behavior differs from expectation or a change requires target method knowledge.

## Utility Module

`CanvasBlacklist` decides whether a block may be converted to a canvas. Inspect it before changing allowed/blocked block behavior.

`CanvasBlockSetController` controls internal `setBlock` behavior for canvas proxies. Use it instead of ad hoc bypasses when Pigmentum itself must force a real `CanvasBlock` replacement, such as undoing the first stroke back to the original block.
