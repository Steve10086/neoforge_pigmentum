# Minimal Decompile Map

Use this guide to decide the smallest Pigmentum code surface to inspect in a consuming NeoForge mod environment.

## Zero-Decompile Path

For most third-party integrations, do not decompile Pigmentum. Use:

- `API.md`
- `com.astune.painter.api.IPaintProvider`
- `com.astune.painter.api.PaintProviders`
- `com.astune.painter.api.PaintPattern`
- `com.astune.painter.api.PixelProvider`
- `com.astune.painter.api.BlendMode`
- `com.astune.painter.api.blend.BlendFunction`
- `com.astune.painter.api.blend.BlendContext`
- `com.astune.painter.api.imageProvider.CanvasImageProvider`
- `com.astune.painter.api.imageProvider.CanvasImageProviderRegistry`
- `com.astune.painter.api.render.CanvasPixelRenderer`
- `com.astune.painter.api.render.CanvasRendererRegistry`
- `com.astune.painter.api.CanvasData`
- `com.astune.painter.api.CanvasFace`

This is enough for custom brushes, patterns, effect layers, image generation, and renderer registration.

## One-Class Inspection

Inspect only the listed class when the task asks about a specific behavior:

| Task | Inspect |
| --- | --- |
| How a brush is registered or detected | `api/PaintProviders.java` |
| Simple brush implementation example | `item/DebugPaintbrush.java` or `item/Paintbrush.java` |
| Effect-writing brush example | `item/EffectCreator.java` |
| Canvas item extraction/display data | `item/CanvasSheet.java`, `registry/ModDataComponents.java` |
| Canvas face geometry from hit location | `api/CanvasData.java`, `api/CanvasFace.java` |
| Pixel storage and serialization | `api/PixelMatrix.java`, then `api/CanvasFace.java` |
| Blend behavior | `api/blend/DefaultBlendFunctions.java`, then `api/blend/BlendContext.java` |
| Default generated texture | `api/imageProvider/DefaultCanvasImageProvider.java` |
| Default face renderer | `api/render/DefaultCanvasPixelRenderer.java` |

## Small Internal Sets

Only inspect these sets when modifying Pigmentum itself or debugging behavior not exposed through API.

### Block Replacement and Canvas Storage

- `block/CanvasBlock.java`
- `block/CanvasBlockEntity.java`
- `block/CanvasBlockHelper.java`
- `block/CanvasBlockModel.java`
- `api/CanvasData.java`
- `api/CanvasDataHolder.java`
- `mixin/BlockEntityMixin.java`
- `mixin/LevelMixin.java`

Use for questions about turning arbitrary blocks into canvases, storing mimicked block state, preserving block entity canvas data, or delegating block shape/render behavior.

### Client Painting Input

- `client/PaintInputHandler.java`
- `network/CanvasUploadPacket.java`
- `network/ItemSyncPacket.java`
- `api/IPaintProvider.java`

Use for continuous painting, stroke interpolation, brush intervals, client-to-server canvas uploads, and syncing item component changes.

### Rendering and Texture Lifecycle

- `client/CanvasBlockEntityRenderer.java`
- `client/CanvasTextureManager.java`
- `client/CanvasRenderEventHandler.java`
- `api/imageProvider/*`
- `api/render/*`
- `mixin/BlockRenderDispatcherMixin.java`
- `mixin/ForgeHooksClientMixin.java`
- `mixin/ItemFrameRendererMixin.java`

Use for dynamic textures, item-frame canvas display, custom geometry, render layer choices, or renderer replacement.

### Networking

- `Painter.java`
- `network/SyncCanvasPacket.java`
- `network/CanvasUploadPacket.java`
- `network/ItemSyncPacket.java`
- `network/CanvasAction.java`
- `network/ClientCanvasCache.java`

Use for packet registration, server/client sync direction, canvas creation/update actions, and client cache invalidation.

### Pistons and Movement

- `network/CanvasPistonDataCache.java`
- `client/ClientPistonCache.java`
- `mixin/PistonStructureResolverMixin.java`
- `mixin/PistonMovingBlockEntityMixin.java`
- `mixin/PistonBaseBlockMixin.java`

Use only for piston movement and canvas data preservation during moving block entity lifecycles.

### Vanilla Compatibility Mixins

- `mixin/AbstractContainerMenuMixin.java`
- `mixin/ItemCombinerMenuMixin.java`
- `mixin/DoorBlockMixin.java`
- `mixin/BlockMixin.java`
- `mixin/BlockStateBaseMixin.java`
- `mixin/LevelAccessorMixin.java`
- `mixin/LevelRendererMixin.java`

Use when a vanilla interaction fails against canvas-proxy blocks, such as smithing/anvil validation, double-door updates, state shape checks, or neighbor/render updates.

## Decompile Discipline

- Start with interfaces and registries, then inspect the one default implementation closest to the desired behavior.
- Prefer method signatures and call sites over full class reading.
- Search exact names first: `IPaintProvider`, `CanvasFace`, `CanvasUploadPacket`, `CanvasRendererRegistry`, `CanvasImageProviderRegistry`.
- Decompile Minecraft/NeoForge classes only after Pigmentum source shows an overridden vanilla contract or mixin target that cannot be understood from mappings.
- Keep any downstream code compiled against public API unless the user explicitly accepts internal compatibility risk.
