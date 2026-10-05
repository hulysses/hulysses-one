# Hulysses One

Mini ERP desenvolvido com Java 21, Spring Boot 4.1.0, Spring MVC, Spring Data JPA e PostgreSQL para cadastro de
parceiros de negócio, catálogo de produtos e pedidos de venda. Este repositório registra a revisão arquitetural da

<details>
<summary><strong>Etapa 1 — Organização do monólito modular</strong></summary>

<br>

**Etapa 1 da disciplina de Microsserviços da POS Java do INFNET**.

## Arquitetura

O fluxo principal é:

**Cliente HTTP → Controller → Service → Repository → PostgreSQL**

O sistema continua sendo um **monólito modular**: uma aplicação Spring Boot, um artefato executável e um banco
PostgreSQL. A organização por domínio facilita futuras evoluções arquiteturais sem introduzir comunicação HTTP entre
serviços, descoberta de serviços, gateway ou mensageria.

- **Controller (`presentation`)**: recebe e valida requisições, chama o service e define a resposta HTTP. Nenhum
  controller acessa repositories.
- **Service (`application`)**: implementa casos de uso, verifica duplicidades e papéis, coordena referências entre
  módulos e controla transações. Escritas são atômicas; leituras são transações somente de leitura.
- **Repository (`persistence`)**: persiste entidades e executa consultas com Spring Data JPA.
- **Entidades (`domain`)**: representam relacionamentos e protegem invariantes locais, como campos obrigatórios, formato
  de documentos, quantidade e cálculo do total do pedido. Não executam consultas nem operações HTTP.
- **DTOs**: requests têm Bean Validation; responses mantêm os campos e estruturas JSON dos recursos existentes e são
  montados dentro do service, antes do encerramento da transação. Entidades e proxies JPA não são retornados pela API.
  `spring.jpa.open-in-view=false` impede consultas durante a serialização HTTP.

## Módulos da aplicação

| Módulo            | Responsabilidade existente                                                                                                                       |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `businesspartner` | Cadastro de pessoas e empresas, documento, contatos, situação, papéis e endereços. Os papéis existentes são CUSTOMER, SUPPLIER, EMPLOYEE e USER. |
| `product`         | Catálogo com nome, descrição, preço, situação e fornecedor; consultas de produtos ativos e ordenados por preço.                                  |
| `sales`           | Pedidos de venda, cliente, número único, data, status, itens, quantidades e total; consultas por cliente/status e total geral.                   |
| `shared`          | Exceções comuns, resposta de erro e configuração OpenAPI. É infraestrutura compartilhada, não um novo domínio de negócio.                        |

Endereços pertencem ao agregado de parceiros e não têm CRUD independente. EMPLOYEE e USER são papéis de parceiros, não
módulos separados de funcionários ou autenticação. Não foram criados módulos fictícios para cumprir a etapa.

### Estrutura final dos pacotes

```text
HulyssesOneApplication
businesspartner/
  application/                BusinessPartnerService
  domain/                     BusinessPartner, Address, BusinessPartnerType, BusinessPartnerRole
    exception/                AddressException, BusinessPartnerDocumentException
  persistence/                BusinessPartnerRepository
  presentation/               BusinessPartnerController
    dto/                      BusinessPartnerRequest/Response, AddressRequest/Response
    validation/               ValidPartnerDocument, PartnerDocumentValidator
product/
  application/                ProductService
  domain/                     Product
    exception/                ProductException
  persistence/                ProductRepository
  presentation/               ProductController
    dto/                      ProductRequest/Response
sales/
  application/                SalesOrderService
  domain/                     SalesOrder, SalesOrderProduct
  persistence/                SalesOrderRepository
  presentation/               SalesOrderController
    dto/                      SalesOrderRequest/Response, SalesOrderItemRequest,
                              SalesOrderProductResponse
shared/
  config/                     OpenApiConfig
  exception/                  DomainException, DuplicateEntityException,
                              EntityNotFoundException, ApiError, GlobalExceptionHandler
```

## Dependências entre módulos

**`product → businesspartner`**: todo produto referencia um fornecedor cadastrado. `ProductService` chama
`BusinessPartnerService.findSupplierById`, que verifica a existência e o papel SUPPLIER.

**`sales → businesspartner`**: todo pedido referencia um cliente. `SalesOrderService` chama
`BusinessPartnerService.findCustomerById`, que verifica a existência e o papel CUSTOMER.

**`sales → product`**: os itens referenciam produtos. O service de vendas consulta `ProductService.findById`, impede
produtos inativos e coordena a inclusão dos itens e o recálculo do total.

As chamadas são locais, dentro da mesma aplicação e transação. Cada service acessa apenas o repository de seu próprio
domínio. As entidades ainda compartilham relações JPA e os DTOs de resposta reutilizam estruturas dos módulos
dependentes; isso é uma dependência explícita do monólito atual, não isolamento de microsserviços.

### Relacionamentos persistidos

- `BusinessPartner → Address`: um-para-muitos bidirecional, com cascade ALL e orphan removal. Cada endereço pertence a
  um parceiro.
- `BusinessPartner → BusinessPartnerRole`: coleção de enums em `business_partner_role`.
- `Product → BusinessPartner`: muitos-para-um pelo `supplier_id` obrigatório.
- `SalesOrder → BusinessPartner`: muitos-para-um pelo `customer_id` obrigatório.
- `SalesOrder → SalesOrderProduct`: um-para-muitos com cascade ALL e orphan removal.
- `SalesOrderProduct → Product`: muitos-para-um obrigatório; o item também referencia seu pedido.

## API e consultas Spring Data JPA

| Recurso                                | Operações                                                      |
|----------------------------------------|----------------------------------------------------------------|
| `/business-partners`                   | POST criar; GET listar                                         |
| `/business-partners/{id}`              | GET consultar; PUT atualizar; DELETE excluir                   |
| `/business-partners/customers`         | GET parceiros com papel CUSTOMER                               |
| `/business-partners/suppliers`         | GET parceiros com papel SUPPLIER                               |
| `/business-partners/search?name=Maria` | GET por parte do nome, sem distinção de maiúsculas/minúsculas  |
| `/products`                            | POST criar; GET listar                                         |
| `/products/{id}`                       | GET consultar; PUT atualizar; DELETE excluir                   |
| `/products/active`                     | GET produtos ativos                                            |
| `/products/ordered-by-price`           | GET produtos por preço crescente                               |
| `/sales-orders`                        | POST criar; GET listar                                         |
| `/sales-orders/{id}`                   | GET consultar; PUT atualizar; DELETE excluir                   |
| `/sales-orders/customer/{customerId}`  | GET pedidos por cliente                                        |
| `/sales-orders/status/{status}`        | GET pedidos por status, sem distinção de maiúsculas/minúsculas |
| `/sales-orders/total`                  | GET soma dos totais de todos os pedidos                        |

POST retorna 201, GET/PUT retornam 200 e DELETE retorna 204 sem corpo. Consultar, atualizar ou excluir um recurso por ID
inexistente retorna 404. Consultas de listas sem correspondência continuam retornando 200 com lista vazia, inclusive
pedidos por ID de cliente sem pedidos.

| Consulta                                      | Uso na API                        | Revisão                                                                                  |
|-----------------------------------------------|-----------------------------------|------------------------------------------------------------------------------------------|
| `findByNameContainingIgnoreCase`              | Busca de parceiros por nome       | `@EntityGraph` carrega endereços na consulta; teste de busca parcial e case insensitive. |
| `findByRolesContaining`                       | Listas de clientes e fornecedores | `@EntityGraph` carrega endereços; teste verifica os papéis e resultados.                 |
| `findByIsActiveTrue`                          | Produtos ativos                   | `@EntityGraph` carrega fornecedor; teste diferencia ativos e inativos.                   |
| `findAllByOrderByPriceAsc`                    | Produtos por preço                | `@EntityGraph` carrega fornecedor; teste verifica ordem crescente.                       |
| `findByCustomerId` / `findByStatusIgnoreCase` | Consultas de pedidos              | Testes verificam cliente, status e listas vazias.                                        |
| `calculateTotalSales`                         | Total de vendas                   | JPQL com SUM/COALESCE preservado e testado com/sem pedidos.                              |

## Validação e erros

Todos os DTOs de entrada têm validações Jakarta, acionadas por `@Valid` nos controllers:

- Parceiros: nome, documento, e-mail e telefone obrigatórios; e-mail válido; tipo obrigatório; papéis não vazios e sem
  elementos nulos; documento compatível com o tipo por constraint própria, com erro no campo `document`.
- Endereços: campos obrigatórios, complemento opcional, CEP com oito dígitos; validação em cascata e endereços nulos
  rejeitados.
- Produtos: nome/descrição obrigatórios, preço obrigatório positivo com até 13 dígitos inteiros e duas casas decimais,
  ID de fornecedor obrigatório positivo.
- Pedidos: número/status obrigatórios, data e ID de cliente obrigatórios, ID positivo; itens não vazios, não nulos e
  validados em cascata; ID de produto e quantidade obrigatórios positivos.
- Strings persistidas respeitam o limite de 255 caracteres das colunas JPA existentes. O formato de telefone e de
  estado/país não recebeu restrições novas.

`GlobalExceptionHandler` usa `@RestControllerAdvice`, `@ExceptionHandler` e `ResponseEntityExceptionHandler`. Todos os
erros tratados seguem o contrato `ApiError`:

```json
{
  "timestamp": "2026-10-04T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Business partner not found with id: 10",
  "path": "/business-partners/10",
  "fields": {}
}
```

- 400: Bean Validation, documento/CEP inválidos, produto inativo, papel inadequado e duplicidades verificadas pelo
  service (status anterior preservado). Erros de validação incluem mensagens por campo em `fields`.
- 400: JSON malformado, enum inválido, parâmetro ausente ou tipo de parâmetro inválido, com mensagem segura.
- 404: recurso ou rota inexistente; também cobre referências inexistentes na criação/atualização.
- 409: violação de integridade, incluindo exclusão de recursos referenciados ou corrida de unicidade no banco, com
  mensagem genérica.
- 405/406/415: erros de método e negociação de conteúdo, no mesmo formato.
- 500: falhas inesperadas com mensagem genérica; detalhes ficam apenas no log do servidor.

Não são enviados SQL, nomes internos de classes, valores de exceções técnicas ou stack traces ao cliente.

## Swagger / OpenAPI

O springdoc 3.1.1 documenta recursos, parâmetros, requests, responses e constraints. `OpenApiConfig` define nome, versão
e descrição da API; `@Tag`, `@Operation`, `@Parameter`, `@Schema` e responses de criação/exclusão acrescentam
informações úteis ao contrato.

Com a aplicação na porta padrão:

- Swagger UI: **http://localhost:8080/swagger-ui.html** (redireciona para `/swagger-ui/index.html`).
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Candidato a serviço independente

O **cadastro de parceiros de negócio** é um candidato futuro. Sua responsabilidade seria manter pessoas/empresas,
documentos, contatos, endereços e papéis, expondo operações de cadastro, atualização, consulta e validação de
elegibilidade como cliente/fornecedor.

Essa responsabilidade tem ciclo de vida próprio e pode ser utilizada por outras funcionalidades além de vendas. Produtos
dependeriam dela para fornecedores; vendas dependeriam dela para clientes. Seria necessário compartilhar IDs estáveis,
dados de identificação/contato, endereços, papéis e resultados das consultas de elegibilidade. EMPLOYEE e USER
permaneceriam papéis até existirem casos de uso que justifiquem outra divisão.

</details>

<details>
<summary><strong>Etapa 1 — Organização do monólito modular</strong></summary>

<br>

# Etapa 2 — Primeiro serviço independente

## Serviço extraído

**Nome:** `business-partner-service`. Mantém pessoas/empresas, documentos, contatos, endereços,
papéis CUSTOMER/SUPPLIER/EMPLOYEE/USER e elegibilidade por papel. Todo o cadastro e suas regras
foram movidos para ele, incluindo persistência já existente e validações. A aplicação principal
mantém produtos, vendas e o ponto de entrada `/business-partners`, cujo service passa a consumir a API remota.

O cadastro possui ciclo de vida próprio, atende produtos e vendas e não precisa conhecer seus modelos.
Isso permite uma fronteira real. Endereços continuam no agregado de parceiros, sem CRUD separado.
Não foram introduzidos outros microsserviços ou persistência artificial.

## Arquitetura

```text
Cliente HTTP
     ↓
Hulysses One :8080
├── BusinessPartnerController → BusinessPartnerService (coordenação HTTP)
├── ProductController → ProductService → ProductRepository
├── SalesOrderController → SalesOrderService → SalesOrderRepository
│                            ↓ consulta de cliente/fornecedor
└── BusinessPartnerService → BusinessPartnerClient (@FeignClient)
                                  ↓ HTTP / JSON
                           Business Partner Service :8081
                           ├── BusinessPartnerController
                           ├── BusinessPartnerService (regras do cadastro/elegibilidade)
                           └── BusinessPartnerRepository → PostgreSQL
```

```text
hulysses-one/
├── pom.xml                         agregador Maven/parent, sem aplicação
├── mvnw / mvnw.cmd / .mvn/          wrapper compartilhado para build
├── hulysses-app/
│   ├── pom.xml                     Spring Boot + OpenFeign
│   └── src/
│       ├── main/                   HulyssesOneApplication, produtos, vendas e cliente HTTP
│       └── test/                   regressão e testes de integração/falhas
├── business-partner-service/
│   ├── pom.xml                     Spring Boot, sem dependência de hulysses-app
│   └── src/
│       ├── main/                   BusinessPartnerServiceApplication e domínio de parceiros
│       └── test/                   API isolada, validação, erros e fixture em JVM separada
├── docs/migrate-etapa1-partners.sql migração manual dos dados existentes
└── README.md
```

## Comunicação

O Hulysses One inicia chamadas síncronas através de `BusinessPartnerService` e `BusinessPartnerClient`.
O serviço de parceiros fornece a API. Controllers nunca acessam Feign diretamente.

| API fornecida em :8081                                                     | Consumo e resposta                                                                                                       |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `POST /business-partners`                                                  | Cadastro via aplicação principal ou diretamente. Envia `BusinessPartnerRequest`, retorna `BusinessPartnerResponse`, 201. |
| `PUT /business-partners/{id}`                                              | Atualização dos campos/coleções. Mesmo request/response, 200.                                                            |
| `GET /business-partners/{id}`                                              | Consulta para montar respostas com cliente/fornecedor, 200. Sem request body.                                            |
| `GET /business-partners/{id}/eligibility?role=SUPPLIER`                    | Consumido ao criar/atualizar produtos. ID e papel são os únicos parâmetros; retorna `BusinessPartnerResponse`, 200.      |
| `GET /business-partners/{id}/eligibility?role=CUSTOMER`                    | Consumido ao criar/atualizar pedidos. Mesmo contrato.                                                                    |
| `GET /business-partners`, `/customers`, `/suppliers`, `/search?name=Maria` | Consultas já existentes, consumidas pela fachada principal; listas de `BusinessPartnerResponse`, 200.                    |
| `DELETE /business-partners/{id}`                                           | Exclui cadastro e componentes do agregado, 204 sem corpo.                                                                |

O request usa `AddressRequest` para endereços; a resposta usa `AddressResponse`. No serviço,
tipo e papéis são enums locais; no consumidor são strings do contrato HTTP. Responses não são entidades.
O corpo inclui os dados necessários para preservar as respostas públicas da Etapa 1.

Bean Validation roda no novo serviço via `@Valid`: campos obrigatórios, limites de coluna,
e-mail, papéis/endereços não nulos, documento compatível com tipo e CEP de oito dígitos.
O consumidor não replica a regra de documento/elegibilidade. A regra anterior de elegibilidade
verifica papel, sem exigir parceiro ativo; preço, quantidades, produto ativo e cálculo do pedido
continuam na aplicação principal.

Erros de entrada/papel/documento/duplicidade: 400; ID inexistente: 404; corrida de unicidade/integridade
local: 409. Cada aplicação usa seu próprio advice e `ApiError` com `timestamp`, `status`, `error`,
`message`, `path` e `fields`. No consumidor, rejeições reconhecidas preservam 400/404/409 e campos de
validação, usando mensagens controladas e o caminho da requisição original. O corpo técnico remoto
não é repassado.

**Falhas de comunicação:** conexão recusada, timeout, erro 5xx ou resposta incompatível geram
`ExternalServiceUnavailableException`; o advice principal responde 503:

```json
{
  "timestamp": "2026-10-04T17:00:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Business partner service is currently unavailable",
  "path": "/products",
  "fields": {}
}
```

Feign usa timeout de conexão de 2 segundos e leitura de 5 segundos, configurados no YAML.
O comportamento padrão `Retryer.NEVER_RETRY` não repete chamadas. Não há circuit breaker,
discovery, gateway, mensageria ou transação distribuída. A compatibilidade Boot 4.1/Cloud 2025.1.3
foi conferida na [matriz oficial Spring Cloud](https://spring.io/projects/spring-cloud/), e os timeouts/retry
na [documentação do OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html).

### Dados existentes da Etapa 1

Para uma base nova basta iniciar as aplicações. Para uma base da Etapa 1, faça backup e, com ambas
paradas, execute manualmente `docs/migrate-etapa1-partners.sql` **antes de iniciar o serviço novo**:

```powershell
psql -h localhost -U postgres -d hulysses_one -v ON_ERROR_STOP=1 -f docs/migrate-etapa1-partners.sql
```

O script transacional confere origem/destino, remove somente as FKs de produtos/pedidos para parceiros
e move as três tabelas do agregado para `business_partner_service`, preservando dados, IDs, índices,
sequências e FKs internas. `supplier_id` e `customer_id` permanecem nas tabelas principais, agora como
valores escalares. Se usar schemas/bancos diferentes, revise a migração para esses destinos.
O script não foi executado sobre os dados reais e não é repetível após a migração; aborta se o destino existir.

**Integridade após a extração:** não há mais FK que impeça excluir um parceiro usado por produto/pedido.
As referências não são apagadas em cascata nem atualizadas pelo serviço remoto. Prefira inativar parceiros
em uso; uma exclusão direta pode deixar IDs sem cadastro e as consultas enriquecidas retornam 404.
Não há atomicidade entre validar papel por HTTP e gravar produto/pedido; uma alteração concorrente
do cadastro pode acontecer entre esses passos. Essa limitação distribuída é explícita e testada.

## Reflexão arquitetural

### 1. Qual funcionalidade foi separada da aplicação principal?

O cadastro de parceiros: pessoas/empresas, documentos, contatos, situação, papéis, endereços e
verificação de elegibilidade como fornecedor/cliente. Produtos e pedidos permanecem juntos no principal.

### 2. Por que ela foi escolhida?

Tem ciclo de vida próprio, concentra o agregado completo e suas regras, atende consumidores distintos e não depende
de produtos/vendas para executar seu cadastro.

### 3. O que ficou mais complexo depois da separação?

Passamos a executar/configurar dois processos, manter contratos DTO compatíveis em cada aplicação,
configurar URL/timeouts e tratar falhas HTTP. As respostas aninhadas requerem consultas remotas,
introduzindo latência e mais chamadas em listas. Deixaram de existir FKs e uma transação única
entre os domínios; a validação remota e a gravação local podem observar alterações concorrentes.
Migrar dados exige mudar sua propriedade e remover FKs antigas. Os testes precisam coordenar
um processo remoto real, e o cadastro tornou-se um novo ponto de falha para casos que o consultam.

### 4. O que aconteceria com a funcionalidade principal caso o novo serviço ficasse indisponível?

O principal inicia normalmente. Cadastro/atualização de produtos e pedidos que validam parceiro,
consultas com dados de cliente/fornecedor e a fachada de parceiros retornam 503 seguro.
As transações locais afetadas não gravam alterações. Total de vendas e exclusões locais não dependem
da API remota. Não há fallback que replique regras ou permita cadastrar com parceiro sem validação.

### 5. A funcionalidade realmente precisa permanecer como um serviço independente ou poderia continuar dentro da aplicação?

Poderia continuar como módulo interno, e isso provavelmente seria mais simples para este mini ERP:
menos operação, menor latência e integridade referencial/transacional direta. A extração cria autonomia
de implantação e uma API reutilizável, útil se houver outros consumidores, equipe ou escala própria.
Sem essas necessidades, os custos observados podem superar os benefícios. A Etapa 2 demonstra uma
fronteira distribuída real; não demonstra que microsserviços sejam automaticamente superiores.

## Registro das alterações da Etapa 2

| Tipo                            | Arquivos/componentes                                                                                                                                                                                                                                                             |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Estrutura                       | `src/` movido para `hulysses-app/src/`; POM original tornou-se `hulysses-app/pom.xml`; POM raiz convertido em agregador.                                                                                                                                                         |
| Movidos ao novo serviço         | Todo `businesspartner` de domínio/persistência/validação/DTOs, service de regras e controller. Pacote raiz passou a `br.com.hulysses.business_partner_service`.                                                                                                                  |
| Criados                         | POM/configuração/bootstrap próprios do serviço; `BusinessPartnerClient`; `ExternalServiceUnavailableException`; `RemotePartnerRequestException`; quatro DTOs HTTP locais do consumidor; `docs/migrate-etapa1-partners.sql`.                                                      |
| Modificados no principal        | Bootstrap com Feign, `BusinessPartnerService` agora remoto, controller de fachada, entidades Product/SalesOrder com IDs, services/responses de produtos/vendas, ProductRepository sem entity graph de fornecedor, YAML/configuração de exemplo, OpenAPI e advice.                |
| Infraestrutura local do serviço | `ApiError`, advice, exceções comuns e OpenAPI possuem implementação própria; não há dependência binária de infraestrutura do principal.                                                                                                                                          |
| Testes                          | Regressão API/arquitetura/advice/contexto movida/adaptada; criados `RemotePartnerProcess`, `UnavailableServiceTests`, `RemoteFailureTests`, `BusinessPartnerApiTests`, `BusinessPartnerServiceApplicationTests`, `StandaloneTestService` e initializer próprio de banco isolado. |
| Removidos do principal          | Entidades, repository, enums, regras e validação condicional de parceiros; vínculos JPA supplier/customer. Flyway e RestClient não utilizados foram removidos dos POMs; não foram adicionados ao novo serviço.                                                                   |
| Dependências novas              | BOM `spring-cloud-dependencies:2025.1.3` e `spring-cloud-starter-openfeign` no principal. Serviço usa Boot 4.1.0, JPA/PostgreSQL, MVC, Validation e springdoc 3.1.1 já utilizados. `maven-dependency-plugin` gera classpath apenas para fixture de testes.                       |

</details>
