package net.brolard.valetotems;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValeTotemsTrailsOverlay extends Overlay
{
    private final Client client;
    private final ValeTotems plugin;
    private final ValeTotemsConfig config;

    // Known Ent Trail IDs
    private static final Set<Integer> ENT_TRAIL_IDS = new HashSet<>(Arrays.asList(55115, 55116));

    @Inject
    public ValeTotemsTrailsOverlay(Client client, ValeTotems plugin, ValeTotemsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.highlightEntTrails())
        {
            return null;
        }

        Tile[][][] tiles = client.getScene().getTiles();
        int z = client.getPlane();

        for (int x = 0; x < 104; x++)
        {
            for (int y = 0; y < 104; y++)
            {
                Tile tile = tiles[z][x][y];
                if (tile == null)
                {
                    continue;
                }

                GroundObject groundObject = tile.getGroundObject();
                if (groundObject != null && isEntTrail(groundObject))
                {
                    highlightGroundObject(graphics, groundObject);
                }
            }
        }

        return null;
    }

    private boolean isEntTrail(GroundObject groundObject)
    {
        // First check if the ID matches
        if (!ENT_TRAIL_IDS.contains(groundObject.getId()))
        {
            return false;
        }

        // Then verify the name to be extra sure
        ObjectComposition comp = client.getObjectDefinition(groundObject.getId());
        if (comp == null || comp.getName() == null)
        {
            return false;
        }

        // Must be exactly "Ent Trail" (case insensitive to be safe)
        return comp.getName().equalsIgnoreCase("Ent Trail");
    }

    private void highlightGroundObject(Graphics2D graphics, GroundObject groundObject)
    {
        LocalPoint lp = groundObject.getLocalLocation();
        if (lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return;
        }

        // Draw a bright green outline
        graphics.setColor(new Color(0, 255, 0, 100));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);

        // Fill with translucent green
        graphics.setColor(new Color(0, 255, 0, 30));
        graphics.fill(poly);
    }
}