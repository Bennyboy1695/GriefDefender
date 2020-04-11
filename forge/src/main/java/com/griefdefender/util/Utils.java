package com.griefdefender.util;

import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.util.text.ITextComponent;

public class Utils {

    public static ITextComponent convertCompToTextComp(Component component) {
        return ITextComponent.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(component));
    }
}
