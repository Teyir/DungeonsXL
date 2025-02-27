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
package de.erethon.dungeonsxl.mob;

import de.erethon.dungeonsxl.api.mob.ExternalMobProvider;
import java.util.Map.Entry;

/**
 * A custom external mob provider like defined in the main config file.
 *
 * @author Daniel Saukel
 */
public class CustomExternalMobProvider implements ExternalMobProvider {

    private String identifier;
    private String command;

    public CustomExternalMobProvider(String identifier, String command) {
        this.identifier = identifier;

        if (command.startsWith("/")) {
            command = command.replaceFirst("/", "");
        }
        this.command = command;
    }

    public CustomExternalMobProvider(Entry<String, Object> entry) {
        this(entry.getKey(), (String) entry.getValue());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getRawCommand() {
        return command;
    }

}
