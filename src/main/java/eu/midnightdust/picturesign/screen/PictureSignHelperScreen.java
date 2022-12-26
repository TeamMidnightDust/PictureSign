package eu.midnightdust.picturesign.screen;

import eu.midnightdust.lib.util.MidnightColorUtil;
import eu.midnightdust.lib.util.screen.TexturedOverlayButtonWidget;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.util.PictureSignType;
import eu.midnightdust.picturesign.util.PictureURLUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import java.util.Objects;
import java.util.stream.IntStream;

public class PictureSignHelperScreen extends Screen {
    private static final Identifier TEXTSIGN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/textsign_button.png");
    private static final Identifier CLIPBOARD_ICON_TEXTURE = new Identifier("picturesign","textures/gui/clipboard_button.png");
    private static final Identifier TRASHBIN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/trashbin_button.png");
    private final SignBlockEntity sign;
    private SignBlockEntityRenderer.SignModel model;
    private String[] text;

    public PictureSignHelperScreen(SignBlockEntity sign, boolean filtered) {
        super(Text.translatable("sign.edit"));
        this.text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getTextOnRow(row, filtered)).map(Text::getString).toArray(String[]::new);
        this.sign = sign;
    }
    protected void init() {
        super.init();
        sign.setEditable(false);
        text = IntStream.range(0, 4).mapToObj((row) ->
                sign.getTextOnRow(row, false)).map(Text::getString).toArray(String[]::new);
        if (!text[3].matches("(.*\\d:.*\\d:.*\\d:.*\\d:.*\\d)")) text[3] = "1:1:0:0:0";
        if (!text[0].startsWith("!PS:") && !text[0].startsWith("!VS:") && !text[0].startsWith("!LS:")) text[0] = "!PS:"+text[0];
        for (int i = 0; i < 3; i++) {
            sign.setTextOnRow(i, Text.of(text[i]));
        }
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height / 4 + 120, 200, 20, ScreenTexts.DONE, (button) -> {
            this.finishEditing();
        }));

        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 84, this.height - 40, 20, 20, 0, 0, 20, CLIPBOARD_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = PictureSignClient.clipboard[i];
                    sign.setTextOnRow(i, Text.of(text[i]));
                }
                assert client != null;
                client.setScreen(this);
            }, Text.of("")));
        if (PictureSignConfig.helperUi)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 62, this.height - 40, 20, 20, 0, 0, 20, TRASHBIN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = "";
                    sign.setTextOnRow(i, Text.empty());
                }
                assert client != null;
                client.setScreen(this);
            }, Text.of("")));
        this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 40, this.height - 40, 20, 20, 0, 0, 20, TEXTSIGN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
            sign.setEditable(true);
            Objects.requireNonNull(client).setScreen(new SignEditScreen(this.sign,false));
        }, Text.of("")));
        this.addDrawableChild(new ButtonWidget(this.width / 2,this.height / 5 + 70,40,20, Text.of(text[0].startsWith("!PS:") ? "Image" : (text[0].startsWith("!VS:") ? "Video" : "Loop")), (buttonWidget) -> {
            if (text[0].startsWith("!PS:")) text[0] = "!VS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
            else if (text[0].startsWith("!VS:")) text[0] = "!LS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
            else if (text[0].startsWith("!LS:")) text[0] = "!PS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
            else text[0] = "!PS:" + text[0].replace("!PS:","").replace("!VS:", "").replace("!LS:", "");
            buttonWidget.setMessage(Text.of(text[0].startsWith("!PS:") ? "Image" : (text[0].startsWith("!VS:") ? "Video" : "Loop")));

            sign.setTextOnRow(0, Text.of(text[0]));
        }));
        TextFieldWidget linkWidget = new TextFieldWidget(textRenderer,this.width / 2 - 175,this.height / 5 + 13,215,40, Text.of("url"));
        linkWidget.setMaxLength(900);
        linkWidget.setText(PictureURLUtils.getLink(sign));
        linkWidget.setChangedListener(s -> {
            String prefix = "";
            if (text[0].startsWith("!PS:")) prefix = "!PS:";
            else if (text[0].startsWith("!VS:")) prefix = "!VS:";
            else if (text[0].startsWith("!LS:")) prefix = "!LS:";
            String[] lines = breakLink(prefix, PictureURLUtils.shortenLink(s));
            for (int i = 0; i < 3; i++) {
                text[i] = lines[i];
                sign.setTextOnRow(i, Text.of(text[i]));
            }
        });
        this.addDrawableChild(linkWidget);
        String[] initialDimensions = text[3].split(":");
        TextFieldWidget widthWidget = new TextFieldWidget(textRenderer,this.width / 2 - 175,this.height / 5 + 70,30,20, Text.of("width"));
        widthWidget.setText(initialDimensions[0]);
        widthWidget.setChangedListener(s -> {
            String[] dimensions = new String[5];
            for (int i = 0; i < dimensions.length; ++i){
                if (text[3].split(":").length > i)
                    dimensions[i] = text[3].split(":")[i];
            }
            dimensions[0] = s;
            StringBuilder mergedDimensions = new StringBuilder();
            for (int i = 0; i < 5; ++i) {
                if (dimensions[i] == null) dimensions[i] = "";
                mergedDimensions.append(dimensions[i]);
                if (i < 4)mergedDimensions.append(":");
            }
            text[3] = String.valueOf(mergedDimensions);
            sign.setTextOnRow(3, Text.of(text[3]));
        });
        this.addDrawableChild(widthWidget);
        TextFieldWidget heightWidget = new TextFieldWidget(textRenderer,this.width / 2 - 140,this.height / 5 + 70,30,20, Text.of("height"));
        heightWidget.setText(initialDimensions[1]);
        heightWidget.setChangedListener(s -> {
            String[] dimensions = new String[5];
            for (int i = 0; i < dimensions.length; ++i){
                if (text[3].split(":").length > i)
                    dimensions[i] = text[3].split(":")[i];
            }
            dimensions[1] = s;
            StringBuilder mergedDimensions = new StringBuilder();
            for (int i = 0; i < 5; ++i) {
                if (dimensions[i] == null) dimensions[i] = "";
                mergedDimensions.append(dimensions[i]);
                if (i < 4)mergedDimensions.append(":");
            }
            text[3] = String.valueOf(mergedDimensions);
            sign.setTextOnRow(3, Text.of(text[3]));
        });
        this.addDrawableChild(heightWidget);
        TextFieldWidget posXWidget = new TextFieldWidget(textRenderer,this.width / 2 - 105,this.height / 5 + 70,30,20, Text.of("posX"));
        posXWidget.setText(initialDimensions[2]);
        posXWidget.setChangedListener(s -> {
            String[] dimensions = new String[5];
            for (int i = 0; i < dimensions.length; ++i){
                if (text[3].split(":").length > i)
                    dimensions[i] = text[3].split(":")[i];
            }
            dimensions[2] = s;
            StringBuilder mergedDimensions = new StringBuilder();
            for (int i = 0; i < 5; ++i) {
                if (dimensions[i] == null) dimensions[i] = "";
                mergedDimensions.append(dimensions[i]);
                if (i < 4)mergedDimensions.append(":");
            }
            text[3] = String.valueOf(mergedDimensions);
            sign.setTextOnRow(3, Text.of(text[3]));
        });
        this.addDrawableChild(posXWidget);
        TextFieldWidget posYWidget = new TextFieldWidget(textRenderer,this.width / 2 - 70,this.height / 5 + 70,30,20, Text.of("posY"));
        posYWidget.setText(initialDimensions[3]);
        posYWidget.setChangedListener(s -> {
            String[] dimensions = new String[5];
            for (int i = 0; i < dimensions.length; ++i){
                if (text[3].split(":").length > i)
                    dimensions[i] = text[3].split(":")[i];
            }
            dimensions[3] = s;
            StringBuilder mergedDimensions = new StringBuilder();
            for (int i = 0; i < 5; ++i) {
                if (dimensions[i] == null) dimensions[i] = "";
                mergedDimensions.append(dimensions[i]);
                if (i < 4)mergedDimensions.append(":");
            }
            text[3] = String.valueOf(mergedDimensions);
            sign.setTextOnRow(3, Text.of(text[3]));
        });
        this.addDrawableChild(posYWidget);
        TextFieldWidget posZWidget = new TextFieldWidget(textRenderer,this.width / 2 - 35,this.height / 5 + 70,30,20, Text.of("posZ"));
        posZWidget.setText(initialDimensions[4]);
        posZWidget.setChangedListener(s -> {
            String[] dimensions = new String[5];
            for (int i = 0; i < dimensions.length; ++i){
                if (text[3].split(":").length > i)
                    dimensions[i] = text[3].split(":")[i];
            }
            dimensions[4] = s;
            StringBuilder mergedDimensions = new StringBuilder();
            for (int i = 0; i < 5; ++i) {
                if (dimensions[i] == null) dimensions[i] = "";
                mergedDimensions.append(dimensions[i]);
                if (i < 4)mergedDimensions.append(":");
            }
            text[3] = String.valueOf(mergedDimensions);
            sign.setTextOnRow(3, Text.of(text[3]));
        });
        this.addDrawableChild(posZWidget);
        this.model = SignBlockEntityRenderer.createSignModel(this.client.getEntityModelLoader(), SignBlockEntityRenderer.getSignType(sign.getCachedState().getBlock()));
    }
    public void removed() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
        for (int i = 0; i < 4; i++) {
            sign.setTextOnRow(i, Text.of(text[i]));
        }
        if (clientPlayNetworkHandler != null) {
            clientPlayNetworkHandler.sendPacket(new UpdateSignC2SPacket(this.sign.getPos(), this.text[0], this.text[1], this.text[2], this.text[3]));
        }
    }
    private String[] breakLink(String prefix, String link) {
        Text linkText = Text.of(prefix+link);
        String[] brokenLink = new String[3];
        Text line0Text = linkText;
        int line0width = line0Text.getString().length();
        assert this.client != null;
        while (this.client.textRenderer.getWidth(line0Text) >= 90) {
            --line0width;
            line0Text = Text.of(line0Text.getString().substring(0,line0width));
        }
        brokenLink[0] = line0Text.getString();
        Text line1Text = Text.of(linkText.getString().substring(line0width));
        int line1width = line1Text.getString().length();
        assert this.client != null;
        while (this.client.textRenderer.getWidth(line1Text) >= 90) {
            --line1width;
            line1Text = Text.of(line1Text.getString().substring(0,line1width));
        }
        brokenLink[1] = line1Text.getString();
        Text line2Text = Text.of(linkText.getString().substring(line0width + line1width));
        int line2width = line2Text.getString().length();
        assert this.client != null;
        if (!PictureSignConfig.exceedVanillaLineLength)
            while (this.client.textRenderer.getWidth(line2Text) >= 90) {
                --line2width;
                line2Text = Text.of(line2Text.getString().substring(0,line2width));
            }
        brokenLink[2] = line2Text.getString();

        return brokenLink;
    }
    public void tick() {
        super.tick();
        if (!this.sign.getType().supports(this.sign.getCachedState())) {
            this.finishEditing();
        }
    }
    private void finishEditing() {
        sign.markDirty();
        assert this.client != null;
        this.client.setScreen(null);
    }
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DiffuseLighting.disableGuiDepthLighting();
        this.renderBackground(matrices);
        drawTextWithShadow(matrices,textRenderer, Text.of("Link" + (PictureSignConfig.safeMode ? " (imgur.com/imgbb.com/iili.io/pictshare.net)" : "")),this.width / 2 - 175, this.height / 5 + 3, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("Width"),this.width / 2 - 175, this.height / 5 + 60, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("Height"),this.width / 2 - 140, this.height / 5 + 60, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("PosX"),this.width / 2 - 105, this.height / 5 + 60, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("PosY"),this.width / 2 - 70, this.height / 5 + 60, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("PosZ"),this.width / 2 - 35, this.height / 5 + 60, -8816268);
        drawTextWithShadow(matrices,textRenderer, Text.of("Mode"),this.width / 2, this.height / 5 + 60, -8816268);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 40, 16777215);
        matrices.push();
        matrices.translate(this.width / 2f + 100, this.height / 5f - 60, 50.0);
        matrices.scale(93.75F, -93.75F, 93.75F);
        matrices.translate(0.0, -1.3125, 0.0);
        BlockState blockState = this.sign.getCachedState();
        boolean bl = blockState.getBlock() instanceof SignBlock;
        if (!bl) {
            matrices.translate(0.0, -0.3125, 0.0);
        }
        matrices.push();
        matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
        VertexConsumerProvider.Immediate immediate = this.client.getBufferBuilders().getEntityVertexConsumers();
        SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getSignTextureId(SignBlockEntityRenderer.getSignType(sign.getCachedState().getBlock()));
        SignBlockEntityRenderer.SignModel var10002 = this.model;
        Objects.requireNonNull(var10002);
        VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(immediate, var10002::getLayer);
        this.model.stick.visible = bl;
        this.model.root.render(matrices, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
        matrices.pop();
        matrices.translate(0.0, 0.3333333432674408, 0.046666666865348816);
        matrices.scale(0.010416667F, -0.010416667F, 0.010416667F);
        int i = this.sign.getTextColor().getSignColor();
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
                this.client.textRenderer.draw(string, n, (float)(m * 10 - this.text.length * 5), i, false, matrix4f, immediate, false, 0, 15728880, false);
            }
        }

        immediate.draw();

        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
        super.render(matrices, mouseX, mouseY, delta);
    }
}
