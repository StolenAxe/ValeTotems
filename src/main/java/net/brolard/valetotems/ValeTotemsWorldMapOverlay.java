package net.brolard.valetotems;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Simple world map point representing a totem site with a colored box
 * and optional points displayed inside.
 */
public class ValeTotemsWorldMapOverlay extends WorldMapPoint
{
    private final int points;
    private final Color color;

    public ValeTotemsWorldMapOverlay(WorldPoint worldPoint, int points, Color color, BufferedImage image)
    {
        super(worldPoint, image);
        this.points = points;
        this.color = color;
        setJumpOnClick(true);
        setTarget(worldPoint);
        setName("Totem " + worldPoint);
    }

    public static BufferedImage createImage(Color color, int points)
    {
        int size = 15;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, size - 1, size - 1);
        g.setColor(Color.WHITE);
        String txt = String.valueOf(points);
        g.drawString(txt, 3, 12);
        g.dispose();
        return img;
    }

    public int getPoints()
    {
        return points;
    }

    public Color getColor()
    {
        return color;
    }
}