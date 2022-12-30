package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.lib.util.screen.TexturedOverlayButtonWidget;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.screen.PictureSignHelperScreen;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinSignEditScreen extends Screen {
    @Shadow
    @Final
    protected SignBlockEntity blockEntity;

    @Shadow
    @Final
    protected String[] text;
    private static final Identifier PICTURESIGN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/picturesign_button.png");
    private static final Identifier CLIPBOARD_ICON_TEXTURE = new Identifier("picturesign","textures/gui/clipboard_button.png");
    private static final Identifier TRASHBIN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/trashbin_button.png");

    protected MixinSignEditScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"),method = "init")
    private void picturesign$init(CallbackInfo ci) {
        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 84, this.height - 40, 20, 20, 0, 0, 20, CLIPBOARD_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = PictureSignClient.clipboard[i];
                    blockEntity.setTextOnRow(i, Text.of(text[i]));
                }
            }, Text.of("")));
        if (PictureSignConfig.helperUi)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 62, this.height - 40, 20, 20, 0, 0, 20, TRASHBIN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    text[i] = "";
                    blockEntity.setTextOnRow(i, Text.empty());
                }
            }, Text.of("")));
        if (PictureSignConfig.helperUi)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 40, this.height - 40, 20, 20, 0, 0, 20, PICTURESIGN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                blockEntity.setEditable(true);
                Objects.requireNonNull(client).setScreen(new PictureSignHelperScreen(this.blockEntity,false));
            }, Text.of("")));
    }
}
