# Hulysses One

Mini ERP desenvolvido com Java 21, Spring Boot 4.1.0, Spring MVC, Spring Data JPA e PostgreSQL para cadastro de 
parceiros de negócio, catálogo de produtos e pedidos de venda. Este repositório registra a revisão arquitetural da 
**Etapa 1 da disciplina de Microsserviços da POS Java do INFNET**.

## Arquitetura

O fluxo principal é:

**Cliente HTTP → Controller → Service → Repository → PostgreSQL**

O sistema continua sendo um **monólito modular**: uma aplicação Spring Boot, um artefato executável e um banco PostgreSQL. A organização por domínio facilita futuras evoluções arquiteturais sem introduzir comunicação HTTP entre serviços, descoberta de serviços, gateway ou mensageria.

- **Controller (`presentation`)**: recebe e valida requisições, chama o service e define a resposta HTTP. Nenhum controller acessa repositories.
- **Service (`application`)**: implementa casos de uso, verifica duplicidades e papéis, coordena referências entre módulos e controla transações. Escritas são atômicas; leituras são transações somente de leitura.
- **Repository (`persistence`)**: persiste entidades e executa consultas com Spring Data JPA.
- **Entidades (`domain`)**: representam relacionamentos e protegem invariantes locais, como campos obrigatórios, formato de documentos, quantidade e cálculo do total do pedido. Não executam consultas nem operações HTTP.
- **DTOs**: requests têm Bean Validation; responses mantêm os campos e estruturas JSON dos recursos existentes e são montados dentro do service, antes do encerramento da transação. Entidades e proxies JPA não são retornados pela API. `spring.jpa.open-in-view=false` impede consultas durante a serialização HTTP.

## Módulos da aplicação

| Módulo | Responsabilidade existente |
| --- | --- |
| `businesspartner` | Cadastro de pessoas e empresas, documento, contatos, situação, papéis e endereços. Os papéis existentes são CUSTOMER, SUPPLIER, EMPLOYEE e USER. |
| `product` | Catálogo com nome, descrição, preço, situação e fornecedor; consultas de produtos ativos e ordenados por preço. |
| `sales` | Pedidos de venda, cliente, número único, data, status, itens, quantidades e total; consultas por cliente/status e total geral. |
| `shared` | Exceções comuns, resposta de erro e configuração OpenAPI. É infraestrutura compartilhada, não um novo domínio de negócio. |

Endereços pertencem ao agregado de parceiros e não têm CRUD independente. EMPLOYEE e USER são papéis de parceiros, não módulos separados de funcionários ou autenticação. Não foram criados módulos fictícios para cumprir a etapa.

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

**`product → businesspartner`**: todo produto referencia um fornecedor cadastrado. `ProductService` chama `BusinessPartnerService.findSupplierById`, que verifica a existência e o papel SUPPLIER.

**`sales → businesspartner`**: todo pedido referencia um cliente. `SalesOrderService` chama `BusinessPartnerService.findCustomerById`, que verifica a existência e o papel CUSTOMER.

**`sales → product`**: os itens referenciam produtos. O service de vendas consulta `ProductService.findById`, impede produtos inativos e coordena a inclusão dos itens e o recálculo do total.

As chamadas são locais, dentro da mesma aplicação e transação. Cada service acessa apenas o repository de seu próprio domínio. As entidades ainda compartilham relações JPA e os DTOs de resposta reutilizam estruturas dos módulos dependentes; isso é uma dependência explícita do monólito atual, não isolamento de microsserviços.

### Relacionamentos persistidos

- `BusinessPartner → Address`: um-para-muitos bidirecional, com cascade ALL e orphan removal. Cada endereço pertence a um parceiro.
- `BusinessPartner → BusinessPartnerRole`: coleção de enums em `business_partner_role`.
- `Product → BusinessPartner`: muitos-para-um pelo `supplier_id` obrigatório.
- `SalesOrder → BusinessPartner`: muitos-para-um pelo `customer_id` obrigatório.
- `SalesOrder → SalesOrderProduct`: um-para-muitos com cascade ALL e orphan removal.
- `SalesOrderProduct → Product`: muitos-para-um obrigatório; o item também referencia seu pedido.

## API e consultas Spring Data JPA

| Recurso | Operações |
| --- | --- |
| `/business-partners` | POST criar; GET listar |
| `/business-partners/{id}` | GET consultar; PUT atualizar; DELETE excluir |
| `/business-partners/customers` | GET parceiros com papel CUSTOMER |
| `/business-partners/suppliers` | GET parceiros com papel SUPPLIER |
| `/business-partners/search?name=Maria` | GET por parte do nome, sem distinção de maiúsculas/minúsculas |
| `/products` | POST criar; GET listar |
| `/products/{id}` | GET consultar; PUT atualizar; DELETE excluir |
| `/products/active` | GET produtos ativos |
| `/products/ordered-by-price` | GET produtos por preço crescente |
| `/sales-orders` | POST criar; GET listar |
| `/sales-orders/{id}` | GET consultar; PUT atualizar; DELETE excluir |
| `/sales-orders/customer/{customerId}` | GET pedidos por cliente |
| `/sales-orders/status/{status}` | GET pedidos por status, sem distinção de maiúsculas/minúsculas |
| `/sales-orders/total` | GET soma dos totais de todos os pedidos |

POST retorna 201, GET/PUT retornam 200 e DELETE retorna 204 sem corpo. Consultar, atualizar ou excluir um recurso por ID inexistente retorna 404. Consultas de listas sem correspondência continuam retornando 200 com lista vazia, inclusive pedidos por ID de cliente sem pedidos.

| Consulta | Uso na API | Revisão |
| --- | --- | --- |
| `findByNameContainingIgnoreCase` | Busca de parceiros por nome | `@EntityGraph` carrega endereços na consulta; teste de busca parcial e case insensitive. |
| `findByRolesContaining` | Listas de clientes e fornecedores | `@EntityGraph` carrega endereços; teste verifica os papéis e resultados. |
| `findByIsActiveTrue` | Produtos ativos | `@EntityGraph` carrega fornecedor; teste diferencia ativos e inativos. |
| `findAllByOrderByPriceAsc` | Produtos por preço | `@EntityGraph` carrega fornecedor; teste verifica ordem crescente. |
| `findByCustomerId` / `findByStatusIgnoreCase` | Consultas de pedidos | Testes verificam cliente, status e listas vazias. |
| `calculateTotalSales` | Total de vendas | JPQL com SUM/COALESCE preservado e testado com/sem pedidos. |

## Validação e erros

Todos os DTOs de entrada têm validações Jakarta, acionadas por `@Valid` nos controllers:

- Parceiros: nome, documento, e-mail e telefone obrigatórios; e-mail válido; tipo obrigatório; papéis não vazios e sem elementos nulos; documento compatível com o tipo por constraint própria, com erro no campo `document`.
- Endereços: campos obrigatórios, complemento opcional, CEP com oito dígitos; validação em cascata e endereços nulos rejeitados.
- Produtos: nome/descrição obrigatórios, preço obrigatório positivo com até 13 dígitos inteiros e duas casas decimais, ID de fornecedor obrigatório positivo.
- Pedidos: número/status obrigatórios, data e ID de cliente obrigatórios, ID positivo; itens não vazios, não nulos e validados em cascata; ID de produto e quantidade obrigatórios positivos.
- Strings persistidas respeitam o limite de 255 caracteres das colunas JPA existentes. O formato de telefone e de estado/país não recebeu restrições novas.

`GlobalExceptionHandler` usa `@RestControllerAdvice`, `@ExceptionHandler` e `ResponseEntityExceptionHandler`. Todos os erros tratados seguem o contrato `ApiError`:

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

- 400: Bean Validation, documento/CEP inválidos, produto inativo, papel inadequado e duplicidades verificadas pelo service (status anterior preservado). Erros de validação incluem mensagens por campo em `fields`.
- 400: JSON malformado, enum inválido, parâmetro ausente ou tipo de parâmetro inválido, com mensagem segura.
- 404: recurso ou rota inexistente; também cobre referências inexistentes na criação/atualização.
- 409: violação de integridade, incluindo exclusão de recursos referenciados ou corrida de unicidade no banco, com mensagem genérica.
- 405/406/415: erros de método e negociação de conteúdo, no mesmo formato.
- 500: falhas inesperadas com mensagem genérica; detalhes ficam apenas no log do servidor.

Não são enviados SQL, nomes internos de classes, valores de exceções técnicas ou stack traces ao cliente.

## Swagger / OpenAPI

O springdoc 3.1.1 documenta recursos, parâmetros, requests, responses e constraints. `OpenApiConfig` define nome, versão e descrição da API; `@Tag`, `@Operation`, `@Parameter`, `@Schema` e responses de criação/exclusão acrescentam informações úteis ao contrato.

Com a aplicação na porta padrão:

- Swagger UI: **http://localhost:8080/swagger-ui.html** (redireciona para `/swagger-ui/index.html`).
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Candidato a serviço independente

O **cadastro de parceiros de negócio** é um candidato futuro. Sua responsabilidade seria manter pessoas/empresas, documentos, contatos, endereços e papéis, expondo operações de cadastro, atualização, consulta e validação de elegibilidade como cliente/fornecedor.

Essa responsabilidade tem ciclo de vida próprio e pode ser utilizada por outras funcionalidades além de vendas. Produtos dependeriam dela para fornecedores; vendas dependeriam dela para clientes. Seria necessário compartilhar IDs estáveis, dados de identificação/contato, endereços, papéis e resultados das consultas de elegibilidade. EMPLOYEE e USER permaneceriam papéis até existirem casos de uso que justifiquem outra divisão.

## Executar e testar

Pré-requisitos: JDK 21, PostgreSQL disponível e Maven 3.6.3+ ou Maven Wrapper.

Crie o banco `hulysses_one`. Configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, ou copie `example.application.properties` para `src/main/resources/application.properties` e ajuste os valores.

Exemplo PowerShell (substitua a senha):

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/hulysses_one'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'sua-senha'
./mvnw.cmd spring-boot:run
```

Com Maven instalado, use `mvn spring-boot:run`. Hibernate mantém o `ddl-auto=update` original; Flyway continua desabilitado porque não existem migrações versionadas no projeto.

```powershell
./mvnw.cmd clean verify
java -jar target/hulysses-one-0.0.1-SNAPSHOT.jar
```

Em Linux/macOS: `./mvnw clean verify` e `./mvnw spring-boot:run`.

O teste original `contextLoads` foi mantido e passou a usar o perfil `test`. A suíte padrão usa **H2 apenas em testes**, sem exigir o PostgreSQL nem alterar dados reais. Os testes de integração percorrem os controllers, services e repositories reais, sem mocks e sem transação externa que esconda falhas de commit/rollback.

Para repetir a mesma suíte no PostgreSQL, configure as variáveis `DB_*` acima e execute:

```powershell
./mvnw.cmd '-Dhulysses.test.postgresql=true' test
```

O usuário do banco precisa de permissão CREATE SCHEMA. `TestDatabaseInitializer` cria um schema temporário com nome aleatório `etapa1_test_<uuid>`, direciona todas as tabelas de teste para ele e remove esse schema no encerramento do contexto. As tabelas reais não são usadas. Uma interrupção abrupta do processo pode exigir limpeza posterior dos schemas temporários.

## Registro das alterações

- Criados: README, configuração versionada `application.yml`, `OpenApiConfig`, `ApiError`, cinco DTOs de resposta e duas classes de validação condicional de documento; testes `ApiIntegrationTests`, `ArchitectureTests`, `GlobalExceptionHandlerTests`, `TestDatabaseInitializer` e configuração do perfil de teste.
- Modificados: POM, três controllers, três services, cinco DTOs de entrada, cinco entidades, três exceções específicas, repositories de parceiros/produtos, configuração de exemplo e teste original.
- Movidos: DomainException, DuplicateEntityException, EntityNotFoundException e GlobalExceptionHandler de `shared.domain` para `shared.exception`; advice reescrito com contrato comum, e duplicidade agora faz parte da hierarquia de domínio.
- Preservados: aplicação principal, módulos existentes, todos os endpoints, tabelas/relacionamentos e regras de negócio descritas nesta revisão. Nenhuma tag foi criada automaticamente.

## Verificação da Etapa 1

Verificações realizadas em 04/10/2026 com Java 21 e PostgreSQL 17:

- Compilação e empacotamento do JAR executável concluídos.
- **27 testes passaram em H2 e os mesmos 27 passaram em PostgreSQL**, sem falhas ou testes ignorados. Incluem o teste original, 19 execuções de integração da API, dois testes de fronteiras arquiteturais e cinco de tratamento de exceções.
- CRUD dos três domínios, consultas derivadas/JPQL, validações de POST/PUT, inexistência em GET/PUT/DELETE, duplicidades, papéis, produto inativo, conflitos de integridade e rollback de atualização de pedido verificados.
- OpenAPI com os 14 caminhos existentes, requests/responses e schema de erro; HTML do Swagger, redirecionamento e configuração do Swagger verificados.
- JAR iniciado na porta 18081 com `ddl-auto=validate`, usando o schema PostgreSQL existente. Consultas principais e documentação responderam 200 por HTTP real; POST com dados inválidos respondeu 400 com campos; recurso inexistente respondeu 404 com contrato consistente. A instância de verificação foi encerrada.
- Schemas temporários do PostgreSQL removidos; consulta posterior confirmou zero schemas `etapa1_test_*` remanescentes.
- Nenhum controller acessa repository; cada service acessa somente o repository de seu domínio; apenas uma classe `@SpringBootApplication` permanece no projeto.

O projeto está pronto para receber a tag **`etapa-1`** após a revisão e o commit das alterações. A tag não foi criada.

## Pontos de atenção para evoluções futuras

- Os dados de fornecedor, cliente e produto permanecem relacionados por JPA. A etapa prepara a organização, mas a extração de um serviço ainda exigirá revisão desses contratos.
- Pedidos não guardam preço unitário histórico por item. A regra de cálculo existente foi preservada; mudar essa regra depende de uma decisão de negócio futura.
- Os endpoints de listas ainda não têm paginação. Entity graphs reduzem parte das consultas adicionais, mas listas grandes e relacionamentos aninhados devem ser avaliados antes de ampliar o volume.
- Nome de produto não é único. A consulta interna `findByNameIgnoreCase` retorna Optional e pode ser ambígua com homônimos; não é utilizada por nenhum endpoint existente.
- Hibernate `ddl-auto=update` foi preservado para desenvolvimento. Uma adoção futura de migrações deve partir do schema e dos dados existentes.
- Autenticação/autorização não existiam e não foram adicionadas como antecipação desta etapa.

Após confirmar compilação, testes e funcionamento local, a tag sugerida é `etapa-1`. Revise o diff, faça o commit das alterações e só então crie a tag; nada é executado automaticamente:

```powershell
git add README.md pom.xml src
git commit -m "Organiza monolito modular para a etapa 1"
git tag -a etapa-1 -m "Etapa 1: monolito modular revisado e verificado"
```
