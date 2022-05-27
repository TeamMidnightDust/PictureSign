package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.lib.util.screen.TexturedOverlayButtonWidget;
import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.screen.PictureSignHelperScreen;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
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

@Mixin(SignEditScreen.class)
public abstract class MixinSignEditScreen extends Screen {
    private static final Identifier PICTURESIGN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/picturesign_button.png");
    private static final Identifier CLIPBOARD_ICON_TEXTURE = new Identifier("picturesign","textures/gui/clipboard_button.png");
    private static final Identifier TRASHBIN_ICON_TEXTURE = new Identifier("picturesign","textures/gui/trashbin_button.png");
    @Shadow @Final private SignBlockEntity sign;

    @Shadow @Final private String[] text;

    protected MixinSignEditScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"),method = "init")
    private void picturesign$init(CallbackInfo ci) {
        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 84, this.height - 40, 20, 20, 0, 0, 20, CLIPBOARD_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                sign.setTextOnRow(0, Text.of(PictureSignClient.clipboard[0]));
                sign.setTextOnRow(1, Text.of(PictureSignClient.clipboard[1]));
                sign.setTextOnRow(2, Text.of(PictureSignClient.clipboard[2]));
                sign.setTextOnRow(3, Text.of(PictureSignClient.clipboard[3]));
                text[0] = PictureSignClient.clipboard[0];
                text[1] = PictureSignClient.clipboard[1];
                text[2] = PictureSignClient.clipboard[2];
                text[3] = PictureSignClient.clipboard[3];
            }, Text.of("")));
        if (PictureSignConfig.helperUi)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 62, this.height - 40, 20, 20, 0, 0, 20, TRASHBIN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                sign.setTextOnRow(0, Text.of(""));
                sign.setTextOnRow(1, Text.of(""));
                sign.setTextOnRow(2, Text.of(""));
                sign.setTextOnRow(3, Text.of(""));
                text[0] = "";
                text[1] = "";
                text[2] = "";
                text[3] = "";
            }, Text.of("")));
        if (PictureSignConfig.helperUi)
            this.addDrawableChild(new TexturedOverlayButtonWidget(this.width - 40, this.height - 40, 20, 20, 0, 0, 20, PICTURESIGN_ICON_TEXTURE, 32, 64, (buttonWidget) -> {
                sign.setEditable(true);
                Objects.requireNonNull(client).setScreen(new PictureSignHelperScreen(this.sign,false));
            }, Text.of("")));
    }
}
