package net.brolard.valetotems;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.Renderable;
import net.runelite.api.DynamicObject;
import net.runelite.api.Animation;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

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
    private static final Set<Integer> ENT_TRAIL_IDS = new HashSet<>(Arrays.asList(57115, 57116, 57117));
    // Animation used when the ent has stepped on the trail
    private static final int STEPPED_ANIM_ID = 12346;

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

        Set<TileObject> admireObjects = new HashSet<>();

        for (int x = 0; x < 104; x++)
        {
            for (int y = 0; y < 104; y++)
            {
                Tile tile = tiles[z][x][y];
                if (tile == null)
                {
                    continue;
                }

                // Check all tile objects for ent trails or admire objects
                for (TileObject obj : getTileObjects(tile))
                {
                    if (isEntTrail(obj))
                    {
                        highlightEntTrail(graphics, obj);
                    }

                    if (ADMIRE_OBJECT_IDS.contains(obj.getId()))
                    {
                        admireObjects.add(obj);
                    }
                }
            }
        }

        highlightAdmireObjects(graphics, admireObjects);

        return null;
    }

    private boolean isEntTrail(TileObject object)
    {
        return object != null && ENT_TRAIL_IDS.contains(object.getId());
    }

    private void highlightEntTrail(Graphics2D graphics, TileObject groundObject)
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

        boolean stepped = false;
        if (groundObject instanceof GameObject)
        {
            Renderable renderable = ((GameObject) groundObject).getRenderable();
            if (renderable instanceof DynamicObject)
            {
                DynamicObject dyn = (DynamicObject) renderable;
                Animation anim = dyn.getAnimation();
                if (anim != null && anim.getId() == STEPPED_ANIM_ID)
                {
                    stepped = true;
                }
            }
        }

        Color base = stepped ? config.activeColor() : config.readyColor();

        graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 100));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);

        graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 30));
        graphics.fill(poly);
    }

    private void highlightAdmireObjects(Graphics2D graphics, Set<TileObject> objects)
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

            TileObject nearest = null;
            int bestDist = Integer.MAX_VALUE;
            WorldPoint npcWp = npc.getWorldLocation();

            for (TileObject obj : objects)
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
                highlightTileObject(graphics, nearest);
            }
        }
    }

    private void highlightTileObject(Graphics2D graphics, TileObject object)
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

    private Iterable<TileObject> getTileObjects(Tile tile)
    {
        Set<TileObject> objs = new HashSet<>();

        if (tile.getGroundObject() != null)
        {
            objs.add(tile.getGroundObject());
        }

        if (tile.getDecorativeObject() != null)
        {
            objs.add(tile.getDecorativeObject());
        }

        if (tile.getWallObject() != null)
        {
            objs.add(tile.getWallObject());
        }

        GameObject[] gos = tile.getGameObjects();
        if (gos != null)
        {
            for (GameObject go : gos)
            {
                if (go != null)
                {
                    objs.add(go);
                }
            }
        }

        return objs;
    }
}