package com.darkcontinent.nenfoundation.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;

/**
 * Gerador opt-in de fixtures descartaveis para cliente real. Nunca sobrescreve
 * playerdata: usa somente o mundo novo run/server/m2-qa. Nao entra no JAR do mod.
 */
public final class PrepararQaM2 {
    public static void main(String[] args) throws Exception {
        Path raiz = Path.of("").toAbsolutePath().normalize();
        Path destino = raiz.resolve("run/server/m2-qa/playerdata");
        var perfil = TagParser.parseTag(Files.readString(raiz.resolve(
                "src/test/resources/saves/v1/perfil-completo.snbt"))
                .replaceAll("(?m)//.*$", ""));
        for (String nome : new String[]{"Gon", "Kurapika"}) {
            var tag = NbtIo.readCompressed(raiz.resolve("src/test/resources/saves/playerdata/dev-v1.dat"),
                    NbtAccounter.unlimitedHeap());
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nome).getBytes(StandardCharsets.UTF_8));
            Path arquivo = destino.resolve(id + ".dat");
            if (Files.exists(arquivo)) throw new IllegalStateException("fixture ja existe: " + arquivo);
            tag.putUUID("UUID", id);
            // A fixture congelada ja e desperta. So a copiamos para um mundo descartavel.
            tag.getCompound("neoforge:attachments").put("nenfoundation:nen_persistente", perfil.copy());
            tag.putString("Dimension", "minecraft:overworld");
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(0)); pos.add(DoubleTag.valueOf(-60)); pos.add(DoubleTag.valueOf(0));
            tag.put("Pos", pos);
            Files.createDirectories(destino);
            NbtIo.writeCompressed(tag, arquivo);
            System.out.println("Fixture de QA criada: " + arquivo);
        }
    }
}
