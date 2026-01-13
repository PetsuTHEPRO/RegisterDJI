# Diagnóstico: Erro ao Carregar Missão

## Erro Reportado
```
DJIMissionException: Erro ao carregar missão: The execution could not be executed.
```

**Localização:** `DroneMissionManager.prepareAndUploadMission()` - Linha 167 (carregamento da missão)

---

## 🔍 Possíveis Causas

### 1. **DRONE NÃO CONECTADO** (Mais Provável) ⚠️
- **Sintoma:** Erro "The execution could not be executed"
- **Causa:** O WaypointMissionOperator não está inicializado porque o drone não está conectado
- **Solução:**
  ```
  ✓ Certifique-se que o drone está ligado
  ✓ Verifique se o controle está conectado via USB ou WiFi
  ✓ Aguarde a conexão ser estabelecida (veja os logs)
  ✓ Verifique permissões USB (pode exigir autorização no dispositivo)
  ```

### 2. **PARÂMETROS DA MISSÃO INVÁLIDOS**
- **Sintoma:** Missão construída mas não é aceita pelo drone
- **Verificar:**
  ```
  ✓ Altitudes: ${DroneConstants.MIN_ALTITUDE}m a ${DroneConstants.MAX_ALTITUDE}m
  ✓ Velocidade automática: 1-20 m/s
  ✓ Velocidade máxima: >= velocidade automática
  ✓ Waypoints válidos: lat (-90 a 90), lng (-180 a 180)
  ✓ Número de waypoints: >= 2
  ```

### 3. **ENUMS INVÁLIDOS NA MISSÃO**
- **Sintoma:** Valores como `finished_action`, `heading_mode`, `flight_path_mode` não reconhecidos
- **Causa:** Dados do servidor não correspondem aos valores esperados pelo DJI SDK
- **Solução:** Verificar no servidor os valores válidos:
  ```
  finished_action: NO_ACTION, GO_HOME, LAND, etc.
  heading_mode: AUTO, USING_INITIAL_DIRECTION, POINTING_TOWARDS_POINT_OF_INTEREST, etc.
  flight_path_mode: NORMAL, CURVED, etc.
  ```

### 4. **DRONE EM ESTADO INVÁLIDO**
- **Sintoma:** Drone conectado mas não pronto para receber missões
- **Verificar:**
  ```
  ✓ Bateria suficiente (geralmente > 25%)
  ✓ Drone não está em VOO
  ✓ Drone não está em modo de espera (Standby)
  ✓ GPS inicializado (se necessário para a missão)
  ✓ Giroscópio calibrado
  ```

### 5. **WAYPOINTS MAU FORMATADOS**
- **Sintoma:** Dados de waypoint não conseguem ser extraídos
- **Verificar:**
  ```
  ✓ Cada waypoint tem: latitude, longitude, altitude
  ✓ Tipos de dados corretos (Double, não String)
  ✓ Nenhum waypoint duplicado no mesmo local
  ✓ Altitude consistente (não alternando muito)
  ```

---

## 📊 Diagnosticar com Logs Melhorados

### Novos Logs Adicionados
Com as melhorias implementadas, você verá agora:

```
I: 🚀 Iniciando preparação de missão: [Nome da Missão]
D: 📍 Validando 5 waypoints...
D: ✅ 5 waypoints válidos após filtragem
D: ⚙️ Validando parâmetros de voo...
D: ✅ Parâmetros de voo validados
D: 🔧 Construindo missão DJI...
D: 🔧 Configurando missão: finishedAction=..., heading=..., flightPath=...
D: ✅ Missão construída: 5 waypoints
D: 📤 Carregando missão no operador...
E: ❌ Erro ao carregar missão no drone: [ERRO DO DJI SDK] (Código: [XXX])
```

### Como Verificar
1. **Abra Logcat no Android Studio:** `View > Tool Windows > Logcat`
2. **Filtre por:** `DroneMissionManager`
3. **Observe a sequência de logs** para identificar em qual etapa falha

---

## 🛠️ Passos para Resolver

### Passo 1: Verificar Conexão do Drone
```bash
# Nos logs, procure por:
D: ✅ Drone conectado!
D: ✅ Product inicializado
# Se não vir esses logs, o drone NÃO está conectado
```

### Passo 2: Validar Estrutura da Missão
```bash
# Nos logs, procure por:
D: ✅ 5 waypoints válidos após filtragem
D:   ✓ Waypoint #1: lat=..., lng=..., alt=...m
# Se vir menos waypoints que o esperado, algum foi filtrado por ser inválido
```

### Passo 3: Verificar Construção
```bash
# Nos logs, procure por:
D: 🔧 Configurando missão: finishedAction=..., heading=..., flightPath=...
# Se vir valores "padrão" (NO_ACTION, AUTO, NORMAL), significa que o servidor
# enviou valores inválidos e foram corrigidos automaticamente
```

### Passo 4: Verificar o Código do Erro DJI
```bash
# O erro agora mostra: "Código: [XXX]"
# Valores comuns:
# Código 1: Falha geral - drone não conectado ou em estado inválido
# Código 2: Parâmetros inválidos
# Código 3: Estado do drone não permite operação
```

---

## 📝 Relatório Recomendado

Se o erro persistir, colete:

1. **Logs completos** (captura de tela de toda a sequência de logs)
2. **Estado do drone:**
   - Bateria %
   - Modo (Manual, P-GPS, etc.)
   - Satélites/GPS fixo?
3. **Dados da missão:**
   - Número de waypoints
   - Altitudes (min/max)
   - Distância total
4. **Informações do dispositivo:**
   - Modelo do drone
   - Versão do firmware
   - Versão do DJI SDK

---

## ✅ Checklist de Resolução

- [ ] Drone está ligado e visível no sistema
- [ ] Controle remoto está conectado
- [ ] USB/WiFi conectado ao dispositivo Android
- [ ] Bateria do drone > 25%
- [ ] GPS inicializado (se necessário)
- [ ] Verificar logs de "Drone conectado"
- [ ] Verificar número de waypoints válidos
- [ ] Verificar valores dos enums no servidor
- [ ] Tentar com uma missão simples (2-3 waypoints)
- [ ] Resetar drone e tentar novamente

