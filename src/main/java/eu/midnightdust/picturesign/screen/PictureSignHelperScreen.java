package eu.midnightdust.picturesign.screen;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.config.PictureSignSecrets;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static eu.midnightdust.picturesign.PictureSignClient.id;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class PictureSignHelperScreen extends Screen {
    private static final ResourceLocation TEXTSIGN_ICON_TEXTURE = id("icon/textsign");
    private static final ResourceLocation CLIPBOARD_ICON_TEXTURE = id("icon/clipboard");
    private static final ResourceLocation TRASHBIN_ICON_TEXTURE = id("icon/trashbin");
    private final SignBlockEntity sign;
    private Model model;
    protected String[] text;
    private final boolean front;
    private final boolean isHanging;
    protected final WoodType signType;
    private static boolean switchScreen = false;
    private final List<AbstractWidget> pictureWidgets = new ArrayList<>();
    private PictureSignType type = PictureSignType.PICTURE;

    public PictureSignHelperScreen(SignBlockEntity sign, boolean front, boolean filtered) {
        super((sign.getBlockState().getBlock() instanceof CeilingHangingSignBlock || sign.getBlockState().getBlock() instanceof WallHangingSignBlock) ? Component.translatable("hanging_sign.edit") : Component.translatable("sign.edit"));
        this.text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getText(front).getMessage(row, filtered)).map(Component::getString).toArray(String[]::new);
        this.sign = sign;
        this.signType = SignBlock.getWoodType(sign.getBlockState().getBlock());
        this.isHanging = sign.getBlockState().getBlock() instanceof CeilingHangingSignBlock || sign.getBlockState().getBlock() instanceof WallHangingSignBlock;

        this.front = front;
    }
    protected void init() {
        super.init();
        if (this.minecraft == null) return;
        text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getText(front).getMessage(row, false)).map(Component::getString).toArray(String[]::new);
        if (!text[3].matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) text[3] = "1:1:0:0:0";
        if (!text[0].startsWith("!")) text[0] = PictureSignType.PICTURE.format+text[0];
        if (text[2].isBlank() && PictureSignConfig.exceedVanillaLineLength) text[2] = "0:0:0";
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            sign.updateText(changer -> changer.setMessage(finalI, Component.nullToEmpty(text[finalI])), front);
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> this.finishEditing()).bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build());

        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null) {
            SpriteIconButton clipboardBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = PictureSignClient.clipboard[i];
                    int finalI = i;
                    sign.updateText(changer -> changer.setMessage(finalI, Component.nullToEmpty(text[finalI])), front);
                }
                assert minecraft != null;
                minecraft.setScreen(this);
            }, true).sprite(CLIPBOARD_ICON_TEXTURE, 16, 16).size(20, 20).build();
            clipboardBuilder.setPosition(this.width - 84, this.height - 40);
            this.addRenderableWidget(clipboardBuilder);
        }
        if (PictureSignConfig.helperUi) {
            SpriteIconButton trashbinBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = "";
                    int finalI = i;
                    sign.updateText(changer -> changer.setMessage(finalI, Component.empty()), front);
                }
                assert minecraft != null;
                minecraft.setScreen(this);
            }, true).sprite(TRASHBIN_ICON_TEXTURE, 16, 16).size(20, 20).build();
            trashbinBuilder.setPosition(this.width - 62, this.height - 40);
            this.addRenderableWidget(trashbinBuilder);

            SpriteIconButton textsignBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                switchScreen = true;
                Objects.requireNonNull(minecraft).setScreen(isHanging ? new HangingSignEditScreen(this.sign, false, front) : new SignEditScreen(this.sign, front, false));
            }, true).sprite(TEXTSIGN_ICON_TEXTURE, 16, 16).size(20, 20).build();
            textsignBuilder.setPosition(this.width - 40, this.height - 40);
            this.addRenderableWidget(textsignBuilder);
        }
        type = PictureSignType.getType(text[0]);
        this.addRenderableWidget(Button.builder(type.name, (buttonWidget) -> {
            text[0] = text[0].replace(type.format, "");
            type = type.next();
            text[0] = type.format + text[0];
            buttonWidget.setMessage(type.name);

            sign.updateText(changer -> changer.setMessage(0, Component.nullToEmpty(text[0])), front);
        }).bounds(this.width / 2,this.height / 5 + 70,40,20).build());
        EditBox linkWidget = new EditBox(font,this.width / 2 - 175,this.height / 5 + 13,215,40, Component.nullToEmpty("url"));
        linkWidget.setMaxLength(900);
        linkWidget.setValue(PictureURLUtils.getLink(sign, front));
        linkWidget.setResponder(s -> {
            String[] lines = breakLink(type.format, PictureURLUtils.shortenLink(PictureSignSecrets.redactSecret(s)));
            for (int i = 0; i < (PictureSignConfig.exceedVanillaLineLength ? 2 : 3); i++) {
                text[i] = lines[i];
                int finalI = i;
                sign.updateText(changer -> changer.setMessage(finalI, Component.nullToEmpty(text[finalI])), front);
            }
        });
        this.addRenderableWidget(linkWidget);
        String[] initialDimensions = text[3].split(":");
        EditBox widthWidget = new EditBox(font,this.width / 2 - 175,this.height / 5 + 70,30,20, Component.nullToEmpty("width"));
        EditBox heightWidget = new EditBox(font,this.width / 2 - 140,this.height / 5 + 70,30,20, Component.nullToEmpty("height"));
        EditBox posXWidget = new EditBox(font,this.width / 2 - 105,this.height / 5 + 70,30,20, Component.nullToEmpty("posX"));
        EditBox posYWidget = new EditBox(font,this.width / 2 - 70,this.height / 5 + 70,30,20, Component.nullToEmpty("posY"));
        EditBox posZWidget = new EditBox(font,this.width / 2 - 35,this.height / 5 + 70,30,20, Component.nullToEmpty("posZ"));
        widthWidget.setValue(initialDimensions[0]);
        heightWidget.setValue(initialDimensions[1]);
        posXWidget.setValue(initialDimensions[2]);
        posYWidget.setValue(initialDimensions[3]);
        posZWidget.setValue(initialDimensions[4]);
        widthWidget.setResponder(s -> applyPosition(s, 0));
        heightWidget.setResponder(s -> applyPosition(s, 1));
        posXWidget.setResponder(s -> applyPosition(s, 2));
        posYWidget.setResponder(s -> applyPosition(s, 3));
        posZWidget.setResponder(s -> applyPosition(s, 4));
        this.addRenderableWidget(widthWidget);
        this.addRenderableWidget(heightWidget);
        this.addRenderableWidget(posXWidget);
        this.addRenderableWidget(posYWidget);
        this.addRenderableWidget(posZWidget);

        pictureWidgets.add(widthWidget);
        pictureWidgets.add(heightWidget);
        pictureWidgets.add(posXWidget);
        pictureWidgets.add(posYWidget);
        pictureWidgets.add(posZWidget);
        if (text[2].matches("(.*\\d:.*\\d:.*\\d)")) addRotationWidgets();
        this.model = SignRenderer.createSignModel(this.minecraft.getEntityModels(), this.signType, !(sign.getBlockState().getBlock() instanceof WallSignBlock));
    }
    public void applyPosition(String position, int index) {
        String[] dimensions = new String[5];
        for (int i = 0; i < dimensions.length; ++i){
            if (text[3].split(":").length > i)
                dimensions[i] = text[3].split(":")[i];
        }
        dimensions[index] = position;
        StringBuilder mergedDimensions = new StringBuilder();
        for (int i = 0; i < 5; ++i) {
            if (dimensions[i] == null) dimensions[i] = "";
            mergedDimensions.append(dimensions[i]);
            if (i < 4)mergedDimensions.append(":");
        }
        text[3] = String.valueOf(mergedDimensions);
        sign.updateText(changer -> changer.setMessage(3, Component.nullToEmpty(text[3])), front);
    }
    public void addRotationWidgets() {
        String[] initialRotation = text[2].split(":");
        RotationSliderWidget rotXWidget = new RotationSliderWidget(this.width / 2 - 176,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[0]));
        RotationSliderWidget rotYWidget = new RotationSliderWidget(this.width / 2 - 103,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[1]));
        RotationSliderWidget rotZWidget = new RotationSliderWidget(this.width / 2 - 30,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[2]));
        rotXWidget.setChangedListener(s -> applyRotation(s, 0));
        rotYWidget.setChangedListener(s -> applyRotation(s, 1));
        rotZWidget.setChangedListener(s -> applyRotation(s, 2));
        this.addRenderableWidget(rotXWidget);
        this.addRenderableWidget(rotYWidget);
        this.addRenderableWidget(rotZWidget);

        pictureWidgets.add(rotXWidget);
        pictureWidgets.add(rotYWidget);
        pictureWidgets.add(rotZWidget);
    }
    public void applyRotation(int rotation, int index) {
        String[] dimensions = new String[3];
        for (int i = 0; i < dimensions.length; ++i){
            if (text[2].split(":").length > i)
                dimensions[i] = text[2].split(":")[i];
        }
        dimensions[index] = String.valueOf(rotation);
        StringBuilder mergedDimensions = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            if (dimensions[i] == null) dimensions[i] = "";
            mergedDimensions.append(dimensions[i]);
            if (i < 2)mergedDimensions.append(":");
        }
        text[2] = String.valueOf(mergedDimensions);
        sign.updateText(changer -> changer.setMessage(2, Component.nullToEmpty(text[2])), front);
    }
    public static class RotationSliderWidget extends AbstractSliderButton {
        private Consumer<Integer> changedListener;

        public RotationSliderWidget(int x, int y, int width, int height, int rot) {
            super(x, y, width, height, Component.nullToEmpty(String.valueOf(rot)), rot / (360d));
        }

        protected void updateMessage() {
            this.setMessage(Component.nullToEmpty(String.valueOf(getValue())));
        }

        @Override
        protected void applyValue() {
            changedListener.accept(getValue());
        }

        protected int getValue() {
            return Double.valueOf(this.value * (360)).intValue();
        }
        public void setChangedListener(Consumer<Integer> changedListener) {
            this.changedListener = changedListener;
        }
    }
    private void finishEditing() {
        assert this.minecraft != null;
        switchScreen = false;
        this.minecraft.setScreen(null);
    }
    public void removed() {
        if (this.minecraft == null || switchScreen) return;
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            sign.updateText(changer -> changer.setMessage(finalI, Component.nullToEmpty(text[finalI])), front);
        }
        if (Minecraft.getInstance().getConnection() != null)
            Minecraft.getInstance().getConnection().getConnection().send(new ServerboundSignUpdatePacket(this.sign.getBlockPos(), front, this.text[0], this.text[1], this.text[2], this.text[3]));
    }

    private String[] breakLink(String prefix, String link) {
        Component linkText = Component.nullToEmpty(prefix+link);
        String[] brokenLink = new String[3];
        assert minecraft != null;
        List<FormattedCharSequence> text = minecraft.font.split(linkText, 90);
        for (int i = 0; i < text.size(); i++) {
            String textLine = orderedToString(text.get(i));
            if (i < (PictureSignConfig.exceedVanillaLineLength ? 2 : 3))
                brokenLink[i] = textLine;
            else if (PictureSignConfig.exceedVanillaLineLength) brokenLink[1] += textLine;
        }

        return brokenLink;
    }
    private String orderedToString(FormattedCharSequence ordered) {
        StringBuilder string = new StringBuilder();
        ordered.accept((i, style, codePoint) -> {
            string.append(Character.toString(codePoint));
            return true;
        }); return string.toString();
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    public void tick() {
        super.tick();
        if (!this.sign.getType().isValid(this.sign.getBlockState())) {
            this.finishEditing();
        }
    }
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.minecraft == null) return;
        Lighting.setupForFlatItems();
        String enabledWebsites = "";
        if (PictureSignConfig.safeMode) {
            if (type.equals(PictureSignType.PICTURE)) enabledWebsites = "(imgur.com/imgbb.com/iili.io/pictshare.net)";
            else if (type.equals(PictureSignType.GIF)) enabledWebsites = "(imgur.com/tenor.com)";
            else if (type.isVideo) enabledWebsites = "(youtube.com/youtu.be/vimeo.com)";
            else if (type.isAudio) enabledWebsites = "(youtube.com/youtu.be/vimeo.com/freesound.org)";
        }
        if (type.isAudio) pictureWidgets.forEach(widget -> widget.active = false);
        else pictureWidgets.forEach(widget -> widget.active = true);
        context.drawString(font, Component.translatable("picturesign.helper.link", enabledWebsites),
                this.width / 2 - 175, this.height / 5 + 3, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.width"),this.width / 2 - 175, this.height / 5 + 60, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.height"),this.width / 2 - 140, this.height / 5 + 60, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.pos_x"),this.width / 2 - 105, this.height / 5 + 60, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.pos_y"),this.width / 2 - 70, this.height / 5 + 60, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.pos_z"),this.width / 2 - 35, this.height / 5 + 60, -8816268);
        context.drawString(font, Component.translatable("picturesign.helper.mode"),this.width / 2, this.height / 5 + 60, -8816268);
        if (text[2].matches("(.*\\d:.*\\d:.*\\d)")) {
            context.drawString(font, Component.translatable("picturesign.helper.rot_x"),this.width / 2 - 175, this.height / 5 + 92, -8816268);
            context.drawString(font, Component.translatable("picturesign.helper.rot_y"),this.width / 2 - 103, this.height / 5 + 92, -8816268);
            context.drawString(font, Component.translatable("picturesign.helper.rot_z"),this.width / 2 - 30, this.height / 5 + 92, -8816268);
        }
        context.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
        PoseStack matrices = context.pose();
        matrices.pushPose();
        MultiBufferSource.BufferSource immediate = this.minecraft.renderBuffers().bufferSource();
        translateForRender(context);
        renderSignBackground(context, sign.getBlockState());

        int i = this.sign.getText(front).getColor().getTextColor();
        matrices.popPose();
        matrices.pushPose();

        context.pose().translate((float)this.width / 2.0F + 100, this.height / 5f + 47.5f, 400.0F);
        if (sign.getBlockState().getBlock() instanceof StandingSignBlock) context.pose().translate(0,-15f,0);
        else if (isHanging) context.pose().translate(0,17f,0);
        Vector3f vector3f = this.getTextScale();
        context.pose().scale(vector3f.x(), vector3f.y(), vector3f.z());
        Matrix4f matrix4f = matrices.last().pose();

        int m;
        String string;
        for(m = 0; m < this.text.length; ++m) {
            string = this.text[m];
            if (string != null) {
                if (this.font.isBidirectional()) {
                    string = this.font.bidirectionalShaping(string);
                }

                float n = (float)(-this.minecraft.font.width(string) / 2);
                this.minecraft.font.drawInBatch(Component.nullToEmpty(string), n, (float)(m * 10 - this.text.length * 5), i, false, matrix4f, immediate, Font.DisplayMode.NORMAL, 0, 15728880, false);
            }
        }

        immediate.endBatch();

        matrices.popPose();
        Lighting.setupFor3DItems();
    }
    protected void translateForRender(GuiGraphics context) {
        PoseStack matrices = context.pose();
        if (isHanging) {
            matrices.translate((float)this.width / 2.0F + 100, this.height / 5f + 50, 50.0F);
        }
        else {
            matrices.pushPose();
            matrices.translate(this.width / 2f + 100, this.height / 5f - 60, 50.0);
            matrices.scale(93.75F, -93.75F, 93.75F);
            matrices.translate(0.0, -1.3125, 0.0);
        }
    }

    protected void renderSignBackground(GuiGraphics context, BlockState state) {
        if (!isHanging && this.minecraft != null) {
            MultiBufferSource.BufferSource immediate = this.minecraft.renderBuffers().bufferSource();
            PoseStack matrices = context.pose();

            boolean bl = state.getBlock() instanceof StandingSignBlock;
            if (!bl) {
                matrices.translate(0.0, -0.15625, 0.0);
            }
            matrices.pushPose();
            matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);

            Material spriteResourceLocation = Sheets.getSignMaterial(SignBlock.getWoodType(state.getBlock()));
            Objects.requireNonNull(model);
            VertexConsumer vertexConsumer = spriteResourceLocation.buffer(immediate, model::renderType);
            this.model.renderToBuffer(matrices, vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY);
            matrices.popPose();
            matrices.translate(0.0, 0.3333333432674408, 0.046666666865348816);
            matrices.scale(0.010416667F, -0.010416667F, 0.010416667F);
        }
        else {
            PoseStack matrices = context.pose();
            matrices.scale(4.5F, 4.5F, 1.0F);
            context.blit(RenderType::guiTextured, ResourceLocation.withDefaultNamespace("textures/gui/hanging_signs/" + this.signType.name() + ".png"), -8, -8, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    protected Vector3f getTextScale() {
        return isHanging ? new Vector3f(1.0F, 1.0F, 1.0F) : new Vector3f(0.9765628F, 0.9765628F, 0.9765628F);
    }
}
