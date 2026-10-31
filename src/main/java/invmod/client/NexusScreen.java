package invmod.client;

import invmod.block.NexusBlockEntity;
import invmod.block.NexusMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The nexus screen, laid out on the original nexusgui.png. */
public class NexusScreen extends AbstractContainerScreen<NexusMenu> {
    private static final Identifier BACKGROUND = ModLayers.texture("nexusgui");
    private static final int TEXT = 0xFF404040;

    public NexusScreen(NexusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        int mode = this.menu.getMode();
        int flux = this.menu.getFlux() * 26 / NexusBlockEntity.MAX_GENERATION_TIME;
        if (flux > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 126, y + 28 + 26 - flux, 185, 26 - flux, 9, flux, 256, 256);
        }
        int cook = this.menu.getCookTime() * 18 / NexusBlockEntity.MAX_COOK_TIME;
        if (cook > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 31, y + 51, 204, 0, cook, 2, 256, 256);
        }
        if (mode == NexusBlockEntity.MODE_INVASION || mode == NexusBlockEntity.MODE_CONTINUOUS_INVASION) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 19, y + 29, 176, 0, 9, 31, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 19, y + 19, 194, 0, 9, 9, 256, 256);
        } else if (mode == NexusBlockEntity.MODE_CONTINUOUS) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 19, y + 29, 176, 31, 9, 31, 256, 256);
        }
        if (this.menu.isActivating()) {
            int l = this.menu.getActivationTimer() * 31 / NexusBlockEntity.TOTAL_ACTIVATION_TIME;
            int v = mode == NexusBlockEntity.MODE_CONTINUOUS_ACTIVATING ? 62 : 31;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x + 19, y + 29 + 31 - l, 176, v - l, 9, l, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int mode = this.menu.getMode();
        graphics.text(this.font, "Nexus - Level " + this.menu.getNexusLevel(), 46, 6, TEXT, false);
        graphics.text(this.font, this.menu.getKills() + " mobs killed", 96, 60, TEXT, false);
        graphics.text(this.font, "R: " + this.menu.getSpawnRadius(), 142, 72, TEXT, false);
        if (mode == NexusBlockEntity.MODE_INVASION || mode == NexusBlockEntity.MODE_CONTINUOUS_INVASION) {
            graphics.text(this.font, "Activated!", 13, 62, TEXT, false);
            graphics.text(this.font, "Wave " + this.menu.getCurrentWave(), 55, 37, TEXT, false);
            graphics.text(this.font, "HP " + this.menu.getHp(), 55, 47, TEXT, false);
        } else if (mode == NexusBlockEntity.MODE_CONTINUOUS) {
            graphics.text(this.font, "Power:", 56, 31, TEXT, false);
            graphics.text(this.font, String.valueOf(this.menu.getPowerLevel()), 61, 44, TEXT, false);
        }
        if (this.menu.isActivating() && mode == NexusBlockEntity.MODE_IDLE) {
            graphics.text(this.font, "Activating...", 13, 62, TEXT, false);
            graphics.text(this.font, "Are you sure?", 8, 72, TEXT, false);
        }
    }
}
