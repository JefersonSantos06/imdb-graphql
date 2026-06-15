# imdb-graphql

API REST que funciona como **wrapper do IMDB**: dado um código, devolve os dados de
um filme, de uma pessoa ou o calendário de estreias, já normalizados em JSON e em
português. Por baixo dos panos consome a **API GraphQL pública do IMDB**
(`https://api.graphql.imdb.com/`) e mantém um cache local com TTL configurável.

> Por que GraphQL e não scraping de HTML? As páginas de `www.imdb.com` ficam atrás
> de um desafio JavaScript do AWS WAF e exigiriam um navegador real (Playwright/
> Chromium). O endpoint GraphQL aceita queries via POST, devolve JSON puro e não
> passa pelo WAF — mais simples, mais rápido e sem dependência de navegador.

## Stack

| Camada        | Tecnologia                                   |
|---------------|----------------------------------------------|
| Linguagem     | Java 25                                       |
| Framework     | Spring Boot 4.1.0 (Web MVC, Security, Validation, HATEOAS, Actuator) |
| Persistência  | Spring Data JPA + H2 (em memória) + Flyway   |
| JSON          | Jackson 3 (`tools.jackson`)                   |
| Cliente HTTP  | `java.net.http.HttpClient` (JDK)              |
| Documentação  | Spring REST Docs + AsciiDoctor                |
| Build         | Maven (wrapper `./mvnw` incluído)             |

## Requisitos

- **JDK 25**
- Não é necessário instalar o Maven — use o wrapper `./mvnw` (ou `mvnw.cmd` no Windows).

## Como executar

```bash
# subir a aplicação (porta 8080)
./mvnw spring-boot:run

# ou empacotar e rodar o jar
./mvnw clean package
java -jar target/imdb-graphql-0.0.1-SNAPSHOT.jar
```

A aplicação sobe em `http://localhost:8080`.

## Autenticação

Todos os endpoints exigem **HTTP Basic**, exceto:

- `GET /actuator/health` — health check
- `/docs/**` — documentação gerada
- `/h2-console/**` — console do H2 (inspeção do cache em dev)

Credenciais padrão (sobrescreva via variáveis de ambiente em produção):

| Variável       | Default     |
|----------------|-------------|
| `APP_USER`     | `admin`     |
| `APP_PASSWORD` | `admin123`  |

Sem credenciais válidas a API responde `401 Unauthorized`.

## Endpoints

Base: `/api/imdb`

| Método | Caminho                    | Descrição                                   |
|--------|----------------------------|---------------------------------------------|
| `GET`  | `/api/imdb/{codigo}`       | Dados completos de um filme (`ttNNNNNNN`)   |
| `GET`  | `/api/imdb/pessoa/{nm}`    | Dados de uma pessoa (`nmNNNNNNN`)           |
| `GET`  | `/api/imdb/estreias`       | Calendário de estreias (sem cache)          |

### Parâmetros de query (opcionais)

| Parâmetro | Aplica-se a       | Descrição                                                        |
|-----------|-------------------|------------------------------------------------------------------|
| `locale`  | todos             | Idioma/região dos campos localizados. Default: `pt-BR`.          |
| `ttl`     | filme e pessoa    | Validade do cache em **segundos**; `0` força um novo scrape.     |

### Exemplos

```bash
# Filme — O Poeta do Crime / The Shawshank Redemption
curl -u admin:admin123 http://localhost:8080/api/imdb/tt0111161

# Filme em inglês, ignorando o cache
curl -u admin:admin123 "http://localhost:8080/api/imdb/tt0111161?locale=en-US&ttl=0"

# Pessoa — Morgan Freeman
curl -u admin:admin123 http://localhost:8080/api/imdb/pessoa/nm0000151

# Calendário de estreias
curl -u admin:admin123 http://localhost:8080/api/imdb/estreias
```

### Erros

As respostas de erro usam o formato [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc7807) (RFC 7807):

| Situação                                   | Status              |
|--------------------------------------------|---------------------|
| Código com formato inválido                | `400 Bad Request`   |
| Filme/pessoa inexistente                   | `404 Not Found`     |
| Falha ao consultar a API GraphQL do IMDB   | `502 Bad Gateway`   |
| Sem credenciais / inválidas                | `401 Unauthorized`  |

## Cache

O cache é genérico e guarda **um documento JSON por `(tipo, chave, locale)`** na
tabela `imdb_cache` (H2). Cada linha tem um `fetched_at` que controla o TTL.

- TTL padrão: **7 dias** (configurável; veja abaixo).
- `ttl=0` na requisição força um novo scrape e atualiza a linha.
- O **calendário de estreias não é cacheado**, por ser sensível ao tempo.

> ⚠️ O H2 está configurado **em memória** (default do Spring Boot), então o cache é
> perdido a cada reinício. O schema é criado pelo Flyway
> ([`V1__create_imdb_cache.sql`](src/main/resources/db/migration/V1__create_imdb_cache.sql)).
> Para persistir entre reinícios, configure um `datasource` adequado
> (ex.: `jdbc:h2:file:./data/imdb`) ou outro banco.

## Configuração

Propriedades em [`application.properties`](src/main/resources/application.properties)
(prefixo `app.imdb`):

| Propriedade              | Default  | Descrição                                                  |
|--------------------------|----------|------------------------------------------------------------|
| `app.imdb.cache-ttl`     | `7d`     | TTL padrão do cache (sobrescrito por `?ttl=` na requisição).|
| `app.imdb.default-locale`| `pt-BR`  | Locale usado quando `locale` não é informado.              |
| `app.imdb.timeout`       | `30s`    | Timeout de conexão/requisição da chamada GraphQL.          |

## Documentação da API

A documentação dos endpoints é gerada a partir dos testes (Spring REST Docs) e
montada com AsciiDoctor no momento do `package`. O HTML resultante é servido
estaticamente:

```bash
./mvnw clean package
# após subir a aplicação:
#   http://localhost:8080/docs/index.html
```

Fonte: [`src/docs/asciidoc/index.adoc`](src/docs/asciidoc/index.adoc).

## Console H2

Disponível em `http://localhost:8080/h2-console` (em dev) para inspecionar a tabela
`imdb_cache`.

## Testes

```bash
./mvnw test
```

Os testes cobrem o mapeamento do JSON do GraphQL para os DTOs
(`ImdbGraphqlMapperTest`), a segurança (`ImdbSecurityTest`) e a geração da
documentação (`ImdbDocsTest`), usando os fixtures em
[`src/test/resources/imdb/`](src/test/resources/imdb).

## Estrutura do projeto

```
src/main/java/com/jsantos/imdbgraphql/
├── SimpleWraperApplication.java     # entrypoint Spring Boot
├── config/
│   ├── ImdbProperties.java          # propriedades app.imdb.*
│   └── SecurityConfig.java          # HTTP Basic + regras de acesso
└── imdb/
    ├── dto/                         # records de resposta (filme, pessoa, estreia, ...)
    ├── model/ImdbCacheEntry.java    # entidade JPA do cache
    ├── repository/                  # Spring Data JPA
    ├── scraper/
    │   ├── ImdbScraper.java         # chamadas GraphQL ao IMDB
    │   └── ImdbGraphqlMapper.java   # JSON do IMDB -> DTOs
    ├── service/ImdbService.java     # orquestra cache + TTL + scraping
    └── web/                         # controller + tratamento de exceções
```

## Fluxo de uma requisição

```
GET /api/imdb/{codigo}
  → ImdbController        (valida o código, lê locale/ttl)
  → ImdbService           (consulta o cache; se válido, devolve)
       └─ cache expirado / ttl=0
          → ImdbScraper   (POST GraphQL em api.graphql.imdb.com)
          → ImdbGraphqlMapper (monta o DTO)
          → grava/atualiza imdb_cache
  → JSON da resposta
```
