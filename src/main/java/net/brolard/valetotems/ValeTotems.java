package net.brolard.valetotems;

import lombok.Getter;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.Text;

import java.util.*;

import static net.runelite.api.gameval.VarbitID.*;

@Slf4j
@PluginDescriptor(
		name = "Vale Totems",
		description = "Makes doing the Vale Totems minigame easier",
		tags = {"varlamore", "fletching", "minigame", "totem"}
)
public class ValeTotems extends Plugin
{
	@Inject private Client client;
	@Inject private ValeTotemsConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private WorldMapPointManager worldMapPointManager;
	@Inject private ValeTotemsOverlay overlay;
	@Inject private ValeTotemsAnimalOverlay animalOverlay;
	@Inject private ValeTotemsChatboxOverlay chatboxOverlay;
	@Inject private ValeTotemsTrailsOverlay trailsOverlay;
	@Inject private ClientThread clientThread;

	// ========================= CONSTANTS  =========================
	private static final int[] TOTEM_POINTS_VARBITS = {
			ENT_TOTEMS_SITE_1_POINTS, ENT_TOTEMS_SITE_2_POINTS, ENT_TOTEMS_SITE_3_POINTS,
			ENT_TOTEMS_SITE_4_POINTS, ENT_TOTEMS_SITE_5_POINTS, ENT_TOTEMS_SITE_6_POINTS,
			ENT_TOTEMS_SITE_7_POINTS, ENT_TOTEMS_SITE_8_POINTS
	};

	private static final int[] TOTEM_DECAY_VARBITS = {
			ENT_TOTEMS_SITE_1_DECAY, ENT_TOTEMS_SITE_2_DECAY, ENT_TOTEMS_SITE_3_DECAY,
			ENT_TOTEMS_SITE_4_DECAY, ENT_TOTEMS_SITE_5_DECAY, ENT_TOTEMS_SITE_6_DECAY,
			ENT_TOTEMS_SITE_7_DECAY, ENT_TOTEMS_SITE_8_DECAY
	};

	private static final int[] TOTEM_BASE_VARBITS = {
			ENT_TOTEMS_SITE_1_BASE_CARVED, ENT_TOTEMS_SITE_2_BASE_CARVED, ENT_TOTEMS_SITE_3_BASE_CARVED,
			ENT_TOTEMS_SITE_4_BASE_CARVED, ENT_TOTEMS_SITE_5_BASE_CARVED, ENT_TOTEMS_SITE_6_BASE_CARVED,
			ENT_TOTEMS_SITE_7_BASE_CARVED, ENT_TOTEMS_SITE_8_BASE_CARVED
	};

	private static final int[][] TOTEM_ANIMAL_VARBITS = {
			{ENT_TOTEMS_SITE_1_ANIMAL_1, ENT_TOTEMS_SITE_1_ANIMAL_2, ENT_TOTEMS_SITE_1_ANIMAL_3},
			{ENT_TOTEMS_SITE_2_ANIMAL_1, ENT_TOTEMS_SITE_2_ANIMAL_2, ENT_TOTEMS_SITE_2_ANIMAL_3},
			{ENT_TOTEMS_SITE_3_ANIMAL_1, ENT_TOTEMS_SITE_3_ANIMAL_2, ENT_TOTEMS_SITE_3_ANIMAL_3},
			{ENT_TOTEMS_SITE_4_ANIMAL_1, ENT_TOTEMS_SITE_4_ANIMAL_2, ENT_TOTEMS_SITE_4_ANIMAL_3},
			{ENT_TOTEMS_SITE_5_ANIMAL_1, ENT_TOTEMS_SITE_5_ANIMAL_2, ENT_TOTEMS_SITE_5_ANIMAL_3},
			{ENT_TOTEMS_SITE_6_ANIMAL_1, ENT_TOTEMS_SITE_6_ANIMAL_2, ENT_TOTEMS_SITE_6_ANIMAL_3},
			{ENT_TOTEMS_SITE_7_ANIMAL_1, ENT_TOTEMS_SITE_7_ANIMAL_2, ENT_TOTEMS_SITE_7_ANIMAL_3},
			{ENT_TOTEMS_SITE_8_ANIMAL_1, ENT_TOTEMS_SITE_8_ANIMAL_2, ENT_TOTEMS_SITE_8_ANIMAL_3}
	};

	private static final WorldPoint[] TOTEM_LOCATIONS = {
			new WorldPoint(1370, 3375, 0), new WorldPoint(1346, 3319, 0), new WorldPoint(1385, 3274, 0),
			new WorldPoint(1413, 3286, 0), new WorldPoint(1438, 3305, 0), new WorldPoint(1477, 3332, 0),
			new WorldPoint(1453, 3341, 0), new WorldPoint(1398, 3329, 0)
	};

	// Spirit animal mapping
	public static final Map<Integer, String> ANIMAL_NAMES = new HashMap<>();
	static {
		ANIMAL_NAMES.put(1, "Buffalo spirit");
		ANIMAL_NAMES.put(2, "Jaguar spirit");
		ANIMAL_NAMES.put(3, "Eagle spirit");
		ANIMAL_NAMES.put(4, "Snake spirit");
		ANIMAL_NAMES.put(5, "Scorpion spirit");
	}

	private final Map<Integer, TotemSiteInfo> totemSites = new HashMap<>();
	private Integer currentActiveSite = null;
	@Getter
	private final Set<Integer> correctAnimals = new HashSet<>();
	@Getter
	private final Set<Integer> carvedAnimals = new HashSet<>();
	private final Map<Integer, String> currentAnimalMap = new HashMap<>();
	private boolean expectingTotemDialog = false;
	private int animalsCarvedAtCurrentSite = 0;

	// ===================== LIFECYCLE =====================
	@Override
	protected void startUp() throws Exception
	{
		log.info("Vale Totems plugin started!");
		overlayManager.add(overlay);
		overlayManager.add(animalOverlay);
		overlayManager.add(chatboxOverlay);
		overlayManager.add(trailsOverlay);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::updateTotemSites);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Vale Totems plugin stopped!");
		overlayManager.remove(overlay);
		overlayManager.remove(animalOverlay);
		overlayManager.remove(chatboxOverlay);
		overlayManager.remove(trailsOverlay);
		totemSites.clear();
		currentActiveSite = null;
		correctAnimals.clear();
		carvedAnimals.clear();
		currentAnimalMap.clear();
	}

	@Provides
	ValeTotemsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ValeTotemsConfig.class);
	}

	// ===================== EVENT SUBSCRIPTIONS =====================
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		updateTotemSites();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateTotemSites();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		if (isTotemObject(object))
		{
			updateTotemSites();
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		if (isTotemObject(object))
		{
			updateTotemSites();
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (!config.debugMode())
		{
			return;
		}

		GroundObject groundObject = event.getGroundObject();
		if (groundObject != null)
		{
			ObjectComposition comp = client.getObjectDefinition(groundObject.getId());
			if (comp != null && comp.getName() != null)
			{
				String name = comp.getName();
				if (name.equalsIgnoreCase("Ent Trail"))
				{
					log.info("Ent Trail spawned: '{}' (ID: {}) at {}",
							name, groundObject.getId(), groundObject.getWorldLocation());
				}
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption() == null || event.getMenuTarget() == null)
		{
			return;
		}

		String option = event.getMenuOption().toLowerCase();
		String target = event.getMenuTarget().toLowerCase();

		if (config.debugMode())
		{
			log.info("Menu clicked: option='{}', target='{}', param0={}, param1={}",
					option, target, event.getParam0(), event.getParam1());
		}

		// Check if clicking on an animal spirit option
		for (Map.Entry<Integer, String> entry : ANIMAL_NAMES.entrySet())
		{
			if (option.contains(entry.getValue().toLowerCase()) ||
					target.contains(entry.getValue().toLowerCase()))
			{
				log.info("Animal spirit selected: {} (ID: {})", entry.getValue(), entry.getKey());
				// Don't clear here - let the chat message confirm it
				break;
			}
		}

		// Check if carving or building a totem
		if ((option.contains("carve") || option.contains("build")) && target.contains("totem"))
		{
			// Find the nearest totem to the player
			findNearestTotemSite();

			// If this is a build action, reset everything
			if (option.contains("build"))
			{
				animalsCarvedAtCurrentSite = 0;
				correctAnimals.clear();
				carvedAnimals.clear();
				currentAnimalMap.clear();
			}
		}
	}

	private void findNearestTotemSite()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		int closestSite = -1;
		int minDistance = Integer.MAX_VALUE;

		// Find which totem site we're closest to
		for (int i = 0; i < TOTEM_LOCATIONS.length; i++)
		{
			int distance = playerLocation.distanceTo(TOTEM_LOCATIONS[i]);
			if (config.debugMode())
			{
				log.debug("Distance to site {}: {}", i + 1, distance);
			}

			if (distance < minDistance && distance <= 10) // Must be within 10 tiles
			{
				minDistance = distance;
				closestSite = i;
			}
		}

		if (closestSite != -1)
		{
			currentActiveSite = closestSite;
			log.info("Player is carving at site {} (distance: {})", closestSite + 1, minDistance);
			updateCorrectAnimals();
		}
		else
		{
			log.warn("Could not determine which totem site the player is at");
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Handle widget 270 being loaded
		if (event.getGroupId() == 270)
		{
			// Only process if we're at a totem site
			if (currentActiveSite != null)
			{
				if (config.debugMode())
				{
					log.info("Totem dialog widget loaded for site {}", currentActiveSite + 1);
				}

				expectingTotemDialog = true;
				updateCorrectAnimals();

				// If debug mode, do the detailed scan
				if (config.debugMode())
				{
					clientThread.invokeLater(() -> {
						log.info("=== SCANNING WIDGET 270 FOR ANIMAL OPTIONS ===");
						scanCarvingWidget();
						return true;
					});
				}
			}
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		// Reset the flag when widget 270 closes
		if (event.getGroupId() == 270)
		{
			expectingTotemDialog = false;

			if (config.debugMode())
			{
				log.debug("Totem dialog closed");
			}
		}
	}

	private void scanCarvingWidget()
	{
		// Scan the specific carving widget
		Widget root = client.getWidget(270, 0);
		if (root == null)
		{
			log.warn("Widget 270 root is null");
			return;
		}

		// Only scan relevant widgets (0-24 based on the logs)
		for (int childId = 0; childId <= 24; childId++)
		{
			Widget child = client.getWidget(270, childId);
			if (child != null && !child.isHidden())
			{
				logDetailedWidgetInfo(child, 270, childId, -1);

				// Only check sub-children for animal widgets (14-18)
				if (childId >= 14 && childId <= 18)
				{
					// Check children of this child
					Widget[] subChildren = child.getChildren();
					if (subChildren != null)
					{
						for (int i = 0; i < subChildren.length; i++)
						{
							if (subChildren[i] != null)
							{
								logDetailedWidgetInfo(subChildren[i], 270, childId, i);
							}
						}
					}

					// Check static children
					Widget[] staticChildren = child.getStaticChildren();
					if (staticChildren != null)
					{
						for (int i = 0; i < staticChildren.length; i++)
						{
							if (staticChildren[i] != null)
							{
								logDetailedWidgetInfo(staticChildren[i], 270, childId, i);
							}
						}
					}
				}
			}
		}
		log.info("=== END WIDGET 270 SCAN ===");
	}

	private void logDetailedWidgetInfo(Widget widget, int groupId, int childId, int subChildId)
	{
		StringBuilder info = new StringBuilder();
		info.append("Widget[").append(groupId).append(",").append(childId);
		if (subChildId >= 0) info.append(",").append(subChildId);
		info.append("]");

		boolean hasContent = false;

		if (widget.getText() != null && !widget.getText().isEmpty())
		{
			info.append(" text='").append(Text.removeTags(widget.getText())).append("'");
			hasContent = true;
		}

		if (widget.getName() != null && !widget.getName().isEmpty())
		{
			info.append(" name='").append(widget.getName()).append("'");
			hasContent = true;
		}

		if (widget.getSpriteId() != -1)
		{
			info.append(" spriteId=").append(widget.getSpriteId());
			hasContent = true;
		}

		String[] actions = widget.getActions();
		if (actions != null && actions.length > 0)
		{
			boolean hasActions = false;
			for (String action : actions)
			{
				if (action != null && !action.isEmpty())
				{
					hasActions = true;
					break;
				}
			}
			if (hasActions)
			{
				info.append(" actions=[");
				for (int i = 0; i < actions.length; i++)
				{
					if (actions[i] != null && !actions[i].isEmpty())
					{
						if (i > 0) info.append(", ");
						info.append("'").append(actions[i]).append("'");
					}
				}
				info.append("]");
				hasContent = true;
			}
		}

		if (widget.isHidden())
		{
			info.append(" [HIDDEN]");
		}

		if (hasContent || subChildId == -1) // Always log direct children even if empty
		{
			log.info(info.toString());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Check if player moved away from the current totem site
		if (currentActiveSite != null)
		{
			Player player = client.getLocalPlayer();
			if (player != null)
			{
				WorldPoint playerLocation = player.getWorldLocation();
				WorldPoint totemLocation = TOTEM_LOCATIONS[currentActiveSite];

				// If player is more than 10 tiles away, reset everything
				if (playerLocation.distanceTo(totemLocation) > 10)
				{
					log.debug("Player moved away from totem site {}", currentActiveSite + 1);
					expectingTotemDialog = false;
					animalsCarvedAtCurrentSite = 0;
					correctAnimals.clear();
					carvedAnimals.clear();
					currentAnimalMap.clear();
					currentActiveSite = null;
				}
			}
		}

		// Debug mode: log ground objects near player
		if (config.debugMode() && config.highlightEntTrails())
		{
			logNearbyGroundObjects();
		}
	}

	private void logNearbyGroundObjects()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		LocalPoint playerLoc = player.getLocalLocation();
		int playerX = playerLoc.getSceneX();
		int playerY = playerLoc.getSceneY();

		Tile[][][] tiles = client.getScene().getTiles();
		int z = client.getPlane();

		// Check tiles within 5 tiles of player
		for (int x = Math.max(0, playerX - 5); x <= Math.min(103, playerX + 5); x++)
		{
			for (int y = Math.max(0, playerY - 5); y <= Math.min(103, playerY + 5); y++)
			{
				Tile tile = tiles[z][x][y];
				if (tile == null)
				{
					continue;
				}

				GroundObject groundObject = tile.getGroundObject();
				if (groundObject != null)
				{
					ObjectComposition comp = client.getObjectDefinition(groundObject.getId());
					if (comp != null && comp.getName() != null)
					{
						String name = comp.getName();
						// Log any object with "Ent" in the name to help find trail IDs
						if (name.toLowerCase().contains("ent") || name.equalsIgnoreCase("Ent Trail"))
						{
							log.info("Found ground object: '{}' (ID: {}) at scene [{}, {}]",
									name, groundObject.getId(), x, y);
						}
					}
				}
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase();

		// Check for spirit animal messages (when they tell you which animal is correct)
		for (Map.Entry<Integer, String> entry : ANIMAL_NAMES.entrySet())
		{
			if (message.contains(entry.getValue().toLowerCase()) &&
					message.contains("correct"))
			{
				correctAnimals.add(entry.getKey());
				currentAnimalMap.put(entry.getKey(), entry.getValue());
				break;
			}
		}

		// Check if an animal was carved - NEW LOGIC
		if ((message.contains("you carve") || message.contains("finish carving")) &&
				(message.contains("into the totem") || message.contains("spirit into the totem")))
		{
			// Find which animal was carved
			for (Map.Entry<Integer, String> entry : ANIMAL_NAMES.entrySet())
			{
				String animalName = entry.getValue().toLowerCase();
				// Remove "spirit" for matching as the message might just say "buffalo" not "buffalo spirit"
				String animalBase = animalName.replace(" spirit", "");

				if (message.contains(animalBase))
				{
					Integer animalId = entry.getKey();
					carvedAnimals.add(animalId);
					log.info("Carved animal: {} (ID: {})", entry.getValue(), animalId);

					animalsCarvedAtCurrentSite++;
					log.debug("Carved animal {} of 3 at site {}", animalsCarvedAtCurrentSite, currentActiveSite + 1);
					break;
				}
			}
		}

		// Clear animals when totem is completed or when all 3 animals are carved
		if (message.contains("you finish carving the totem") || message.contains("totem pole is complete"))
		{
			correctAnimals.clear();
			carvedAnimals.clear();
			currentAnimalMap.clear();
			currentActiveSite = null;
			expectingTotemDialog = false;
			animalsCarvedAtCurrentSite = 0;
			log.info("Totem completed/built - cleared all stored animals");
		}
		else if (animalsCarvedAtCurrentSite >= 3)
		{
			// Clear after 3 carves even if we don't see the completion message
			correctAnimals.clear();
			carvedAnimals.clear();
			currentAnimalMap.clear();
			currentActiveSite = null;
			expectingTotemDialog = false;
			animalsCarvedAtCurrentSite = 0;
			log.info("3 animals carved - cleared all stored animals");
		}
	}

	// ===================== HELPER METHODS =====================
	private void updateTotemSites()
	{
		// Cache client reference to avoid multiple calls
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (int i = 0; i < 8; i++)
		{
			TotemSiteInfo site = totemSites.computeIfAbsent(i, k -> new TotemSiteInfo());
			site.siteNumber = i + 1;
			site.location = TOTEM_LOCATIONS[i];

			try {
				site.points = client.getVarbitValue(TOTEM_POINTS_VARBITS[i]);
				site.decay = client.getVarbitValue(TOTEM_DECAY_VARBITS[i]);
				site.baseCarved = client.getVarbitValue(TOTEM_BASE_VARBITS[i]);
			} catch (Exception e) {
				log.debug("Error reading varbit for site {}: {}", i, e.getMessage());
			}
		}
	}

	private void updateCorrectAnimals()
	{
		if (currentActiveSite == null || currentActiveSite >= TOTEM_ANIMAL_VARBITS.length)
		{
			log.debug("updateCorrectAnimals: currentActiveSite is null or out of bounds: {}", currentActiveSite);
			return;
		}

		correctAnimals.clear();

		try {
			log.info("Checking animal varbits for site {}", currentActiveSite + 1);
			for (int i = 0; i < TOTEM_ANIMAL_VARBITS[currentActiveSite].length; i++)
			{
				int varbit = TOTEM_ANIMAL_VARBITS[currentActiveSite][i];
				int animalId = client.getVarbitValue(varbit);
				log.info("Site {} animal varbit[{}] = {} (value: {})",
						currentActiveSite + 1, i, varbit, animalId);

				if (animalId > 0 && animalId <= 5)
				{
					correctAnimals.add(animalId);
					log.info("Added correct animal: {} ({})", animalId, ANIMAL_NAMES.get(animalId));
				}
			}
			log.info("Total correct animals found: {}", correctAnimals.size());
		} catch (Exception e) {
			log.error("Error reading animal varbits: ", e);
		}
	}

	private boolean isTotemObject(GameObject object)
	{
		if (object == null)
		{
			return false;
		}

		ObjectComposition comp = client.getObjectDefinition(object.getId());
		if (comp == null || comp.getName() == null)
		{
			return false;
		}

		String name = comp.getName().toLowerCase();
		return name.contains("totem") && name.contains("pole");
	}

	// ===================== PUBLIC ACCESSORS =====================
	public Set<Integer> getCorrectAnimalIds()
	{
		return correctAnimals;
	}

	public Set<Integer> getUncarvedAnimalIds()
	{
		Set<Integer> uncarved = new HashSet<>(correctAnimals);
		uncarved.removeAll(carvedAnimals);
		return uncarved;
	}

	public Map<Integer, TotemSiteInfo> getTotemSites()
	{
		return Collections.unmodifiableMap(totemSites);
	}

	public Integer getCurrentActiveSite()
	{
		return currentActiveSite;
	}

	public boolean isExpectingTotemDialog()
	{
		return expectingTotemDialog;
	}

	// ===================== INNER CLASS =====================
	public static class TotemSiteInfo
	{
		public int siteNumber;
		public WorldPoint location;
		public int points;
		public int decay;
		public int baseCarved;

		public String getStatus()
		{
			if (baseCarved == 0 && decay == 0)
			{
				return "Ready to build";
			}
			else if (baseCarved == 1 && decay > 0)
			{
				return "Active";
			}
			else if (points > 0)
			{
				return "Claimable (" + points + ")";
			}
			else
			{
				return "Empty";
			}
		}
	}
}