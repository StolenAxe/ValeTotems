package net.brolard.valetotems;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
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

    // Known Ent Trail IDs (game objects spawned as the ent walks)
    private static final Set<Integer> ENT_TRAIL_IDS = new HashSet<>(Arrays.asList(57115, 57116));

    // Locations the ent admires when performing its worship animation
    private static final Set<Integer> ADMIRE_OBJECT_IDS = new HashSet<>(Arrays.asList(
            ObjectID.ENT_TOTEMS_ADMIRE_LOC_1,
            ObjectID.ENT_TOTEMS_ADMIRE_LOC_2,
            ObjectID.ENT_TOTEMS_ADMIRE_LOC_3
    ));

    // Ent NPC IDs
    private static final Set<Integer> ENT_NPC_IDS = new HashSet<>(Arrays.asList(
            NpcID.ENT_TOTEMS_ENT,
            NpcID.ENT_TOTEMS_ENT_BUFFED
    ));

    // Worship animation IDs
    private static final Set<Integer> ENT_WORSHIP_ANIMS = new HashSet<>(Arrays.asList(
            AnimationID.NPC_ENT_WORSHIP_01,
            AnimationID.NPC_ENT_WORSHIP_02
    ));

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

        Set<GameObject> admireObjects = new HashSet<>();

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

                // Collect admire objects for later highlighting
                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects != null)
                {
                    for (GameObject obj : gameObjects)
                    {
                        if (obj != null && ADMIRE_OBJECT_IDS.contains(obj.getId()))
                        {
                            admireObjects.add(obj);
                        }
                    }
                }
            }
        }

        highlightAdmireObjects(graphics, admireObjects);

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

    private void highlightAdmireObjects(Graphics2D graphics, Set<GameObject> objects)
    {
        if (objects.isEmpty())
        {
            return;
        }

        for (NPC npc : client.getNpcs())
        {
            if (!ENT_NPC_IDS.contains(npc.getId()))
            {
                continue;
            }

            if (!ENT_WORSHIP_ANIMS.contains(npc.getAnimation()))
            {
                continue;
            }

            GameObject nearest = null;
            int bestDist = Integer.MAX_VALUE;
            WorldPoint npcWp = npc.getWorldLocation();

            for (GameObject obj : objects)
            {
                int dist = npcWp.distanceTo(obj.getWorldLocation());
                if (dist < bestDist)
                {
                    bestDist = dist;
                    nearest = obj;
                }
            }

            if (nearest != null)
            {
                highlightGameObject(graphics, nearest);
            }
        }
    }

    private void highlightGameObject(Graphics2D graphics, GameObject object)
    {
        LocalPoint lp = object.getLocalLocation();
        if (lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return;
        }

        graphics.setColor(new Color(0, 255, 255, 100));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);

        graphics.setColor(new Color(0, 255, 255, 30));
        graphics.fill(poly);
    }
}