package net.brolard.valetotems;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("valetotems")
public interface ValeTotemsConfig extends Config
{
	@ConfigItem(
			keyName = "showTooltips",
			name = "Show Site Tooltips",
			description = "Show tooltips with site status information"
	)
	default boolean showTooltips()
	{
		return false;
	}

	@ConfigItem(
			keyName = "highlightDialogOptions",
			name = "Highlight Dialog Options",
			description = "Highlight the correct animal options in blue when carving totems"
	)
	default boolean highlightDialogOptions()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showAnimalPopup",
			name = "Show Animal Popup",
			description = "Show a popup with the correct animal numbers when carving"
	)
	default boolean showAnimalPopup()
	{
		return false;
	}

	@ConfigItem(
			keyName = "highlightEntTrails",
			name = "Highlight Ent Trails",
			description = "Highlight Ent Trail ground objects in green. WIP, currently broken."
	)
	default boolean highlightEntTrails()
	{
		return false;
	}

	@ConfigItem(
			keyName = "debugMode",
			name = "Debug Mode",
			description = "Show debug information to help find widget IDs"
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showWorldMapOverlay",
			name = "Show World Map Overlay",
			description = "Display totem status boxes on the world map"
	)
	default boolean showWorldMapOverlay()
	{
		return false;
	}

	@ConfigItem(
			keyName = "readyColor",
			name = "Ready Color",
			description = "Color for ready-to-build totems"
	)
	default java.awt.Color readyColor()
	{
		return java.awt.Color.CYAN;
	}

	@ConfigItem(
			keyName = "activeColor",
			name = "Active Color",
			description = "Color for active totems"
	)
	default java.awt.Color activeColor()
	{
		return java.awt.Color.GREEN;
	}

	@ConfigItem(
			keyName = "emptyColor",
			name = "Empty Color",
			description = "Color for empty totem sites"
	)
	default java.awt.Color emptyColor()
	{
		return java.awt.Color.GRAY;
	}
}