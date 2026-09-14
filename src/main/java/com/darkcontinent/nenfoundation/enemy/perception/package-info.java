/**
 * Percepcao de inimigo: visao, audicao, memoria de ameaca e escolha de alvo.
 *
 * <p>Carrega a decisao da issue #136: percepcao NAO mora no cerebro. O
 * {@code EnemyBrain} decide intencao a partir de uma leitura pronta; quem
 * produz essa leitura e este pacote, e ele o faz com ORCAMENTO -- varredura
 * cara tem intervalo declarado, e o portao reprova quem varrer todo tick.</p>
 *
 * <p>Nada aqui varre o mundo por conta propria. O acesso a mundo entra por
 * {@link com.darkcontinent.nenfoundation.enemy.perception.SensorDeVisao}, que a
 * entidade implementa; assim a regra e testavel sem servidor e o unico lado que
 * pode chama-la e o autoritativo.</p>
 */
package com.darkcontinent.nenfoundation.enemy.perception;
