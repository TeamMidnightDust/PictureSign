package eu.midnightdust.picturesign.mixin;

import eu.midnightdust.picturesign.PictureSignClient;
import eu.midnightdust.picturesign.config.PictureSignConfig;
import eu.midnightdust.picturesign.screen.PictureSignHelperScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import static eu.midnightdust.picturesign.PictureSignClient.id;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinSignEditScreen extends Screen {
    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Shadow @Final private boolean isFrontText;

    @Unique private static final ResourceLocation PICTURESIGN_ICON_TEXTURE = id("icon/picturesign");
    @Unique private static final ResourceLocation CLIPBOARD_ICON_TEXTURE = id("icon/clipboard");
    @Unique private static final ResourceLocation TRASHBIN_ICON_TEXTURE = id("icon/trashbin");
    @Unique private static boolean picturesign$switchScreen = false;

    protected MixinSignEditScreen(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"),method = "init")
    private void picturesign$init(CallbackInfo ci) {
        if (PictureSignClient.clipboard != null && PictureSignClient.clipboard[0] != null) {
            SpriteIconButton clipboardBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    messages[i] = PictureSignClient.clipboard[i];
                    int finalI = i;
                    sign.updateText(changer -> changer.setMessage(finalI, Component.nullToEmpty(messages[finalI])), isFrontText);
                }
            }, true).sprite(CLIPBOARD_ICON_TEXTURE, 16, 16).size(20, 20).build();
            clipboardBuilder.setPosition(this.width - 84, this.height - 40);
            this.addRenderableWidget(clipboardBuilder);
        }
        if (PictureSignConfig.helperUi) {
            SpriteIconButton trashbinBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                for (int i = 0; i < 4; i++) {
                    messages[i] = "";
                    int finalI = i;
                    sign.updateText(changer -> changer.setMessage(finalI, Component.empty()), isFrontText);
                }
            }, true).sprite(TRASHBIN_ICON_TEXTURE, 16, 16).size(20, 20).build();
            trashbinBuilder.setPosition(this.width - 62, this.height - 40);
            this.addRenderableWidget(trashbinBuilder);

            SpriteIconButton picturesignBuilder = SpriteIconButton.builder(Component.empty(), (buttonWidget) -> {
                picturesign$switchScreen = true;
                Objects.requireNonNull(minecraft).setScreen(new PictureSignHelperScreen(this.sign, isFrontText, false));
            }, true).sprite(PICTURESIGN_ICON_TEXTURE, 16, 16).size(20, 20).build();
            picturesignBuilder.setPosition(this.width - 40, this.height - 40);
            this.addRenderableWidget(picturesignBuilder);
        }
    }
    @Inject(at = @At("HEAD"), method = "removed", cancellable = true)
    private void picturesign$removed(CallbackInfo ci) {
        if (picturesign$switchScreen) {
            picturesign$switchScreen = false;
            ci.cancel();
        }
    }
}


