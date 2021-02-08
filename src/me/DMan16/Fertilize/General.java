package me.DMan16.Fertilize;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.md_5.bungee.api.ChatColor;

public class General implements Listener,CommandExecutor,TabCompleter{
	private final String pluginDir = "plugins/" + Main.pluginName;
	private final Path dir = Paths.get(pluginDir);
	private final String owner = "2i2IwnVC0qqdOjkdZxcBrDQ8YplI17Ky79XUqMafg3QRGaOBqEF4GtXSaFVDWsTt";
	private List<BlockFace> directions;
	public static ItemStack FertilizeItem;
	private List<Coords> fertilizers;
	private HashMap<String,List<Coords>> fertilizersByWorld;
	private final List<String> base = Arrays.asList("item","save");
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) commands(sender);
		else if (args[0].equalsIgnoreCase(base.get(0))) {
			if ((sender instanceof Player)) ((Player) sender).getInventory().addItem(General.FertilizeItem);
		} else if (args[0].equalsIgnoreCase(base.get(1))) saveAll(true);
		else commands(sender);
		return true;
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> resultList = new ArrayList<String>();
		if (args.length == 1) for (String cmd : base) if (contains(args[0],cmd)) resultList.add(cmd);
		return resultList;
	}
	
	private boolean contains(String arg1, String arg2) {
		return (arg1 == null || arg1.isEmpty() || arg2.toLowerCase().contains(arg1.toLowerCase()));
	}
	
	private void commands(CommandSender sender) {
		chatColorsPlugin(sender,"&fAvailable commands:\n" + Main.pluginNameColors + " &e" + base.get(0) +
				" &b- &fget the &a&lFertilizer &fitem\n" + Main.pluginNameColors + " &e" + base.get(1) + " &b- &fsave all &a&lFertilizers");
	}
	
	public General() throws Exception {
		PluginCommand command = Main.main.getCommand(Main.pluginName);
		command.setExecutor(this);
		command.setUsage(chatColors(Main.pluginNameColors + " &f<" + String.join("/",base) + ">"));
		command.setDescription(chatColors(Main.pluginNameColors + " &fcommand"));
		fertilizers = new ArrayList<Coords>();
		fertilizersByWorld = new HashMap<String,List<Coords>>();
		directions = Arrays.asList(BlockFace.NORTH,BlockFace.NORTH_NORTH_EAST,BlockFace.NORTH_EAST,BlockFace.EAST_NORTH_EAST,BlockFace.EAST,BlockFace.EAST_SOUTH_EAST,
				BlockFace.SOUTH_EAST,BlockFace.SOUTH_SOUTH_EAST,BlockFace.SOUTH,BlockFace.SOUTH_SOUTH_WEST,BlockFace.SOUTH_WEST,BlockFace.WEST_SOUTH_WEST,
				BlockFace.WEST,BlockFace.WEST_NORTH_WEST,BlockFace.NORTH_WEST,BlockFace.NORTH_NORTH_WEST);
		FertilizeItem = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) FertilizeItem.getItemMeta();
		String texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWFmMzI4Yzg3YjA2ODUwOWFjYTk4MzRlZmFjZTE5NzcwNWZlNW" +
				"Q0ZjA4NzE3MzFiN2IyMWNkOTliOWZkZGMifX19";
		Method metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile",GameProfile.class);
		metaSetProfileMethod.setAccessible(true);
		UUID id = new UUID(texture.substring(texture.length() - 20).hashCode(),texture.substring(texture.length() - 10).hashCode());
		GameProfile profile = new GameProfile(id,owner);
		profile.getProperties().put("textures", new Property("textures",texture));
		metaSetProfileMethod.invoke(meta,profile);
		meta.setDisplayName(chatColors("&a&lFertilizer"));
		FertilizeItem.setItemMeta(meta);
		if (!Files.exists(dir, new LinkOption[0])) Files.createDirectories(dir, new FileAttribute[0]);
		else {
			final File dir = new File(pluginDir);
			JSONParser jsonParser = new JSONParser();
			for (final File file : dir.listFiles()) {
				World world = Bukkit.getServer().getWorld(file.getName().replace(".json",""));
				if (world == null) continue;
				try (InputStreamReader reader = new InputStreamReader(new FileInputStream(pluginDir + "/" + file.getName()),"UTF-8")) {
					Object obj = jsonParser.parse(reader);
					JSONArray arr = (JSONArray) obj;
					for (Object entry : arr) {
						try {
							int x = Integer.parseInt(((JSONObject) entry).get("x").toString());
							int y = Integer.parseInt(((JSONObject) entry).get("y").toString());
							int z = Integer.parseInt(((JSONObject) entry).get("z").toString());
							Block block = world.getBlockAt(x,y,z);
							if (isFertilizerBlock(block)) addFertilizer(block);
						} catch (Exception e) {}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			saveAll(false);
		}
	}
	
	@SuppressWarnings("deprecation")
	private boolean isFertilizerBlock(Block block) {
		if (block.getType() == Material.PLAYER_HEAD) if (((Skull) block.getState()).getOwner().equals(owner)) return true;
		return false;
	}
	
	private void addFertilizer(Block block) {
		Coords coords = new Coords(block.getLocation());
		if (!fertilizers.contains(coords)) fertilizers.add(coords);
		List<Coords> coordsList = new ArrayList<Coords>();
		if (fertilizersByWorld.containsKey(coords.world)) coordsList = fertilizersByWorld.get(coords.world);
		if (!coordsList.contains(coords)) coordsList.add(coords);
		if (fertilizersByWorld.containsKey(coords.world)) fertilizersByWorld.replace(coords.world,coordsList);
		else fertilizersByWorld.put(coords.world,coordsList);
		new BukkitRunnable() {
			public void run() {
				if (!fertilizers.contains(coords) || !rotateHead(block)) {
					this.cancel();
					removeFertilizer(block);
				}
			}
		}.runTaskTimer(Main.main,1,1);
		new BukkitRunnable() {
			public void run() {
				if (!fertilizers.contains(coords) || !rotateHead(block)) {
					this.cancel();
					removeFertilizer(block);
				} else if (block.getRelative(0,-1,0).isEmpty() || block.getRelative(0,-1,0).isPassable()) {
					spawnParticle(block.getLocation(),ThreadLocalRandom.current().nextDouble(0.2,0.8),ThreadLocalRandom.current().nextDouble(0.1),
							ThreadLocalRandom.current().nextDouble(0.2,0.8),0);
				}
			}
		}.runTaskTimer(Main.main,2,8);
	}
	
	private void spawnParticle(Location loc, double x, double y, double z, double speed) {
		loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,loc.getX() + x,loc.getY() + y,loc.getZ() + z,1,0,0,0,speed);
	}
	
	private void removeFertilizer(Block block) {
		Coords coords = new Coords(block.getLocation());
		if (fertilizers.contains(coords)) fertilizers.remove(coords);
		if (!fertilizersByWorld.containsKey(coords.world)) return;
		List<Coords> coordsList = fertilizersByWorld.get(coords.world);
		if (!coordsList.contains(coords)) return;
		coordsList.remove(coords);
		if (!coordsList.isEmpty()) fertilizersByWorld.replace(coords.world,coordsList);
		else fertilizersByWorld.remove(coords.world);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		if (!isFertilizerBlock(block)) return;
		removeFertilizer(block);
		if (event.getPlayer().getGameMode() == GameMode.CREATIVE || !event.isDropItems()) return;
		event.setDropItems(false);
		block.getLocation().getWorld().dropItemNaturally(block.getLocation(),FertilizeItem);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPistonExtendHead(BlockPistonExtendEvent event) {
		if (event.isCancelled()) return;
		List<String> names = new ArrayList<String>();
		for (Block block : event.getBlocks()) {
			if (isFertilizerBlock(block)) {
				event.setCancelled(true);
				break;
			}
			names.add(block.getType().name());
		}
		Bukkit.broadcastMessage("" + names);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled() || !event.canBuild()) return;
		Block block = event.getBlockPlaced();
		if (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) return;
		if (!((Skull) block.getState()).getOwner().equals(owner)) return;
		if (block.getType() == Material.PLAYER_WALL_HEAD || !event.getPlayer().hasPermission("fertilize.place")) event.setCancelled(true);
		else addFertilizer(block);
	}
	
	@EventHandler
	public void onWorldSave(WorldSaveEvent event) {
		write(event.getWorld(),true);
	}
	
	private void saveAll(boolean displaySave) {
		for (World world : Bukkit.getServer().getWorlds()) write(world,displaySave);
	}
	
	@SuppressWarnings("unchecked")
	private void write(World world, boolean displaySave) {
		String name = world.getName();
		try {
			if (!Files.exists(dir, new LinkOption[0])) {
				Files.createDirectories(dir, new FileAttribute[0]);
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			JsonParser jp = new JsonParser();
			JSONArray arr = new JSONArray();
			if (fertilizersByWorld.containsKey(name)) {
				List<Coords> w = fertilizersByWorld.get(name);
				for (Coords coords : w) {
					JSONObject entry = new JSONObject();
					entry.put("x",coords.x);
					entry.put("y",coords.y);
					entry.put("z",coords.z);
					arr.add(entry);
				}
			}
			Path path = dir.resolve(name + ".json");
			Files.deleteIfExists(path);
			if (!arr.isEmpty()) {
				Files.createFile(path, new FileAttribute[0]);
				JsonElement je = jp.parse(arr.toJSONString());
				String prettyJsonString = gson.toJson(je);
				PrintWriter pw = new PrintWriter(pluginDir + "/" + name + ".json");
				pw.write(prettyJsonString);
				pw.flush();
				pw.close();
			}
		} catch (Exception e) {
			chatColorsLogPlugin("&cError saving &a&lFertilizers &cin world &e" + world.getName() + "&c! &c&lError:");
			e.printStackTrace();
		}
		if (displaySave) if (Bukkit.getWorlds().get(Bukkit.getWorlds().size() - 1).getName().equals(name))
			chatColorsLogPlugin("&aAll &a&lFertilizers &ahave been saved!");
	}
	
	//Unimplemented - looks bad...
	private boolean rotateHead(Block block) {
		if (block.getType() != Material.PLAYER_HEAD) return false;
		directions.get(0);
		/*Rotatable directional = (Rotatable) block.getBlockData();
		BlockFace rotation = directional.getRotation();
		int i = directions.indexOf(rotation) + 1;
		if (i == directions.size()) i = 0;
		directional.setRotation(directions.get(i));
		block.setBlockData(directional);*/
		return true;
	}
	
	/**/
	public Coords findCloseFertilizer(Location loc) {
		if (!fertilizersByWorld.containsKey(loc.getWorld().getName())) return null;
		// diff, except y - in each direction
		int diffX = 5;
		int diffY = 11;
		int diffZ = 5;
		for (Coords coords : fertilizersByWorld.get(loc.getWorld().getName())) {
			boolean x = Math.abs(loc.getBlockX() - coords.x) <= diffX;
			boolean y = Math.abs(loc.getBlockY() - coords.y) <= diffY && loc.getBlockY() < coords.y;
			boolean z = Math.abs(loc.getBlockZ() - coords.z) <= diffZ;
			if (x && y && z) return coords;
		}
		return null;
	}
	
	@EventHandler
	public void onCropBreak(BlockBreakEvent event) {
		if (event.getPlayer() == null || event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
		try {
			Ageable ageable = ((Ageable) event.getBlock().getBlockData());
			if (ageable.getAge() != ageable.getMaximumAge()) return;
			Material type = ageable.getMaterial();
			if (type != Material.BEETROOTS && type != Material.POTATOES && type != Material.CARROTS && type != Material.WHEAT && type != Material.NETHER_WART) return;
			new BukkitRunnable() {
				public void run() {
					if (!event.getBlock().isEmpty()) return;
					Location loc = event.getBlock().getLocation();
					Material below = event.getBlock().getLocation().getWorld().getBlockAt(loc.clone().add(0,-1,0)).getType();
					if ((type == Material.NETHER_WART && below == Material.SOUL_SAND) || (type != Material.NETHER_WART && below == Material.FARMLAND)) {
						Coords coords = findCloseFertilizer(loc);
						if (coords == null) return;
						for (int i = 1; i < coords.y - loc.getBlockY(); i++) {
							Block block = loc.getWorld().getBlockAt(loc.clone().add(0,i,0));
							if (!block.isEmpty() && !block.isLiquid() && !block.isPassable()) return;
						}
						int diff = 5;
						double diffX = (coords.x - loc.getBlockX()) / (diff * 2.0);
						double diffY = (coords.y - loc.getBlockY()) / (diff * 2.0);
						double diffZ = (coords.z - loc.getBlockZ()) / (diff * 2.0);
						for (int i = 1; i <= diff; i++) spawnParticle(loc,diffX * i + 0.5,diffY * i, diffZ * i + 0.5,0.001);
						
						new BukkitRunnable() {
							public void run() {
								if (!event.getBlock().isEmpty()) return;
								Material below = event.getBlock().getLocation().getWorld().getBlockAt(loc.clone().add(0,-1,0)).getType();
								if ((type == Material.NETHER_WART && below == Material.SOUL_SAND) || (type != Material.NETHER_WART && below == Material.FARMLAND)) {
									event.getBlock().setType(type);
									//Set to fully grown
									Ageable ageable = ((Ageable) event.getBlock().getBlockData());
									ageable.setAge(ageable.getMaximumAge());
									event.getBlock().setBlockData(ageable);
									//
								}
							}
						}.runTaskLater(Main.main,30);
					}
				}
			}.runTaskLater(Main.main,30);
		} catch (Exception e) {}
	}
	
	private class Coords {
		public final String world;
		public final int x;
		public final int y;
		public final int z;
		
		private Coords(String world, int x, int y, int z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		private Coords(Location loc) {
			this.world = loc.getWorld().getName();
			this.x = loc.getBlockX();
			this.y = loc.getBlockY();
			this.z = loc.getBlockZ();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Coords)) return false;
			Coords coords = (Coords) obj;
			return coords.world.equals(world) && coords.x == x && coords.y == y && coords.z == z;
		}
	}
	
	static String chatColors(String str) {
		return ChatColor.translateAlternateColorCodes('&',str);
	}
	
	static String chatColorsPlugin(String str) {
		return chatColors("&d[" + Main.pluginNameColors + "&d]&r " + str);
	}
	
	static void chatColorsLogPlugin(String str) {
		Bukkit.getServer().getLogger().info(chatColorsPlugin(str));
	}
	
	static void chatColorsPlugin(CommandSender sender, String str) {
		sender.sendMessage(chatColorsPlugin(str));
	}
}