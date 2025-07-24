package net.brolard.valetotems;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class ValeTotemsOverlay extends OverlayPanel
{
    private final Client client;
    private final ValeTotems plugin;
    private final ValeTotemsConfig config;

    @Inject
    public ValeTotemsOverlay(Client client, ValeTotems plugin, ValeTotemsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showTooltips())
        {
            return null;
        }

        panelComponent.getChildren().clear();

        // Add title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Vale Totems")
                .color(Color.WHITE)
                .build());

        // Show status for each totem site in compact format
        int totalPoints = 0;
        for (int i = 0; i < 8; i++)
        {
            ValeTotems.TotemSiteInfo site = plugin.getTotemSites().get(i);
            if (site != null)
            {
                totalPoints += site.points;

                String siteLabel = "Site " + site.siteNumber + ": " + getCompactStatus(site);
                String pointsText = site.points > 0 ? String.valueOf(site.points) : "0";
                Color statusColor = getStatusColor(site);

                panelComponent.getChildren().add(LineComponent.builder()
                        .left(siteLabel)
                        .leftColor(statusColor)
                        .right(pointsText)
                        .rightColor(site.points > 0 ? Color.YELLOW : Color.GRAY)
                        .build());
            }
        }

        // Show total points at bottom
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total Points")
                .leftColor(Color.WHITE)
                .right(String.valueOf(totalPoints))
                .rightColor(Color.YELLOW)
                .build());

        return super.render(graphics);
    }

    private String getCompactStatus(ValeTotems.TotemSiteInfo site)
    {
        if (site.baseCarved == 0 && site.decay == 0)
        {
            return "Ready";
        }
        else if (site.baseCarved == 1)
        {
            return "Active";
        }
        else
        {
            return "Empty";
        }
    }

    private Color getStatusColor(ValeTotems.TotemSiteInfo site)
    {
        if (site.baseCarved == 0 && site.decay == 0)
        {
            return Color.GREEN; // Ready to build
        }
        else if (site.baseCarved == 1)
        {
            return Color.CYAN; // Active totem
        }
        else
        {
            return Color.GRAY; // Empty/other
        }
    }
}