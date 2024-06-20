package eu.midnightdust.picturesign.screen;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HangingSignEditScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static eu.midnightdust.picturesign.PictureSignClient.id;

public class PictureSignHelperScreen extends Screen {
    private static final Identifier TEXTSIGN_ICON_TEXTURE = id("icon/textsign");
    private static final Identifier CLIPBOARD_ICON_TEXTURE = id("icon/clipboard");
    private static final Identifier TRASHBIN_ICON_TEXTURE = id("icon/trashbin");
    private final SignBlockEntity sign;
    private SignBlockEntityRenderer.SignModel model;
    protected String[] text;
    private final boolean front;
    private final boolean isHanging;
    protected final WoodType signType;
    private static boolean switchScreen = false;
    private PictureSignType type = PictureSignType.PICTURE;

    public PictureSignHelperScreen(SignBlockEntity sign, boolean front, boolean filtered) {
        super((sign.getCachedState().getBlock() instanceof HangingSignBlock || sign.getCachedState().getBlock() instanceof WallHangingSignBlock) ? Text.translatable("hanging_sign.edit") : Text.translatable("sign.edit"));
        this.text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getText(front).getMessage(row, filtered)).map(Text::getString).toArray(String[]::new);
        this.sign = sign;
        this.signType = AbstractSignBlock.getWoodType(sign.getCachedState().getBlock());
        this.isHanging = sign.getCachedState().getBlock() instanceof HangingSignBlock || sign.getCachedState().getBlock() instanceof WallHangingSignBlock;

        this.front = front;
    }
    protected void init() {
        super.init();
        if (this.client == null) return;
        text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getText(front).getMessage(row, false)).map(Text::getString).toArray(String[]::new);
        if (!text[3].matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) text[3] = "1:1:0:0:0";
        if (!text[0].startsWith("!")) text[0] = PictureSignType.PICTURE.format+text[0];
        if (text[2].isBlank() && PictureSignConfig.exceedVanillaLineLength) text[2] = "0:0:0";
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            sign.changeText(changer -> changer.withMessage(finalI, Text.of(text[finalI])), front);
        }
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.finishEditing()).dimensions(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build());

        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null) {
            TextIconButtonWidget clipboardBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = PictureSignClient.clipboard[i];
                    int finalI = i;
                    sign.changeText(changer -> changer.withMessage(finalI, Text.of(text[finalI])), front);
                }
                assert client != null;
                client.setScreen(this);
            }, true).texture(CLIPBOARD_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            clipboardBuilder.setPosition(this.width - 84, this.height - 40);
            this.addDrawableChild(clipboardBuilder);
        }
        if (PictureSignConfig.helperUi) {
            TextIconButtonWidget trashbinBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = "";
                    int finalI = i;
                    sign.changeText(changer -> changer.withMessage(finalI, Text.empty()), front);
                }
                assert client != null;
                client.setScreen(this);
            }, true).texture(TRASHBIN_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            trashbinBuilder.setPosition(this.width - 62, this.height - 40);
            this.addDrawableChild(trashbinBuilder);

            TextIconButtonWidget textsignBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                switchScreen = true;
                Objects.requireNonNull(client).setScreen(isHanging ? new HangingSignEditScreen(this.sign, false, front) : new SignEditScreen(this.sign, front, false));
            }, true).texture(TEXTSIGN_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            textsignBuilder.setPosition(this.width - 40, this.height - 40);
            this.addDrawableChild(textsignBuilder);
        }
        type = PictureSignType.getType(text[0]);
        this.addDrawableChild(ButtonWidget.builder(type.name, (buttonWidget) -> {
            text[0] = text[0].replace(type.format, "");
            type = type.next();
            text[0] = type.format + text[0];
//            if (text[0].startsWith("!PS:")) text[0] = "!VS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
//            else if (text[0].startsWith("!VS:")) text[0] = "!LS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
//            else if (text[0].startsWith("!LS:")) text[0] = "!PS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
//            else text[0] = "!PS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
//            type = PictureSignType.getType(text[0]);
            buttonWidget.setMessage(type.name);

            sign.changeText(changer -> changer.withMessage(0, Text.of(text[0])), front);
        }).dimensions(this.width / 2,this.height / 5 + 70,40,20).build());
        TextFieldWidget linkWidget = new TextFieldWidget(textRenderer,this.width / 2 - 175,this.height / 5 + 13,215,40, Text.of("url"));
        linkWidget.setMaxLength(900);
        linkWidget.setText(PictureURLUtils.getLink(sign, front));
        linkWidget.setChangedListener(s -> {
            String[] lines = breakLink(type.format, PictureURLUtils.shortenLink(s));
            for (int i = 0; i < (PictureSignConfig.exceedVanillaLineLength ? 2 : 3); i++) {
                text[i] = lines[i];
                int finalI = i;
                sign.changeText(changer -> changer.withMessage(finalI, Text.of(text[finalI])), front);
            }
        });
        this.addDrawableChild(linkWidget);
        String[] initialDimensions = text[3].split(":");
        TextFieldWidget widthWidget = new TextFieldWidget(textRenderer,this.width / 2 - 175,this.height / 5 + 70,30,20, Text.of("width"));
        TextFieldWidget heightWidget = new TextFieldWidget(textRenderer,this.width / 2 - 140,this.height / 5 + 70,30,20, Text.of("height"));
        TextFieldWidget posXWidget = new TextFieldWidget(textRenderer,this.width / 2 - 105,this.height / 5 + 70,30,20, Text.of("posX"));
        TextFieldWidget posYWidget = new TextFieldWidget(textRenderer,this.width / 2 - 70,this.height / 5 + 70,30,20, Text.of("posY"));
        TextFieldWidget posZWidget = new TextFieldWidget(textRenderer,this.width / 2 - 35,this.height / 5 + 70,30,20, Text.of("posZ"));
        widthWidget.setText(initialDimensions[0]);
        heightWidget.setText(initialDimensions[1]);
        posXWidget.setText(initialDimensions[2]);
        posYWidget.setText(initialDimensions[3]);
        posZWidget.setText(initialDimensions[4]);
        widthWidget.setChangedListener(s -> applyPosition(s, 0));
        heightWidget.setChangedListener(s -> applyPosition(s, 1));
        posXWidget.setChangedListener(s -> applyPosition(s, 2));
        posYWidget.setChangedListener(s -> applyPosition(s, 3));
        posZWidget.setChangedListener(s -> applyPosition(s, 4));
        this.addDrawableChild(widthWidget);
        this.addDrawableChild(heightWidget);
        this.addDrawableChild(posXWidget);
        this.addDrawableChild(posYWidget);
        this.addDrawableChild(posZWidget);
        if (text[2].matches("(.*\\d:.*\\d:.*\\d)")) addRotationWidgets();
        this.model = SignBlockEntityRenderer.createSignModel(this.client.getEntityModelLoader(), AbstractSignBlock.getWoodType(sign.getCachedState().getBlock()));
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
        sign.changeText(changer -> changer.withMessage(3, Text.of(text[3])), front);
    }
    public void addRotationWidgets() {
        String[] initialRotation = text[2].split(":");
        RotationSliderWidget rotXWidget = new RotationSliderWidget(this.width / 2 - 176,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[0]));
        RotationSliderWidget rotYWidget = new RotationSliderWidget(this.width / 2 - 103,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[1]));
        RotationSliderWidget rotZWidget = new RotationSliderWidget(this.width / 2 - 30,this.height / 5 + 100,70,20, Integer.parseInt(initialRotation[2]));
        rotXWidget.setChangedListener(s -> applyRotation(s, 0));
        rotYWidget.setChangedListener(s -> applyRotation(s, 1));
        rotZWidget.setChangedListener(s -> applyRotation(s, 2));
        this.addDrawableChild(rotXWidget);
        this.addDrawableChild(rotYWidget);
        this.addDrawableChild(rotZWidget);
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
        sign.changeText(changer -> changer.withMessage(2, Text.of(text[2])), front);
    }
    public static class RotationSliderWidget extends SliderWidget {
        private Consumer<Integer> changedListener;

        public RotationSliderWidget(int x, int y, int width, int height, int rot) {
            super(x, y, width, height, Text.of(String.valueOf(rot)), rot / (360d));
        }

        protected void updateMessage() {
            this.setMessage(Text.of(String.valueOf(getValue())));
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
        assert this.client != null;
        switchScreen = false;
        this.client.setScreen(null);
    }
    public void removed() {
        if (this.client == null || switchScreen) return;
        ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            sign.changeText(changer -> changer.withMessage(finalI, Text.of(text[finalI])), front);
        }
        if (clientPlayNetworkHandler != null) {
            clientPlayNetworkHandler.sendPacket(new UpdateSignC2SPacket(this.sign.getPos(), front, this.text[0], this.text[1], this.text[2], this.text[3]));
        }
    }

    private String[] breakLink(String prefix, String link) {
        Text linkText = Text.of(prefix+link);
        String[] brokenLink = new String[3];
        assert client != null;
        List<OrderedText> text = client.textRenderer.wrapLines(linkText, 90);
        for (int i = 0; i < text.size(); i++) {
            String textLine = orderedToString(text.get(i));
            if (i < (PictureSignConfig.exceedVanillaLineLength ? 2 : 3))
                brokenLink[i] = textLine;
            else if (PictureSignConfig.exceedVanillaLineLength) brokenLink[1] += textLine;
        }

        return brokenLink;
    }
    private String orderedToString(OrderedText ordered) {
        StringBuilder string = new StringBuilder();
        ordered.accept((i, style, codePoint) -> {
            string.append(Character.toString(codePoint));
            return true;
        }); return string.toString();
    }
    @Override
    public boolean shouldPause() {
        return false;
    }
    public void tick() {
        super.tick();
        if (!this.sign.getType().supports(this.sign.getCachedState())) {
            this.finishEditing();
        }
    }
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.client == null) return;
        DiffuseLighting.disableGuiDepthLighting();
        context.drawTextWithShadow(textRenderer, Text.of("Link" +
                (PictureSignConfig.safeMode ? (type.equals(PictureSignType.PICTURE) ? " (imgur.com/imgbb.com/iili.io/pictshare.net)" : " (youtube.com/youtu.be/vimeo.com)") : "")),
                this.width / 2 - 175, this.height / 5 + 3, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("Width"),this.width / 2 - 175, this.height / 5 + 60, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("Height"),this.width / 2 - 140, this.height / 5 + 60, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("PosX"),this.width / 2 - 105, this.height / 5 + 60, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("PosY"),this.width / 2 - 70, this.height / 5 + 60, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("PosZ"),this.width / 2 - 35, this.height / 5 + 60, -8816268);
        context.drawTextWithShadow(textRenderer, Text.of("Mode"),this.width / 2, this.height / 5 + 60, -8816268);
        if (text[2].matches("(.*\\d:.*\\d:.*\\d)")) {
            context.drawTextWithShadow(textRenderer, Text.of("RotX"),this.width / 2 - 175, this.height / 5 + 92, -8816268);
            context.drawTextWithShadow(textRenderer, Text.of("RotY"),this.width / 2 - 103, this.height / 5 + 92, -8816268);
            context.drawTextWithShadow(textRenderer, Text.of("RotZ"),this.width / 2 - 30, this.height / 5 + 92, -8816268);
        }
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 40, 16777215);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
        translateForRender(context, sign.getCachedState());
        renderSignBackground(context, sign.getCachedState());

        int i = this.sign.getText(front).getColor().getSignColor();
        matrices.pop();
        matrices.push();

        context.getMatrices().translate((float)this.width / 2.0F + 100, this.height / 5f + 47.5f, 400.0F);
        if (sign.getCachedState().getBlock() instanceof SignBlock) context.getMatrices().translate(0,-15f,0);
        else if (isHanging) context.getMatrices().translate(0,17f,0);
        Vector3f vector3f = this.getTextScale();
        context.getMatrices().scale(vector3f.x(), vector3f.y(), vector3f.z());
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        int m;
        String string;
        for(m = 0; m < this.text.length; ++m) {
            string = this.text[m];
            if (string != null) {
                if (this.textRenderer.isRightToLeft()) {
                    string = this.textRenderer.mirror(string);
                }

                float n = (float)(-this.client.textRenderer.getWidth(string) / 2);
                this.client.textRenderer.draw(string, n, (float)(m * 10 - this.text.length * 5), i, false, matrix4f, immediate, TextRenderer.TextLayerType.NORMAL, 0, 15728880, false);
            }
        }

        immediate.draw();

        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }
    protected void translateForRender(DrawContext context, BlockState state) {
        MatrixStack matrices = context.getMatrices();
        if (isHanging) {
            matrices.translate((float)this.width / 2.0F + 100, this.height / 5f + 50, 50.0F);
        }
        else {
            matrices.push();
            matrices.translate(this.width / 2f + 100, this.height / 5f - 60, 50.0);
            matrices.scale(93.75F, -93.75F, 93.75F);
            matrices.translate(0.0, -1.3125, 0.0);
        }
    }

    protected void renderSignBackground(DrawContext context, BlockState state) {
        if (!isHanging) {
            VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
            MatrixStack matrices = context.getMatrices();

            BlockState blockState = this.sign.getCachedState();
            boolean bl = blockState.getBlock() instanceof SignBlock;
            if (!bl) {
                matrices.translate(0.0, -0.15625, 0.0);
            }
            matrices.push();
            matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);

            SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getSignTextureId(AbstractSignBlock.getWoodType(sign.getCachedState().getBlock()));
            SignBlockEntityRenderer.SignModel var10002 = this.model;
            Objects.requireNonNull(var10002);
            VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(immediate, var10002::getLayer);
            this.model.stick.visible = bl;
            this.model.root.render(matrices, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
            matrices.pop();
            matrices.translate(0.0, 0.3333333432674408, 0.046666666865348816);
            matrices.scale(0.010416667F, -0.010416667F, 0.010416667F);
        }
        else {
            MatrixStack matrices = context.getMatrices();
            matrices.scale(4.5F, 4.5F, 1.0F);
            context.drawTexture(Identifier.ofVanilla("textures/gui/hanging_signs/" + this.signType.name() + ".png"), -8, -8, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    protected Vector3f getTextScale() {
        return isHanging ? new Vector3f(1.0F, 1.0F, 1.0F) : new Vector3f(0.9765628F, 0.9765628F, 0.9765628F);
    }
}
