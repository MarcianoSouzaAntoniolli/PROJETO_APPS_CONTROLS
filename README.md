# 📱 ScanProduto — App Android de Leitura de Produtos

App Android profissional para leitura de produtos via código de barras (EAN-13 / EAN-8), com consulta em API REST, PostgreSQL ou arquivo TXT local.

---

## 🗂 Estrutura de Pastas

```
ScanProduto/
├── app/
│   └── src/main/
│       ├── java/com/scanproduto/
│       │   ├── ScanProdutoApp.kt              # Application class
│       │   ├── model/
│       │   │   ├── Produto.kt                 # Entidade de dados
│       │   │   ├── Resource.kt                # Estado genérico (Loading/Success/Error)
│       │   │   └── AppConfig.kt               # Config de conexão
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── ProdutoApiService.kt   # Interface Retrofit (endpoints)
│       │   │   │   └── RetrofitClient.kt      # Configuração do cliente HTTP
│       │   │   ├── db/
│       │   │   │   ├── AppDatabase.kt         # Banco Room (cache local)
│       │   │   │   ├── ProdutoDao.kt          # DAO do Room
│       │   │   │   └── PostgresDataSource.kt  # Conexão JDBC ao PostgreSQL
│       │   │   ├── local/
│       │   │   │   ├── TxtLocalDataSource.kt  # Leitura de arquivo TXT
│       │   │   │   └── PreferencesManager.kt  # SharedPreferences
│       │   │   └── repository/
│       │   │       └── ProdutoRepository.kt   # Orquestra todas as fontes
│       │   ├── ui/
│       │   │   ├── main/
│       │   │   │   ├── MainActivity.kt        # Tela principal + scanner
│       │   │   │   └── MainViewModel.kt       # ViewModel da tela principal
│       │   │   ├── settings/
│       │   │   │   └── SettingsActivity.kt    # Tela de configurações
│       │   │   └── product/
│       │   │       └── ImportActivity.kt      # Tela de importação TXT
│       │   └── utils/
│       │       ├── FeedbackUtils.kt           # Vibração / feedback
│       │       └── FormatUtils.kt             # Formatação de moeda/estoque
│       ├── res/
│       │   ├── layout/                        # XMLs das telas
│       │   ├── drawable/                      # Ícones vetoriais
│       │   ├── menu/                          # Menu da toolbar
│       │   ├── values/                        # strings, colors, themes
│       │   └── xml/                           # network_security_config
│       └── AndroidManifest.xml
├── mock_server.py                             # Servidor mock Python/Flask
├── produtos_exemplo.txt                       # Arquivo TXT de exemplo
└── README.md
```

---

## 🚀 Como Rodar

### 1. Abrir no Android Studio

1. Abra o **Android Studio** (Hedgehog ou superior recomendado)
2. Clique em **"Open"** e selecione a pasta `ScanProduto/`
3. Aguarde o Gradle sincronizar as dependências
4. Conecte um dispositivo Android (API 24+) ou use o emulador
5. Clique em **▶ Run**

> **Nota:** O emulador não tem suporte a câmera real para leitura de barcode. Use um dispositivo físico para testar o scanner.

---

### 2. Rodar o Servidor Mock (API simulada)

```bash
# Instala Flask
pip install flask

# Inicia o servidor (porta 8080)
python mock_server.py
```

O servidor ficará em: `http://SEU_IP:8080`

**Descobrir seu IP local:**
- Windows: `ipconfig` → IPv4 Address
- Mac/Linux: `ifconfig` ou `ip a`

**No app Android:**
- Acesse **≡ Menu → Configurações**
- URL Base da API: `http://192.168.X.X:8080/`
- Salve e teste a conexão

---

### 3. Endpoints disponíveis no Mock

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/produto/{ean}` | Busca por EAN |
| GET | `/produto/codigo/{codigo}` | Busca por código interno |
| GET | `/produtos?page=0&size=50` | Lista paginada |
| POST | `/produto` | Cadastra produto |
| PUT | `/produto/{ean}` | Atualiza produto |
| POST | `/produtos/batch` | Importação em lote |

**Exemplo cURL:**
```bash
curl http://localhost:8080/produto/7891234567890
```

---

### 4. Testar com arquivo TXT

1. Copie o arquivo `produtos_exemplo.txt` para o celular (via cabo ou Google Drive)
2. No app, acesse **≡ Menu → Importar TXT**
3. Toque em **Selecionar Arquivo TXT** e escolha o arquivo
4. Toque em **Importar para Cache**
5. Os produtos ficarão disponíveis offline

**Formato do arquivo:**
```
EAN;CODIGO;DESCRICAO;ESTOQUE;PRECO
7891234567890;001;Arroz 5kg;100;29.90
```

---

### 5. Configurar PostgreSQL

1. Acesse **≡ Menu → Configurações**
2. Fonte de dados: **Banco PostgreSQL**
3. Preencha: Host, Porta (5432), Nome do banco, Usuário, Senha
4. Toque em **Testar Conexão**

**Estrutura esperada da tabela:**
```sql
CREATE TABLE produtos (
    ean         VARCHAR(20) PRIMARY KEY,
    codigo      VARCHAR(50) NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    estoque     INTEGER DEFAULT 0,
    preco       DECIMAL(10,2) DEFAULT 0.00
);

-- Dados de exemplo
INSERT INTO produtos VALUES
('7891234567890', '001', 'Arroz Branco 5kg', 50, 29.90),
('7890000000001', '002', 'Feijão Carioca 1kg', 8, 8.50);
```

> ⚠️ **Atenção:** Conexão JDBC direta no Android é para uso em redes internas/intranet. Para produção externa, use sempre uma API intermediária.

---

## ⚙️ Configurações de Build

### Alterar URL padrão da API

Em `app/build.gradle`, ajuste:
```groovy
buildConfigField "String", "API_BASE_URL", '"http://SEU_IP:8080/"'
```

---

## 📦 Dependências principais

| Biblioteca | Versão | Uso |
|-----------|--------|-----|
| Retrofit | 2.9.0 | Cliente HTTP para API REST |
| OkHttp | 4.12.0 | HTTP + logging |
| Room | 2.6.1 | Cache SQLite local |
| CameraX | 1.3.1 | Preview da câmera |
| ML Kit Barcode | 17.2.0 | Leitura EAN-13/EAN-8 |
| PostgreSQL JDBC | 42.6.0 | Conexão direta ao banco |
| Coroutines | 1.7.3 | Assincronismo |
| Gson | 2.10.1 | Serialização JSON |
| Material | 1.11.0 | UI components |

---

## 🏗 Arquitetura

```
UI (Activity/Fragment)
    ↓ observa LiveData
ViewModel
    ↓ chama
Repository
    ├── API REST (Retrofit)
    ├── PostgreSQL (JDBC)
    ├── Arquivo TXT (FileReader)
    └── Cache Room (SQLite)
```

Padrão **MVVM** com **Repository Pattern** e **Resource sealed class** para gerenciar estados.

---

## 📱 Suporte a Coletores Android

Coletores Android (Zebra, Honeywell, Datalogic, etc.) enviam o código de barras como **teclado virtual (HID)**. O app suporta isso via:

- O campo `etCodigo` recebe o código digitado pelo scanner
- O `imeOptions="actionSearch"` confirma automaticamente ao pressionar Enter
- Use `android:inputType="number"` para aceitar apenas números

Para coletores com broadcast (ex: Zebra DataWedge), adicione em `MainActivity`:
```kotlin
// Registrar BroadcastReceiver para receber intent do DataWedge
val filter = IntentFilter("com.symbol.datawedge.api.RESULT_ACTION")
registerReceiver(scannerReceiver, filter)
```

---

## 🔒 Permissões necessárias

| Permissão | Motivo |
|-----------|--------|
| `CAMERA` | Scanner via câmera |
| `INTERNET` | API REST e PostgreSQL |
| `VIBRATE` | Feedback ao ler código |
| `READ_MEDIA_IMAGES` | Seleção de arquivo |
