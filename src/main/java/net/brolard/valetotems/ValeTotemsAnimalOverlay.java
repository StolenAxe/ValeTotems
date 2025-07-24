package net.brolard.valetotems;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.Set;

public class ValeTotemsAnimalOverlay extends OverlayPanel
{
    private final Client client;
    private final ValeTotems plugin;
    private final ValeTotemsConfig config;

    @Inject
    public ValeTotemsAnimalOverlay(Client client, ValeTotems plugin, ValeTotemsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Only show when we have active animals to display
        Set<Integer> animalIds = plugin.getCorrectAnimals();
        if (animalIds == null || animalIds.isEmpty() || !config.showAnimalPopup())
        {
            return null;
        }

        // Check if the current site's totem is already active
        Integer currentSite = plugin.getCurrentActiveSite();
        if (currentSite != null)
        {
            ValeTotems.TotemSiteInfo site = plugin.getTotemSites().get(currentSite);
            if (site != null && site.baseCarved == 1 && site.decay > 0)
            {
                // Totem is active, don't show the overlay
                return null;
            }
        }

        panelComponent.getChildren().clear();

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Correct Animals")
                .color(Color.CYAN)
                .build());

        Set<Integer> carvedAnimals = plugin.getCarvedAnimals();

        // Show each animal with its number and carved status
        for (Integer animalId : animalIds)
        {
            String animalName = ValeTotems.ANIMAL_NAMES.getOrDefault(animalId, "Unknown");
            boolean isCarved = carvedAnimals.contains(animalId);

            LineComponent.LineComponentBuilder builder = LineComponent.builder()
                    .left(animalName)
                    .right(animalId.toString());

            if (isCarved)
            {
                builder.leftColor(Color.GRAY)
                        .rightColor(Color.GRAY);
            }
            else
            {
                builder.leftColor(Color.WHITE)
                        .rightColor(Color.GREEN);
            }

            panelComponent.getChildren().add(builder.build());
        }

        return super.render(graphics);
    }
}