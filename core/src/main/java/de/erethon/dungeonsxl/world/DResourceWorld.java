/*
 * Copyright (C) 2012-2022 Frank Baumann
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
package de.erethon.dungeonsxl.world;

import de.erethon.dungeonsxl.DungeonsXL;
import de.erethon.dungeonsxl.api.dungeon.Dungeon;
import de.erethon.dungeonsxl.api.dungeon.Game;
import de.erethon.dungeonsxl.api.dungeon.GameRuleContainer;
import de.erethon.dungeonsxl.api.event.world.EditWorldGenerateEvent;
import de.erethon.dungeonsxl.api.event.world.ResourceWorldInstantiateEvent;
import de.erethon.dungeonsxl.api.player.EditPlayer;
import de.erethon.dungeonsxl.api.world.EditWorld;
import de.erethon.dungeonsxl.api.world.GameWorld;
import de.erethon.dungeonsxl.api.world.ResourceWorld;
import de.erethon.bedrock.compatibility.Version;
import de.erethon.bedrock.misc.FileUtil;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

/**
 * @author Daniel Saukel
 */
public class DResourceWorld implements ResourceWorld {

    public static final File RAW = new File(DungeonsXL.MAPS, ".raw");

    private DungeonsXL plugin;

    private File folder;
    private WorldConfig config;
    private SignData signData;
    EditWorld editWorld;

    public DResourceWorld(DungeonsXL plugin, String name) {
        this.plugin = plugin;

        folder = new File(DungeonsXL.MAPS, name);
        if (!folder.exists()) {
            folder.mkdir();
        }

        File configFile = new File(folder, WorldConfig.FILE_NAME);
        if (configFile.exists()) {
            config = new WorldConfig(plugin, configFile);
        }

        signData = new SignData(new File(folder, SignData.FILE_NAME));
    }

    public DResourceWorld(DungeonsXL plugin, File folder) {
        this.plugin = plugin;

        this.folder = folder;

        File configFile = new File(folder, WorldConfig.FILE_NAME);
        if (configFile.exists()) {
            config = new WorldConfig(plugin, configFile);
        }

        signData = new SignData(new File(folder, SignData.FILE_NAME));
    }

    /* Getters and setters */
    @Override
    public String getName() {
        return folder.getName();
    }

    @Override
    public void setName(String name) {
        folder.renameTo(new File(folder.getParentFile(), name));
        folder = new File(folder.getParentFile(), name);
    }

    @Override
    public File getFolder() {
        return folder;
    }

    @Override
    public GameRuleContainer getRules() {
        return getConfig(false);
    }

    /**
     * Returns the config of this world.
     *
     * @param generate if a config should be generated if none exists
     * @return the config of this world
     */
    public WorldConfig getConfig(boolean generate) {
        if (config == null) {
            File file = new File(folder, WorldConfig.FILE_NAME);
            if (!file.exists() && generate) {
                try {
                    file.createNewFile();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            config = new WorldConfig(plugin, file);
        }

        return config;
    }

    @Override
    public Environment getWorldEnvironment() {
        return (getConfig(false) != null && getConfig(false).getWorldEnvironment() != null) ? getConfig(false).getWorldEnvironment() : Environment.NORMAL;
    }

    @Override
    public void addInvitedPlayer(OfflinePlayer player) {
        getConfig(true).addInvitedPlayer(player.getUniqueId().toString());
        config.save();
    }

    @Override
    public boolean removeInvitedPlayer(OfflinePlayer player) {
        if (config == null) {
            return false;
        }

        config.removeInvitedPlayers(player.getUniqueId().toString(), player.getName().toLowerCase());
        config.save();

        EditPlayer editPlayer = plugin.getPlayerCache().getEditPlayer(player.getPlayer());
        if (editPlayer != null) {
            if (plugin.getEditWorld(editPlayer.getWorld()).getResource() == this) {
                editPlayer.leave();
            }
        }

        return true;
    }

    @Override
    public boolean isInvitedPlayer(OfflinePlayer player) {
        if (config == null) {
            return false;
        }

        return config.getInvitedPlayers().contains(player.getName().toLowerCase()) || config.getInvitedPlayers().contains(player.getUniqueId().toString());
    }

    /* Actions */
    @Override
    public void backup() {
        File target = new File(DungeonsXL.BACKUPS, getName() + "-" + System.currentTimeMillis());
        FileUtil.copyDir(folder, target);
    }

    public DInstanceWorld instantiate(Game game) {
        plugin.setLoadingWorld(true);
        String name = DInstanceWorld.generateName(game != null);
        File instanceFolder = new File(Bukkit.getWorldContainer(), name);

        DInstanceWorld instance = game != null ? new DGameWorld(plugin, this, instanceFolder, game) : new DEditWorld(plugin, this, instanceFolder);
        ResourceWorldInstantiateEvent event = new ResourceWorldInstantiateEvent(this, name);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        FileUtil.copyDir(folder, instanceFolder, DungeonsXL.EXCLUDED_FILES);
        instance.world = Bukkit.createWorld(WorldCreator.name(name).environment(getWorldEnvironment())).getName();
        if (Version.isAtLeast(Version.MC1_13)) {
            instance.getWorld().setGameRule(GameRule.DO_FIRE_TICK, false);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("dynmap")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dynmap pause all");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dmap worldset " + name + " enabled:false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dynmap pause none");
        }

        if (game != null) {
            signData.deserializeSigns((DGameWorld) instance);
            instance.getWorld().setAutoSave(false);
        } else {
            signData.deserializeSigns((DEditWorld) instance);
        }

        plugin.setLoadingWorld(false);
        return instance;
    }

    @Override
    public EditWorld getEditWorld() {
        return editWorld;
    }

    @Override
    public EditWorld getOrInstantiateEditWorld(boolean ignoreLimit) {
        if (editWorld != null) {
            return editWorld;
        }
        if (plugin.isLoadingWorld()) {
            return null;
        }
        if (!ignoreLimit && plugin.getMainConfig().getMaxInstances() <= plugin.getInstanceCache().size()) {
            return null;
        }

        editWorld = (EditWorld) instantiate(null);
        return editWorld;
    }

    @Override
    public GameWorld instantiateGameWorld(Game game, boolean ignoreLimit) {
        if (plugin.isLoadingWorld()) {
            return null;
        }
        if (!ignoreLimit && plugin.getMainConfig().getMaxInstances() <= plugin.getInstanceCache().size()) {
            return null;
        }
        return (DGameWorld) instantiate(game);
    }

    @Override
    public Dungeon getSingleFloorDungeon() {
        return plugin.getDungeonRegistry().get(getName());
    }

    /**
     * Returns the DXLData.data file
     *
     * @return the DXLData.data file
     */
    public SignData getSignData() {
        return signData;
    }

    /**
     * Generate a new DResourceWorld.
     *
     * @return the automatically created DEditWorld instance
     */
    public DEditWorld generate() {
        String name = DInstanceWorld.generateName(false);
        File folder = new File(Bukkit.getWorldContainer(), name);
        WorldCreator creator = new WorldCreator(name);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        DEditWorld editWorld = new DEditWorld(plugin, this, folder);
        this.editWorld = editWorld;

        ResourceWorldInstantiateEvent event = new ResourceWorldInstantiateEvent(this, name);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        if (!RAW.exists()) {
            createRaw();
        }
        FileUtil.copyDir(RAW, folder, DungeonsXL.EXCLUDED_FILES);
        editWorld.generateIdFile();
        editWorld.world = creator.createWorld().getName();
        editWorld.generateIdFile();

        Bukkit.getPluginManager().callEvent(new EditWorldGenerateEvent(editWorld));
        return editWorld;
    }

    void clearFolder() {
        for (File file : FileUtil.getFilesForFolder(getFolder())) {
            if (file.getName().equals(SignData.FILE_NAME) || file.getName().equals(WorldConfig.FILE_NAME)) {
                continue;
            }
            if (file.isDirectory()) {
                FileUtil.removeDir(file);
            } else {
                file.delete();
            }
        }
    }

    /**
     * Removes files that are not needed from a world
     *
     * @param dir the directory to purge
     */
    public static void deleteUnusedFiles(File dir) {
        for (File file : dir.listFiles()) {
            if (file.getName().equalsIgnoreCase("uid.dat") || file.getName().contains(".id_")) {
                file.delete();
            }
        }
    }

    /**
     * Creates the "raw" world that is copied for new instances.
     */
    public static void createRaw() {
        WorldCreator rawCreator = WorldCreator.name(".raw");
        rawCreator.type(WorldType.FLAT);
        rawCreator.generateStructures(false);
        World world = rawCreator.createWorld();
        File worldFolder = new File(Bukkit.getWorldContainer(), ".raw");
        FileUtil.copyDir(worldFolder, RAW, DungeonsXL.EXCLUDED_FILES);
        Bukkit.unloadWorld(world, /* SPIGOT-5225 */ !Version.isAtLeast(Version.MC1_14_4));
        FileUtil.removeDir(worldFolder);
    }

    @Override
    public String toString() {
        return "DResourceWorld{name=" + getName() + "}";
    }

}
