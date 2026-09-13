package com.darkcontinent.nenfoundation.registry;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

/** Conteúdo estático do livro, separado do bootstrap do registro para ser testável. */
final class BestiarioContent {
    private BestiarioContent() { }

    static WrittenBookContent conteudo() {
        return new WrittenBookContent(
                Filterable.passThrough("Bestiário da Associação Hunter"),
                "Associação Hunter", 0,
                List.of(
                        pagina("BESTIÁRIO HUNTER\n\nRegistro de campo das criaturas encontradas no continente desconhecido.\n\nLeia o ambiente antes de atacar: comportamento, terreno e sinais de presença são parte da defesa."),
                        pagina("GREAT STAMP\n\nComportamento: predador terrestre de investida. Mantém distância curta e avança em linha; a janela segura é antes da carga ou após errar o golpe.\n\nOnde aparece: regiões abertas e secas, especialmente campos e áreas pedregosas.\n\nAnime: espécie apresentada durante a expedição ao Continente Negro, em Hunter × Hunter (2011)."),
                        pagina("FOXBEAR\n\nComportamento: criatura territorial de patrulha. Aproxima-se para pressionar o alvo e usa o corpo pesado para controlar espaço.\n\nOnde aparece: florestas e bordas de biomas naturais. Procure pegadas e clareiras marcadas.\n\nAnime: fauna de referência do Continente Negro; o comportamento deste registro é uma interpretação de jogo."),
                        pagina("FROG-IN-WAITING\n\nComportamento: emboscador enterrado. Permanece oculto e emerge quando uma presa entra no raio de ataque.\n\nOnde aparece: margens úmidas, pântanos e solo com água próxima.\n\nAnime: criatura inspirada na fauna hostil do Continente Negro; esta variação é conteúdo original do mod."),
                        pagina("MASTER OF THE SWAMP\n\nComportamento: predador aquático de grande porte. A água profunda é seu território; puxar ou enfrentar a criatura exige espaço para reagir.\n\nOnde aparece: água de pântanos e zonas alagadas.\n\nAnime: baseado no monstro aquático mostrado no exame Hunter em Hunter × Hunter (2011); o nome do registro é uma adaptação do mod."),
                        pagina("KIRIKO\n\nComportamento: criatura de disfarce. Pode parecer humana à distância; observe movimentação, contexto e reação antes de se aproximar.\n\nOnde aparece: trilhas, florestas e áreas onde viajantes passam.\n\nAnime: as Kiriko aparecem no exame Hunter de Hunter × Hunter (2011)."),
                        pagina("MAN-FACED APE\n\nComportamento: imitador e emboscador. Usa silhueta humanoide para reduzir suspeitas e revela sua natureza quando o alvo perde a cautela.\n\nOnde aparece: selvas e regiões de vegetação densa.\n\nAnime: inspirado nos animais perigosos do exame Hunter; esta implementação é uma leitura original do mod."),
                        pagina("SPIDER EAGLE\n\nComportamento: caçador aéreo. Usa altura e mergulhos para atacar; terreno aberto ajuda a perceber a aproximação.\n\nOnde aparece: cânions, penhascos e biomas com espaço vertical.\n\nAnime: fauna inspirada no Continente Negro; o mob e suas regras são conteúdo original do mod."),
                        pagina("NOTA DO REGISTRO\n\nAs aparições no anime indicam a inspiração ou a espécie de referência, não uma cópia de cenas.\n\nDica de campo: use este livro junto dos sinais do ambiente. O spawn depende do bioma e das regras do servidor; um registro não substitui reconhecimento local.")),
                true);
    }

    private static Filterable<Component> pagina(String texto) {
        return Filterable.passThrough(Component.literal(texto));
    }
}
