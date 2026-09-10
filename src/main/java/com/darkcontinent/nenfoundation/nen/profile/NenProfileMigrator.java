package com.darkcontinent.nenfoundation.nen.profile;

/**
 * Traz um {@link PersistentNenData} lido do disco para o schema atual.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. O migrador existe desde a v1, quando ainda nao ha nada para migrar. Ele
 * nasce vazio de proposito. Criar o versionamento depois que saves reais
 * existem obriga a adivinhar de que versao cada save veio, e adivinhar errado
 * corrompe progresso sem uma linha no log.
 *
 * <p>2. Save de versao FUTURA e recusado com excecao, nao aceito "na
 * esperanca". Um mundo aberto por um JAR mais antigo que o save silenciosamente
 * regrava os campos que o JAR antigo nao conhece — o jogador perde progresso e
 * nada acusa. Recusar barulhento custa uma tarde; aceitar custa o mundo dele.
 *
 * <p>3. Cada degrau de migracao e uma funcao propria e testada em
 * {@code src/test/.../nen/profile}. Nao existe "migracao generica".
 */
public final class NenProfileMigrator {

    private NenProfileMigrator() {
    }

    /**
     * Devolve o dado no schema atual.
     *
     * @throws SchemaDoFuturoException se o save foi escrito por uma versao do
     *     mod mais nova que esta
     */
    public static PersistentNenData migrar(PersistentNenData lido) {
        int versao = lido.schemaVersion();

        if (versao > PersistentNenData.SCHEMA_ATUAL) {
            throw new SchemaDoFuturoException(versao, PersistentNenData.SCHEMA_ATUAL);
        }
        if (versao < 1) {
            // Schema 0 nunca foi publicado. Um save que chega com 0 nao veio de
            // uma versao antiga: veio de um campo ausente ou de NBT corrompido.
            // Tratar como v1 aqui esconderia o defeito.
            throw new SchemaInvalidoException(versao);
        }

        // Degraus de migracao entram AQUI, em ordem crescente e um por linha:
        //   if (versao < 2) { lido = de1Para2(lido); versao = 2; }

        return versao == PersistentNenData.SCHEMA_ATUAL
                ? lido
                : new PersistentNenData(
                        PersistentNenData.SCHEMA_ATUAL,
                        lido.awakened(), lido.category(), lido.categoryRevealed(),
                        lido.auraPotential(), lido.control(), lido.output(),
                        lido.techniqueProficiency(), lido.unlockedTechniques(),
                        lido.unlockedAbilities(), lido.progressionFlags());
    }

    /** Save escrito por uma versao do mod mais nova que a que esta rodando. */
    public static final class SchemaDoFuturoException extends IllegalStateException {
        public SchemaDoFuturoException(int encontrado, int suportado) {
            super("Save de Nen na versao de schema " + encontrado
                    + ", mas este JAR so entende ate " + suportado
                    + ". Atualize o mod em vez de abrir o mundo: abrir agora"
                    + " apagaria os campos que esta versao nao conhece.");
        }
    }

    /** Versao de schema que nunca existiu. */
    public static final class SchemaInvalidoException extends IllegalStateException {
        public SchemaInvalidoException(int encontrado) {
            super("Versao de schema de Nen invalida: " + encontrado
                    + ". Nenhuma versao publicada usou este numero;"
                    + " o dado esta corrompido ou veio de outra fonte.");
        }
    }
}
