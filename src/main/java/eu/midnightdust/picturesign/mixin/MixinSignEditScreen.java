package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.screen.PictureSignHelperScreen;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static eu.midnightdust.picturesign.PictureSignClient.MOD_ID;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinSignEditScreen extends Screen {
    private static final Identifier PICTURESIGN_ICON_TEXTURE = new Identifier(MOD_ID,"icon/picturesign");
    private static final Identifier CLIPBOARD_ICON_TEXTURE = new Identifier(MOD_ID,"icon/clipboard");
    private static final Identifier TRASHBIN_ICON_TEXTURE = new Identifier(MOD_ID,"icon/trashbin");
    @Shadow @Final private SignBlockEntity blockEntity;

    @Shadow @Final private String[] messages;

    @Shadow @Final private boolean front;
    private static boolean switchScreen = false;

    protected MixinSignEditScreen(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"),method = "init")
    private void picturesign$init(CallbackInfo ci) {
        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null) {
            TextIconButtonWidget clipboardBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    messages[i] = PictureSignClient.clipboard[i];
                    int finalI = i;
                    blockEntity.changeText(changer -> changer.withMessage(finalI, Text.of(messages[finalI])), front);
                }
            }, true).texture(CLIPBOARD_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            clipboardBuilder.setPosition(this.width - 84, this.height - 40);
            this.addDrawableChild(clipboardBuilder);
        }
        if (PictureSignConfig.helperUi) {
            TextIconButtonWidget trashbinBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    messages[i] = "";
                    int finalI = i;
                    blockEntity.changeText(changer -> changer.withMessage(finalI, Text.empty()), front);
                }
            }, true).texture(TRASHBIN_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            trashbinBuilder.setPosition(this.width - 62, this.height - 40);
            this.addDrawableChild(trashbinBuilder);

            TextIconButtonWidget picturesignBuilder = TextIconButtonWidget.builder(Text.empty(), (buttonWidget) -> {
                switchScreen = true;
                Objects.requireNonNull(client).setScreen(new PictureSignHelperScreen(this.blockEntity, front, false));
            }, true).texture(PICTURESIGN_ICON_TEXTURE, 16, 16).dimension(20, 20).build();
            picturesignBuilder.setPosition(this.width - 40, this.height - 40);
            this.addDrawableChild(picturesignBuilder);
        }
    }
    @Inject(at = @At("HEAD"), method = "removed", cancellable = true)
    private void picturesign$removed(CallbackInfo ci) {
        if (switchScreen) {
            switchScreen = false;
            ci.cancel();
        }
    }
}


