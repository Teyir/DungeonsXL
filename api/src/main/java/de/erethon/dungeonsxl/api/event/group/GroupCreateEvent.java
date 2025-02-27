/*
 * Copyright (C) 2014-2022 Daniel Saukel
 *
 * This library is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNULesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.erethon.dungeonsxl.api.event.group;

import de.erethon.dungeonsxl.api.player.GlobalPlayer;
import de.erethon.dungeonsxl.api.player.PlayerGroup;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Fired when a group is created explicitly or implicitly.
 *
 * @author Daniel Saukel
 */
public class GroupCreateEvent extends GroupEvent implements Cancellable {

    /**
     * The reason why the group is created.
     */
    public enum Cause {

        ANNOUNCER,
        COMMAND,
        /**
         * When a group is created to mirror the state of a party plugin.
         *
         * @see de.erethon.dungeonsxl.api.player.GroupAdapter
         */
        GROUP_ADAPTER,
        GROUP_SIGN,
        TUTORIAL,
        /**
         * When a group is created by an addon.
         */
        CUSTOM

    }

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private GlobalPlayer creator;
    private Cause cause;

    public GroupCreateEvent(PlayerGroup group, GlobalPlayer creator, Cause cause) {
        super(group);
        this.creator = creator;
        this.cause = cause;
    }

    /**
     * Returns the player who created the group.
     *
     * @return the player who created the group
     */
    public GlobalPlayer getCreator() {
        return creator;
    }

    /**
     * Returns the cause for the group creation.
     *
     * @return the cause for the group creation
     */
    public Cause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{group=" + group + "; creator=" + creator + "; cause=" + cause + "}";
    }

}
