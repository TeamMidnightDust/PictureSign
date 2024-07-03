package eu.midnightdust.picturesign.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class TextIconButtonWidget extends ButtonWidget {
    protected final Identifier texture;
    protected final int textureWidth;
    protected final int textureHeight;

    TextIconButtonWidget(int width, int height, Text message, int textureWidth, int textureHeight, Identifier texture, ButtonWidget.PressAction onPress, @Nullable ButtonWidget.NarrationSupplier narrationSupplier) {
        super(0, 0, width, height, message, onPress, narrationSupplier == null ? DEFAULT_NARRATION_SUPPLIER : narrationSupplier);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.texture = texture;
    }

    public static Builder builder(Text text, ButtonWidget.PressAction onPress, boolean hideLabel) {
        return new Builder(text, onPress, hideLabel);
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Text text;
        private final ButtonWidget.PressAction onPress;
        private final boolean hideText;
        private int width = 150;
        private int height = 20;
        @Nullable
        private Identifier texture;
        private int textureWidth;
        private int textureHeight;
        @Nullable
        ButtonWidget.NarrationSupplier narrationSupplier;

        public Builder(Text text, ButtonWidget.PressAction onPress, boolean hideText) {
            this.text = text;
            this.onPress = onPress;
            this.hideText = hideText;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder dimension(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder texture(Identifier texture, int width, int height) {
            this.texture = texture;
            this.textureWidth = width;
            this.textureHeight = height;
            return this;
        }

        public Builder narration(ButtonWidget.NarrationSupplier narrationSupplier) {
            this.narrationSupplier = narrationSupplier;
            return this;
        }

        public TextIconButtonWidget build() {
            if (this.texture == null) {
                throw new IllegalStateException("Sprite not set");
            } else {
                this.texture = new Identifier(texture.getNamespace(), "textures/gui/sprites/"+texture.getPath() +".png");
                return new IconOnly(this.width, this.height, this.text, this.textureWidth, this.textureHeight, this.texture, this.onPress, this.narrationSupplier);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static class IconOnly extends TextIconButtonWidget {
        protected IconOnly(int i, int j, Text text, int k, int l, Identifier identifier, ButtonWidget.PressAction pressAction, @Nullable ButtonWidget.NarrationSupplier narrationSupplier) {
            super(i, j, text, k, l, identifier, pressAction, narrationSupplier);
        }
        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderButton(context, mouseX, mouseY, delta);
            int i = this.getX() + this.getWidth() / 2 - this.textureWidth / 2;
            int j = this.getY() + this.getHeight() / 2 - this.textureHeight / 2;
            context.drawTexture(this.texture, i, j, 0, 0, this.textureWidth, this.textureHeight, this.textureWidth, this.textureHeight);
        }

        public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        }
    }
}
