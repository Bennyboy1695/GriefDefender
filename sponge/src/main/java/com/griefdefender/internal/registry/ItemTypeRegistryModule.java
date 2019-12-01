/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.internal.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.item.ItemType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ItemTypeRegistryModule {

    protected final Map<String, GDItemType> itemTypeMappings = new HashMap<>();
    private final org.spongepowered.common.registry.type.ItemTypeRegistryModule SPONGE_REGISTRY = org.spongepowered.common.registry.type.ItemTypeRegistryModule.getInstance();

    public static ItemTypeRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    public Optional<GDItemType> getById(String id) {
        if (!checkNotNull(id).contains(":")) {
            id = "minecraft:" + id;
        }
        return Optional.ofNullable(this.itemTypeMappings.get(id.toLowerCase(Locale.ENGLISH)));
    }

    public Collection<GDItemType> getAll() {
        return ImmutableList.copyOf(this.itemTypeMappings.values());
    }

    public void registerDefaults() {
        for (ItemType type : SPONGE_REGISTRY.getAll()) {
            this.itemTypeMappings.put(type.getId(), new GDItemType(type));
        }
    }

    private static final class Holder {

        static final ItemTypeRegistryModule INSTANCE = new ItemTypeRegistryModule();
    }
}