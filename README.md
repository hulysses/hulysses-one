# Hulysses One

Mini ERP de parceiros de negócio, produtos e pedidos, desenvolvido com Java 21, Spring Boot 4.1.0,
Spring Data JPA e PostgreSQL. As etapas abaixo registram a evolução do projeto.

```text
hulysses-one/
├── pom.xml                     
├── hulysses-app/                produtos, pedidos e integração Feign
├── business-partner-service/    cadastro de parceiros
├── config-server/               configurações centralizadas
├── compose.yml
├── .env.example
└── .gitignore / .dockerignore
```

<details>
<summary><strong>Etapa 1 — Organização do monólito modular</strong></summary>

## Arquitetura e responsabilidades

Nesta etapa, o sistema era uma única aplicação Spring Boot, organizada por domínio:

```text
Cliente HTTP → Controller → Service → Repository → PostgreSQL
```

| Módulo | Responsabilidade |
| --- | --- |
| `businesspartner` | Pessoas/empresas, documentos, contatos, endereços e papéis CUSTOMER, SUPPLIER, EMPLOYEE e USER. |
| `product` | Produtos, preço, situação e fornecedor. |
| `sales` | Pedidos, cliente, itens, quantidades e total. |
| `shared` | Exceções, resposta de erro e configuração OpenAPI. |

Cada domínio usa pacotes `presentation`, `application`, `domain` e `persistence`.
Controllers validam DTOs e chamam services; services coordenam regras e transações;
repositories acessam apenas os dados de seu domínio. Entidades JPA não são contratos HTTP.

Produtos dependiam de parceiros fornecedores; pedidos dependiam de parceiros clientes e produtos.
As chamadas eram locais, com relações JPA: parceiro/endereço, produto/fornecedor,
pedido/cliente e pedido/itens/produto. Endereços pertencem ao agregado de parceiros.

## API, validação e erros

| Recurso | Operações e consultas |
| --- | --- |
| `/business-partners` | CRUD; filtros `/customers`, `/suppliers` e `/search?name=Maria`. |
| `/products` | CRUD; filtros `/active` e `/ordered-by-price`. |
| `/sales-orders` | CRUD; consultas `/customer/{customerId}`, `/status/{status}` e `/total`. |

CRUD: POST na coleção; GET na coleção ou `/{id}`; PUT/DELETE em `/{id}`.
POST retorna 201, GET/PUT 200 e DELETE 204. As consultas usam métodos Spring Data JPA;
o total de vendas usa JPQL com SUM/COALESCE.

Bean Validation com `@Valid` verifica campos obrigatórios, e-mail, CPF/CNPJ conforme o tipo,
CEP de oito dígitos, preço positivo e quantidades positivas. Pedidos exigem itens válidos;
produtos/pedidos verificam os papéis de fornecedor/cliente.

`GlobalExceptionHandler` centraliza o contrato `ApiError`:
`timestamp`, `status`, `error`, `message`, `path` e `fields`.
Respostas: 400 para entrada/regra inválida, 404 para recurso inexistente, 409 para conflito de
integridade e 500 com mensagem genérica. Erros técnicos e stack traces não são enviados ao cliente.

Swagger: http://localhost:8080/swagger-ui.html. OpenAPI: `/v3/api-docs`.
Testes cobrem API, regras, consultas, validações e tratamento de erros.

## Candidato a serviço independente

**Cadastro de parceiros de negócio:** possui ciclo de vida próprio e atende produtos e vendas.
Uma API poderia fornecer cadastro e elegibilidade por papel, usando IDs e DTOs de identificação,
contatos, endereços e papéis. Esse candidato foi extraído na Etapa 2.

</details>

<details>
<summary><strong>Etapa 2 — Primeiro serviço independente</strong></summary>

## Serviço extraído e arquitetura

O `business-partner-service` passou a concentrar cadastro, endereços, papéis, validações e persistência
de parceiros. O `hulysses-app` mantém produtos e pedidos, além da fachada `/business-partners`.
São duas aplicações independentes no mesmo repositório, cada uma com POM, bootstrap e configuração.

```text
Cliente HTTP → Hulysses App :8080
                  Controller → Service → BusinessPartnerClient (OpenFeign)
                                                   ↓ HTTP/JSON
                                Partner Service :8081
                                Controller → Service → Repository
```

Controllers não chamam Feign diretamente. `BusinessPartnerService` do principal coordena as chamadas;
as regras de cadastro/elegibilidade ficam somente no serviço extraído.

## Contrato HTTP e configuração

| Endpoint do serviço de parceiros | Contrato / resultado |
| --- | --- |
| `POST /business-partners` | `BusinessPartnerRequest` → `BusinessPartnerResponse`; 201. |
| `PUT /business-partners/{id}` | Mesmo request/response; 200. |
| `GET /business-partners/{id}` | Consulta por ID; `BusinessPartnerResponse`; 200. |
| `GET /business-partners/{id}/eligibility?role=SUPPLIER` | Valida fornecedor de produto; mesma response; 200. |
| `GET /business-partners/{id}/eligibility?role=CUSTOMER` | Valida cliente de pedido; mesma response; 200. |
| `GET /business-partners` e filtros | Listas de `BusinessPartnerResponse`; 200. |
| `DELETE /business-partners/{id}` | Exclusão do agregado; 204. |

Endereços usam `AddressRequest`/`AddressResponse`. Cada aplicação possui seus DTOs HTTP;
nenhuma compartilha entidades JPA. Bean Validation roda no serviço de parceiros.
Produtos/pedidos guardam `supplier_id`/`customer_id` como referências escalares.

O `BusinessPartnerClient` usa `@FeignClient` com `${services.business-partner.url}`,
definida por `BUSINESS_PARTNER_SERVICE_URL` (local: `http://localhost:8081`).
Timeouts padrão: conexão 2000 ms e leitura 5000 ms. Na Etapa 3, essas propriedades foram centralizadas.

Falhas de conexão, timeout, 5xx ou resposta incompatível geram `ExternalServiceUnavailableException`.
O principal retorna **503 no formato ApiError**, com mensagem controlada, sem detalhes de Feign/rede.
Rejeições reconhecidas preservam 400/404/409. Não há retry, circuit breaker ou service discovery.

## Execução, testes e dados anteriores

Os comandos atuais para iniciar cada aplicação separadamente estão na Etapa 3.
Swagger: http://localhost:8080/swagger-ui.html e http://localhost:8081/swagger-ui.html.

| Cenário exigido | Como verificar |
| --- | --- |
| Serviço isolado | Iniciar parceiros e cadastrar/consultar diretamente em `:8081/business-partners`. |
| Integração | Com ambas as APIs ligadas, criar produto no principal usando um fornecedor existente; esperar 201. |
| Indisponibilidade | Desligar parceiros e repetir a operação no principal; esperar 503 controlado, sem gravação. |

A extração preservou testes anteriores e acrescentou integração HTTP em JVM separada e testes de falhas.
Foram criados Feign Client, DTOs do consumidor e exceções remotas; entidades, repository e regras de
parceiros saíram do principal. Spring Cloud OpenFeign e BOM Cloud 2025.1.3 foram adicionados.

Na Etapa 2, a migração de dados anteriores separava o agregado por schema, preservando IDs e removendo
FKs entre domínios. A transferência para os bancos Docker da Etapa 3 não é automática e exige backup e revisão.
Após a extração, excluir um parceiro referenciado pode deixar IDs sem cadastro; prefira inativá-lo.
Validação remota e gravação local não formam uma transação única.

## Reflexão arquitetural

### 1. Qual funcionalidade foi separada da aplicação principal?

Cadastro de parceiros, endereços, papéis e elegibilidade como cliente/fornecedor.
Produtos e pedidos permanecem no principal.

### 2. Por que ela foi escolhida?

Tem responsabilidade e ciclo de vida próprios, atende produtos/vendas e não depende deles para funcionar.

### 3. O que ficou mais complexo depois da separação?

Execução de dois processos, contratos DTO, configuração HTTP, latência, falhas remotas e consistência
entre validação e gravação. As FKs entre domínios e a transação única deixaram de existir.

### 4. O que aconteceria com a funcionalidade principal caso o novo serviço ficasse indisponível?

Operações dependentes de parceiros retornam 503 e não gravam alterações. Operações locais,
como total de vendas, continuam disponíveis; não existe fallback que duplique as regras remotas.

### 5. A funcionalidade realmente precisa permanecer como um serviço independente ou poderia continuar dentro da aplicação?

Poderia continuar no monólito, com menor custo operacional e integridade direta.
A separação oferece implantação independente e API reutilizável, mas só compensa se essas necessidades existirem.

</details>

<details>
<summary><strong>Etapa 3 — Cloud Native e Containerização</strong></summary>

## Arquitetura e persistência

```text
                   Config Server :8888
                     /           \
                    ↓             ↓
        Hulysses App :8080 ─HTTP→ Partner Service :8081
                ↓                       ↓
           hulysses-db           business-partner-db
           PostgreSQL 17        PostgreSQL 17
```

O Compose coordena os cinco serviços na rede `hulysses-network`.
O principal possui produtos/pedidos/itens; parceiros possui parceiros/endereços/papéis.
São instâncias, usuários, bancos e volumes independentes, sem acesso ao banco do outro domínio.
Os PostgreSQL não publicam portas no host; a comunicação entre aplicações usa DNS Docker e Feign.

Cada aplicação tem Dockerfile multi-stage com Maven/Java 21; a imagem final usa Java 21 JRE,
usuário sem privilégios e não inclui Maven. Healthchecks usam `/actuator/health` nas aplicações
e `pg_isready` nos bancos. As APIs aguardam seu banco e o Config Server saudáveis.

## Profiles e Config Server

| Profile | Uso |
| --- | --- |
| `dev` | Desenvolvimento local com PostgreSQL e URLs localhost; Config Server obrigatório. |
| `prod` | Compose/configuração externa; DNS Docker e senhas obrigatórias. |
| `test` | H2 e configuração local; não exige Docker nem Config Server externo. |
| `native` | Backend de arquivos do Config Server. |

Configurações centralizadas ficam em `config-server/src/main/resources/config-repo/`:
`application.yml`, arquivos de cada aplicação e suas variantes `-dev.yml`/`-prod.yml`.
Centralizam portas, datasource sem senha, schemas, JPA, URL/timeouts Feign, erros seguros e health.
No Compose, esse diretório é montado em `/config-repo`; alterações não exigem recompilar imagens.

As APIs guardam localmente nome, profile, descoberta/timeouts do servidor e senha do datasource.
Usam `spring.config.import` com o prefixo `configserver:` e a URL de `CONFIG_SERVER_URL`, sem `optional:`:
startup dev/prod falha se o Config Server estiver indisponível. Arquivos centrais são lidos no startup;
mudanças exigem reiniciar a API. Mudanças em variáveis do Compose exigem recriar o container.

## Variáveis de ambiente

| Variáveis | Finalidade / padrão no Compose |
| --- | --- |
| `DOCKER_DB_URL` (Compose atual), `DB_USERNAME`, `DB_PASSWORD` | Principal: `jdbc:postgresql://hulysses-db:5432/hulysses`; usuário hulysses; senha obrigatória. |
| `PARTNER_DB_URL`, `PARTNER_DB_USERNAME`, `PARTNER_DB_PASSWORD` | Parceiros: `jdbc:postgresql://business-partner-db:5432/business_partner`; usuário business_partner; senha obrigatória. |
| `DB_NAME`, `PARTNER_DB_NAME` | hulysses / business_partner. |
| `DB_SCHEMA`, `PARTNER_DB_SCHEMA` | public / business_partner_service. |
| `BUSINESS_PARTNER_SERVICE_URL`, `CONFIG_SERVER_URL` | URLs calculadas pelo Compose usando nomes de serviços e portas internas. |
| `SERVER_PORT`, `PARTNER_SERVER_PORT`, `CONFIG_SERVER_PORT` | Portas internas: 8080 / 8081 / 8888. |
| `APP_HOST_PORT`, `PARTNER_HOST_PORT`, `CONFIG_SERVER_HOST_PORT` | Portas publicadas no host: 8080 / 8081 / 8888; Config Server somente em loopback. |
| `FEIGN_CONNECT_TIMEOUT`, `FEIGN_READ_TIMEOUT` | 2000 / 5000 ms. |
| `CONFIG_CONNECT_TIMEOUT`, `CONFIG_READ_TIMEOUT` | 2000 / 5000 ms. |
| `JPA_DDL_AUTO`, `JPA_SHOW_SQL`, `JPA_CREATE_NAMESPACES` | update / false / true. |
| `SPRING_PROFILES_ACTIVE`, `CONFIG_SEARCH_LOCATIONS` | Compose seleciona prod nas APIs e file:/config-repo/ no servidor. |

Senhas ficam nas variáveis das APIs, não nos arquivos servidos pelo Config Server.
O `.env` é ignorado pelo Git e lido pelo Compose; Maven/IDE exigem variáveis do terminal/IDE.
Variáveis do terminal prevalecem sobre o `.env`. Dentro dos containers, não use localhost para outro serviço.
`ddl-auto=update` facilita bancos vazios nesta atividade; migrações de produção/dados antigos não são automáticas.

## Como executar com Docker

Requer Docker Desktop com containers Linux e Compose V2 ou superior.
Na raiz, crie o `.env` somente se ainda não existir e preencha **DB_PASSWORD** e **PARTNER_DB_PASSWORD**:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Execute em ordem, avançando somente se o comando anterior passar:

```powershell
docker compose config --quiet
docker compose build
docker compose up -d --wait --wait-timeout 180
docker compose ps -a
```

Espere cinco serviços saudáveis. Java/Maven/PostgreSQL no host não são necessários para o Compose.
No Compose atual, `DB_URL` fica reservado ao desenvolvimento local. Para personalizar a URL do
container, use `DOCKER_DB_URL=jdbc:postgresql://hulysses-db:5432/hulysses` no `.env`;
sem esse override, o Compose já usa `hulysses-db` e `DB_NAME`.
Se houver configurações antigas, remova-as com `Remove-Item Env:DOCKER_DB_URL, Env:DB_USERNAME -ErrorAction SilentlyContinue`
e execute `docker compose up -d --force-recreate --wait hulysses-app`.
`DB_USERNAME` deve corresponder ao usuário do volume existente (por exemplo, `postgres`);
alterar o `.env` não cria usuários nem modifica senhas no banco já inicializado.

```bash
docker compose logs -f            # acompanhar; Ctrl+C encerra a visualização
docker compose up -d --build      # reconstruir após mudanças no código
docker compose down              # parar preservando volumes
```

Os volumes `hulysses-db-data` e `business-partner-db-data` preservam os dados após down/up.
**`docker compose down -v` remove também os volumes e seus dados.** Alterar credenciais no `.env`
não reconfigura um banco já inicializado em volume existente.

## Execução separada fora do Docker

Requer JDK 21, Maven e PostgreSQL disponível. Configure `DB_*` e `PARTNER_DB_*` no terminal/IDE.
Em dev, as URLs padrão usam localhost:5432, bancos hulysses_one/business_partner e usuário/senha postgres.
Defina `CONFIG_SERVER_URL=http://localhost:8888` e `BUSINESS_PARTNER_SERVICE_URL=http://localhost:8081`.
Abra um terminal para cada comando, iniciando o Config Server primeiro:

```powershell
mvn -pl config-server spring-boot:run
mvn -pl business-partner-service spring-boot:run '-Dspring-boot.run.profiles=dev'
mvn -pl hulysses-app spring-boot:run '-Dspring-boot.run.profiles=dev'
```

## Como testar

Swagger: **http://localhost:8080/swagger-ui.html** e **http://localhost:8081/swagger-ui.html**.
OpenAPI: `/v3/api-docs` nas duas APIs. Health: `/actuator/health` nas três aplicações.

Confira os quatro conjuntos de configuração:

```powershell
@('hulysses-app/dev', 'hulysses-app/prod',
  'business-partner-service/dev', 'business-partner-service/prod') | ForEach-Object {
  Invoke-RestMethod "http://localhost:8888/$_"
}
```

Dev deve usar defaults locais; prod, DNS Docker. `propertySources` não deve conter senha.
Para testar cadastro e integração no mesmo PowerShell:

```powershell
$partnerBody = @{
  name = 'Fornecedor Etapa 3'; document = '12345678901234'
  email = 'teste@example.com'; phone = '11999999999'
  type = 'COMPANY'; roles = @('SUPPLIER')
} | ConvertTo-Json
$partner = Invoke-RestMethod -Method Post 'http://localhost:8081/business-partners' `
  -ContentType 'application/json' -Body $partnerBody
Invoke-RestMethod "http://localhost:8081/business-partners/$($partner.id)"

$productBody = @{
  name = 'Produto Etapa 3'; description = 'Teste HTTP'; price = 10.50; supplierId = $partner.id
} | ConvertTo-Json
$product = Invoke-RestMethod -Method Post 'http://localhost:8080/products' `
  -ContentType 'application/json' -Body $productBody
```

Use outro documento de 14 dígitos se já estiver cadastrado. Cadastros retornam 201; consulta retorna 200.

| Cenário | Verificação esperada |
| --- | --- |
| Principal | `GET :8080/sales-orders/total` retorna 200 usando seu PostgreSQL. |
| Parceiros | Cadastro/consulta em :8081 funciona e persiste somente no banco de parceiros. |
| Feign | Produto criado no principal usa fornecedor remoto e persiste no banco principal. |
| Indisponibilidade | `docker compose stop business-partner-service`; repetir POST /products retorna 503 seguro, sem gravar. O total de vendas continua disponível. |
| Persistência | Reiniciar parceiros; executar down/up sem -v; consultar os mesmos IDs de parceiro/produto. |
| Configuração externa | Alterar PARTNER_SERVER_PORT=8091 no .env e executar up -d --wait; manter PARTNER_HOST_PORT e conferir Feign sem build. Restaurar 8081 depois. |

Para restaurar o parceiro: `docker compose up -d --wait business-partner-service`.
Para confirmar persistência, mantenha o terminal aberto, execute down/up sem -v e consulte:

```powershell
Invoke-RestMethod "http://localhost:8081/business-partners/$($partner.id)"
Invoke-RestMethod "http://localhost:8080/products/$($product.id)"
```

Para conferir os dados em cada banco, usando usuários/bancos padrão:

```powershell
docker compose exec -T hulysses-db psql -U hulysses -d hulysses `
  -c 'SELECT id, name, supplier_id FROM public.product;'
docker compose exec -T business-partner-db psql -U business_partner -d business_partner `
  -c 'SELECT id, name FROM business_partner_service.business_partner;'
```

Testes automatizados: **`mvn clean verify`**, sem Docker ou Config Server externo.
São 38 testes anteriores e cinco testes do Config Server para as quatro combinações de aplicação/profile e exposição do health.
H2 permanece somente nos testes; execução real utiliza PostgreSQL.

## Reflexão arquitetural

### 1. Quais configurações da aplicação podem variar entre ambientes?

Portas, bancos, credenciais, schemas, URLs dos serviços/Config Server, profiles, timeouts e opções JPA.

### 2. Quais dessas configurações foram externalizadas?

As variáveis da tabela acima. Config Server centraliza propriedades operacionais; profiles selecionam
ambientes; variáveis fornecem overrides e senhas. Bootstrap e configuração test permanecem locais.

### 3. Por que um serviço não deve acessar diretamente o banco de outro serviço?

Cada domínio é proprietário de seus dados e regras. Acesso direto acoplaria o principal ao schema
de parceiros e dificultaria evolução independente; a API REST/DTOs define o contrato entre eles.

### 4. Qual problema o Docker resolve no projeto?

Padroniza Java 21, PostgreSQL 17 e empacotamento, isolando processos e reduzindo diferenças entre máquinas.

### 5. Qual é a função do Docker Compose?

Coordena Config Server, as duas APIs e os dois PostgreSQL: build, rede, portas, variáveis, volumes e saúde.

### 6. Qual problema uma configuração centralizada procura resolver?

Reduz divergências de propriedades espalhadas entre aplicações e ambientes, fornecendo configurações
por nome/profile. Config Server não é um armazenamento seguro de secrets por si só.

</details>

<details>
<summary><strong>Etapa 4 — Mensageria e processamento em lote</strong></summary>

## Arquitetura e responsabilidades

Esta etapa acrescenta registro assíncrono
de atividade por RabbitMQ e importação de parceiros por CSV com Spring Batch, reutilizando
DTOs, validações, service e repository existentes.

```text
POST BusinessPartner → commit PostgreSQL → Producer → RabbitMQ → Queue → Consumer → Atividade
CSV → ItemReader → ItemProcessor → ItemWriter → PostgreSQL
```

O `hulysses-app` mantém produtos, pedidos e integração REST/Feign.
O container `business-partner-consumer` usa a mesma imagem e o banco do domínio de parceiros;
apenas ele habilita o listener no Compose, permitindo pará-lo sem interromper o cadastro.

## Mensageria e processamento em lote

| Componente | Implementação |
| --- | --- |
| Broker | RabbitMQ com Management UI, healthcheck e volume próprio. |
| Exchange / fila / routing key | `business-partner.events` / `business-partner.activities` / `business-partner.created`. |
| Mensagem JSON | `eventId`, `eventType`, `businessPartnerId` e `occurredAt`. |
| Producer | Publica `BUSINESS_PARTNER_CREATED` após confirmar o cadastro no banco. |
| Consumer | Persiste a atividade; ACK após gravação e `eventId` para evitar duplicidade. |
| Job / Step | `businessPartnerImportJob` / `businessPartnerImportStep`. |
| Reader / Processor / Writer | FlatFileItemReader; normalização/validação; gravação pelo service existente. |
| Chunks | Blocos de 10 registros, cada um com sua transação. |

O cadastro não aguarda o processamento do histórico. Fila/exchange são duráveis e mensagens
são persistentes. Sem outbox/retry/DLQ: falha do broker fica no log e não desfaz o cadastro,
mas a mensagem não possui recuperação automática. A espera na fila vale para mensagens aceitas pelo RabbitMQ.

O [CSV de exemplo](business-partner-service/src/main/resources/batch/business-partners.csv)
usa `name,document,email,phone,type,roles`. `type` é INDIVIDUAL/COMPANY;
`roles` pode conter CUSTOMER/SUPPLIER separados por `;`. Inválidos e documentos repetidos são filtrados.
Cadastros via Batch também geram atividades após o commit dos chunks.

Spring Boot gerencia Spring AMQP 4.1.0 e Spring Batch 6.0.4. Metadados `BATCH_*` ficam no schema
`public` do banco de parceiros, usando o mesmo datasource. A API inicializa essas tabelas;
o consumer as reutiliza. Não há execução automática no startup ou scheduler; o endpoint aguarda o resultado do Job.

## Configuração e execução

Config Server mantém as variantes dev/prod; host, porta e credenciais RabbitMQ são externos.
No Docker, utiliza-se `rabbitmq:5672`. Senhas ficam nas variáveis locais, fora dos arquivos
servidos pelo Config Server. Testes usam H2, sem broker externo.

Crie `.env` apenas se ainda não existir e preencha **DB_PASSWORD, PARTNER_DB_PASSWORD e RABBITMQ_PASSWORD**.
Se já existir, acrescente as opções de `.env.example` preservando as credenciais atuais.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 240
docker compose ps -a
```

Espere sete serviços saudáveis: os cinco da Etapa 3, `rabbitmq` e `business-partner-consumer`.
O consumer não publica porta no host. `docker compose down` preserva volumes; não use `down -v` nos testes.

| Acesso padrão | Endereço |
| --- | --- |
| API principal / Swagger | http://localhost:8080/swagger-ui.html |
| Parceiros, histórico e Batch / Swagger | http://localhost:8081/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 — usuário/senha do `.env`. |
| Config Server | http://localhost:8888/business-partner-service/prod |

## Como testar

| Cenário | Verificação esperada |
| --- | --- |
| Mensageria normal | Cadastrar via POST `/business-partners`; conferir `Published`/`Processed` nos logs e atividade em `GET :8081/business-partners/{id}/activities`. |
| Consumer parado | Parar somente `business-partner-consumer`; cadastrar outro parceiro; POST retorna sucesso e fila mostra Consumers=0 / Ready≥1. |
| Retomada | Iniciar o consumer; aguardar consumo e consultar o histórico do parceiro. |
| Batch | Importar CSV; conferir status, contadores, chunks nos logs e parceiros persistidos. |
| Reimportação | Repetir CSV; documentos existentes são filtrados, sem duplicar cadastros. |

No cenário de indisponibilidade, mantenha API, banco e RabbitMQ ativos. Na Management UI,
inspecione `business-partner.activities` com **requeue habilitado**, para preservar a mensagem.
Use estes comandos antes e depois do novo cadastro:

```powershell
docker compose stop business-partner-consumer
# Cadastre outro parceiro e confira a mensagem aguardando na Management UI.
docker compose start business-partner-consumer
docker compose logs --tail 80 business-partner-service business-partner-consumer
```

Para executar o Batch com o exemplo incorporado ou enviar CSV por multipart:

```powershell
Invoke-RestMethod 'http://localhost:8081/batch/business-partners/import' -Method Post
curl.exe -X POST 'http://localhost:8081/batch/business-partners/import' `
  -F 'file=@business-partner-service/src/main/resources/batch/business-partners.csv'
docker compose logs --tail 100 business-partner-service
Invoke-RestMethod 'http://localhost:8081/business-partners'
```

Primeira importação sem os documentos do exemplo: **COMPLETED, readCount=13, writeCount=12,
filterCount=1 e commitCount=2**; logs mostram 10 e 2 gravações. Ao repetir: writeCount=0 e filterCount=13.
Verifique sempre `status`: estrutura/cabeçalho incorretos podem retornar HTTP 200 com `FAILED`.
O chunk com falha é revertido; chunks anteriores confirmados permanecem.

Testes automatizados: `mvn clean verify` e `scripts/test-etapa4.ps1`:

```powershell
mvn clean verify
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-etapa4.ps1
```

O script usa projeto/volumes separados: APIs 18080/18081, Config Server 18888 e Management 15674.
Cria `.env.etapa4-validation` ignorado pelo Git e preserva os dados. Instruções completas estão no guia.

## Reflexão arquitetural

### 1. Qual operação foi escolhida para comunicação assíncrona?

Registro de atividade relacionada ao cadastro de BusinessPartner através do RabbitMQ.

### 2. Por que essa operação não precisa ser concluída durante a requisição original?

O cadastro principal pode retornar sucesso sem aguardar o processamento secundário da atividade/histórico.

### 3. O que acontece se o consumidor estiver temporariamente indisponível?

Mensagens aceitas pelo RabbitMQ permanecem na fila e são processadas quando o consumer voltar.
A API continua disponível.

### 4. Qual funcionalidade foi escolhida para Batch?

Importação de Business Partners através de CSV.

### 5. Por que é adequada para Batch?

Vários registros passam pelo mesmo pipeline de leitura, validação/processamento e persistência em chunks.

### 6. Quando utilizar REST, mensageria e Batch no Hulysses One?

REST para consultas/cadastros com resposta imediata; mensageria para atividades secundárias posteriores;
Batch para conjuntos de dados, como importações. O endpoint REST inicia o Job, e Spring Batch processa o arquivo.
