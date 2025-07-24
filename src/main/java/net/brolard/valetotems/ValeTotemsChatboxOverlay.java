package net.brolard.valetotems;

import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ValeTotemsChatboxOverlay extends Overlay
{
    private final Client client;
    private final ValeTotems plugin;
    private final ValeTotemsConfig config;

    private boolean hasLoggedOptions = false;
    private Set<Integer> lastLoggedAnimals = new HashSet<>();

    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!config.highlightDialogOptions())
        {
            return null;
        }

        // Only render if we're expecting a totem dialog
        if (!plugin.isExpectingTotemDialog())
        {
            return null;
        }

        Set<Integer> correct = plugin.getCorrectAnimalIds();
        Set<Integer> uncarved = plugin.getUncarvedAnimalIds();

        if (correct.isEmpty() || uncarved.isEmpty())
        {
            hasLoggedOptions = false;
            lastLoggedAnimals.clear();
            return null;
        }

        // Check if widget 270 is visible (the carving interface)
        Widget root = client.getWidget(270, 0);
        if (root == null || root.isHidden())
        {
            hasLoggedOptions = false;
            return null;
        }

        // The animal options are at specific child IDs:
        // Buffalo = 14 (option 1)
        // Jaguar = 15 (option 2)
        // Eagle = 16 (option 3)
        // Snake = 17 (option 4)
        // Scorpion = 18 (option 5)

        // Check each animal option
        for (int animalId = 1; animalId <= 5; animalId++)
        {
            // Only highlight if it's correct AND hasn't been carved yet
            if (correct.contains(animalId) && uncarved.contains(animalId))
            {
                // Calculate the widget child ID (14 + (animalId - 1))
                int widgetChildId = 13 + animalId;
                Widget animalWidget = client.getWidget(270, widgetChildId);

                if (animalWidget != null && !animalWidget.isHidden())
                {
                    highlightWidget(g, animalWidget);
                }
            }
        }

        // Log once when the correct animals change
        if (config.debugMode() && (!hasLoggedOptions || !uncarved.equals(lastLoggedAnimals)))
        {
            StringBuilder animalNames = new StringBuilder();
            for (Integer id : uncarved)
            {
                if (animalNames.length() > 0) animalNames.append(", ");
                animalNames.append(ValeTotems.ANIMAL_NAMES.getOrDefault(id, "Unknown"));
            }
            log.info("Highlighting uncarved animals: {}", animalNames);
            hasLoggedOptions = true;
            lastLoggedAnimals = new HashSet<>(uncarved);
        }

        return null;
    }

    private void highlightWidget(Graphics2D graphics, Widget widget)
    {
        if (widget == null)
        {
            return;
        }

        Rectangle bounds = widget.getBounds();
        if (bounds != null)
        {
            // Fill with translucent green
            graphics.setColor(new Color(0, 255, 0, 50));
            graphics.fill(bounds);

            // Draw green border
            graphics.setColor(Color.GREEN);
            graphics.setStroke(new BasicStroke(3));
            graphics.draw(bounds);
        }
    }
}