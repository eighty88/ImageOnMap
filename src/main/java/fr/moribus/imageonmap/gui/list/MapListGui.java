/*
 * Copyright (C) 2013 Moribus
 * Copyright (C) 2015 ProkopyL <prokopylmc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.moribus.imageonmap.gui.list;

import fr.moribus.imageonmap.gui.core.*;
import fr.moribus.imageonmap.map.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;


public class MapListGui extends AbstractGui {

	private final Integer MAPS_PER_PAGE = 7 * 3;


	private ImageMap.Type mapsType;

	private List<ImageMap> maps = new ArrayList<>();

	private int currentPage = 0;
	private int lastPage = 0;


	public MapListGui(ImageMap.Type mapsType) {
		this.mapsType = mapsType;
	}

	@Override
	public void display(Player player) {

		String inventoryTitle = "Maps » ";
		if(mapsType == ImageMap.Type.POSTER) {
			inventoryTitle += "Posters";
		} else {
			inventoryTitle += "Single maps";
		}

		inventory = Bukkit.createInventory(player, 6 * 9, ChatColor.BLACK + inventoryTitle);

		ItemStack back = new ItemStack(Material.EMERALD);
		ItemMeta meta = back.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "« Back");
		meta.setLore(Arrays.asList(
				ChatColor.GRAY + "Go back to the map",
				ChatColor.GRAY + "type selector."
		));
		back.setItemMeta(meta);

		setSlotData(back, inventory.getSize() - 5, "back");

		player.openInventory(getInventory());

		update(player);
	}

	@Override
	public void update(Player player) {
		update(player, false);
	}

	public void update(final Player player, final Boolean noCache) {
		if (maps == null || maps.isEmpty() || noCache) {
			updateMapCache(player);
		}


		if(maps.isEmpty()) {
			ItemStack empty = new ItemStack(Material.BARRIER);
			ItemMeta meta = empty.getItemMeta();
			meta.setDisplayName(ChatColor.RED + "Nothing to display here");
			meta.setLore(Arrays.asList(
					ChatColor.GRAY + "You don't have any map in",
					ChatColor.GRAY + "this category. Try the other",
					ChatColor.GRAY + "one, or create a new map with",
					ChatColor.WHITE + "/tomap <URL> [resize]"
			));

			empty.setItemMeta(meta);

			setSlotData(empty, 13, "");

			return;
		}


		int index = currentPage * MAPS_PER_PAGE;
		int lastIndex = index + MAPS_PER_PAGE;
		int slot = 10;

		ImageMap map;

		for (; index < lastIndex; index++) {
			try {
				map = maps.get(index);
				setSlotData(getMapIcon(map), slot, map.getId());
			} catch(IndexOutOfBoundsException e) {
				setSlotData(new ItemStack(Material.AIR), slot, "");
			}

			if (slot % 9 == 7) slot += 3;
			else slot++;
		}

		if (currentPage != 0)
			setSlotData(getPageIcon(currentPage - 1), inventory.getSize() - 9, "previousPage");
		else
			setSlotData(new ItemStack(Material.AIR), inventory.getSize() - 9, "");

		if (currentPage != lastPage)
			setSlotData(getPageIcon(currentPage + 1), inventory.getSize() - 1, "nextPage");
		else
			setSlotData(new ItemStack(Material.AIR), inventory.getSize() - 1, "");
	}

	@Override
	public void onClick(Player player, ItemStack stack, String action, ClickType clickType) {

		switch (action)
		{
			case "back":
				GuiManager.openGui(player, new CategorySelectionGui());
				return;

			case "previousPage":
				previousPage(player);
				return;

			case "nextPage":
				nextPage(player);
				return;

			default:
				// The action is the map ID
				ImageMap map = null;
				for(ImageMap lookupMap : maps)
				{
					if(lookupMap.getId().equals(action))
					{
						map = lookupMap;
						break;
					}
				}

				if(map == null) return;

				switch (clickType)
				{
					case LEFT:
					case SHIFT_LEFT:
						if(map.give(player))
						{
							player.sendMessage(ChatColor.GRAY + "The requested map was too big to fit in your inventory.");
							player.sendMessage(ChatColor.GRAY + "Use '/maptool getremaining' to get the remaining maps.");
						}

						player.closeInventory();

						break;

					case RIGHT:
					case SHIFT_RIGHT:
						// TODO
						break;
				}

		}

	}

	private void nextPage(Player player)
	{
		if(currentPage < lastPage) currentPage++;

		update(player);
	}

	private void previousPage(Player player)
	{
		if(currentPage > 0) currentPage--;

		update(player);
	}


	private void updateMapCache(Player player)
	{
		for(ImageMap map : MapManager.getMapList(player.getUniqueId()))
		{
			if(map.getType() == mapsType) maps.add(map);
		}

		lastPage = (int) Math.ceil(((double) maps.size()) / ((double) MAPS_PER_PAGE)) - 1;

		if(currentPage > lastPage)
			currentPage = lastPage;
	}

	private ItemStack getMapIcon(ImageMap map)
	{
		ItemStack icon = new ItemStack(Material.MAP);


		ItemMeta meta = icon.getItemMeta();

		meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + map.getId().replace("-", " "));

		if(map.getType() == ImageMap.Type.POSTER)
		{
			meta.setLore(Arrays.asList(
					ChatColor.WHITE + "" + map.getMapCount() + " map" + (map.getMapCount() != 1 ? "s" : "")
			));
		}

		else
		{
			meta.setLore(Arrays.asList(
					ChatColor.WHITE + "Single map"
			));
		}

		List<String> lore = meta.getLore();
		lore.addAll(Arrays.asList(
				"",
				ChatColor.GRAY + "» Left-click to get this map",
				ChatColor.GRAY + "» Right-click for details"
		));
		meta.setLore(lore);

		GuiUtils.removeVanillaInfos(meta);

		icon.setItemMeta(meta);


		return icon;
	}

	private ItemStack getPageIcon(Integer targetPage)
	{
		ItemStack icon = new ItemStack(Material.ARROW);
		ItemMeta meta = icon.getItemMeta();


		if(currentPage < targetPage) { // next page
			meta.setDisplayName(ChatColor.GREEN + "Next page");
		}
		else {
			meta.setDisplayName(ChatColor.GREEN + "Previous page");
		}

		meta.setLore(Collections.singletonList(
				ChatColor.GRAY + "Go to page " + ChatColor.WHITE + (targetPage + 1) + ChatColor.GRAY + " of " + ChatColor.WHITE + (lastPage + 1)
		));


		icon.setItemMeta(meta);

		return icon;
	}
}
