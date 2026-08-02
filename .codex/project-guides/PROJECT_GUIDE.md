# Project Guide

Generated: 2026-07-02 03:06:17Z

<!-- guideweaver:start -->

## Repo Shape

- Files indexed: 126
- Files changed in this refresh: 126
- Git remotes: git@github.com:Steve10086/neoforge_painter.git
- Manifests: build.gradle, settings.gradle
- Top-level source roots: .codex, .github, gradle, src

## File Types

- `.java`: 91
- `.json`: 11
- `.png`: 6
- `.md`: 5
- `(none)`: 3
- `.gradle`: 2
- `.properties`: 2
- `.bat`: 1
- `.jar`: 1
- `.toml`: 1
- `.txt`: 1
- `.yaml`: 1
- `.yml`: 1

## Changed Files

- `.codex/skills/pigmentum-mod-dev/SKILL.md`
- `.codex/skills/pigmentum-mod-dev/agents/openai.yaml`
- `.codex/skills/pigmentum-mod-dev/references/decompile-map.md`
- `.codex/skills/pigmentum-mod-dev/references/modules.md`
- `.gitattributes`
- `.github/workflows/build.yml`
- `.gitignore`
- `API.md`
- `README.md`
- `TEMPLATE_LICENSE.txt`
- `build.gradle`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`
- `settings.gradle`
- `src/main/java/com/astune/painter/CanvasProperties.java`
- `src/main/java/com/astune/painter/Config.java`
- `src/main/java/com/astune/painter/Painter.java`
- `src/main/java/com/astune/painter/PainterClient.java`
- `src/main/java/com/astune/painter/api/BlendMode.java`
- `src/main/java/com/astune/painter/api/CanvasData.java`
- `src/main/java/com/astune/painter/api/CanvasDataHolder.java`
- `src/main/java/com/astune/painter/api/CanvasFace.java`
- `src/main/java/com/astune/painter/api/CompositePainting.java`
- `src/main/java/com/astune/painter/api/ExposurePredicate.java`
- `src/main/java/com/astune/painter/api/IPaintLayer.java`
- `src/main/java/com/astune/painter/api/IPaintProvider.java`
- `src/main/java/com/astune/painter/api/IPixelMatrix.java`
- `src/main/java/com/astune/painter/api/PaintPattern.java`
- `src/main/java/com/astune/painter/api/PaintProviders.java`
- `src/main/java/com/astune/painter/api/PixelMatrix.java`
- `src/main/java/com/astune/painter/api/PixelProvider.java`
- `src/main/java/com/astune/painter/api/ResourcesBundle.java`
- `src/main/java/com/astune/painter/api/blend/BlendContext.java`
- `src/main/java/com/astune/painter/api/blend/BlendFunction.java`
- `src/main/java/com/astune/painter/api/blend/DefaultBlendFunctions.java`
- `src/main/java/com/astune/painter/api/imageProvider/CanvasImageProvider.java`
- `src/main/java/com/astune/painter/api/imageProvider/CanvasImageProviderRegistry.java`
- `src/main/java/com/astune/painter/api/imageProvider/DefaultCanvasImageProvider.java`
- `src/main/java/com/astune/painter/api/imageProvider/ImageProviderContext.java`
- `src/main/java/com/astune/painter/api/render/CanvasPixelRenderer.java`
- `src/main/java/com/astune/painter/api/render/CanvasRendererRegistry.java`
- `src/main/java/com/astune/painter/api/render/DefaultCanvasPixelRenderer.java`
- `src/main/java/com/astune/painter/api/render/RenderContext.java`
- `src/main/java/com/astune/painter/block/CanvasBlock.java`
- `src/main/java/com/astune/painter/block/CanvasBlockEntity.java`
- `src/main/java/com/astune/painter/block/CanvasBlockHelper.java`
- `src/main/java/com/astune/painter/block/CanvasBlockItem.java`
- `src/main/java/com/astune/painter/block/CanvasBlockModel.java`
- `src/main/java/com/astune/painter/block/NoOcclusionCanvasBlock.java`
- `src/main/java/com/astune/painter/block/OcclusionCanvasBlock.java`
- `src/main/java/com/astune/painter/client/CanvasBlockClientExtensions.java`
- `src/main/java/com/astune/painter/client/CanvasBlockEntityRenderer.java`
- `src/main/java/com/astune/painter/client/CanvasRenderEventHandler.java`
- `src/main/java/com/astune/painter/client/CanvasTextureManager.java`
- `src/main/java/com/astune/painter/client/ClientPistonCache.java`
- `src/main/java/com/astune/painter/client/ClientSetup.java`
- `src/main/java/com/astune/painter/client/CompositeRenderer.java`
- `src/main/java/com/astune/painter/client/PaintInputHandler.java`
- `src/main/java/com/astune/painter/client/PixelQuadBuilder.java`
- `src/main/java/com/astune/painter/client/inventory/BrushConfigScreen.java`
- `src/main/java/com/astune/painter/command/PainterCommands.java`
- `src/main/java/com/astune/painter/event/CanvasBlockReplacedEvent.java`
- `src/main/java/com/astune/painter/event/ClientCanvasFrameEvent.java`
- `src/main/java/com/astune/painter/event/ClientCanvasTickEvent.java`
- `src/main/java/com/astune/painter/event/ModBusEvents.java`
- `src/main/java/com/astune/painter/event/PaintEvents.java`
- `src/main/java/com/astune/painter/event/ServerCanvasUpdateEvent.java`
- `src/main/java/com/astune/painter/item/CanvasSheet.java`
- `src/main/java/com/astune/painter/item/DebugPaintbrush.java`
- `src/main/java/com/astune/painter/item/EffectCreator.java`
- `src/main/java/com/astune/painter/item/Paintbrush.java`
- `src/main/java/com/astune/painter/mixin/AbstractContainerMenuMixin.java`
- `src/main/java/com/astune/painter/mixin/BlockEntityMixin.java`
- `src/main/java/com/astune/painter/mixin/BlockEntityRenderersAccessor.java`
- `src/main/java/com/astune/painter/mixin/BlockMixin.java`
- `src/main/java/com/astune/painter/mixin/BlockRenderDispatcherMixin.java`
- `src/main/java/com/astune/painter/mixin/BlockStateBaseMixin.java`

## Dependency Guides

- none

<!-- guideweaver:end -->
