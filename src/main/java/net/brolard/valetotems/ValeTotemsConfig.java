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
}