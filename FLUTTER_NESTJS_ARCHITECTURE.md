# Arquitetura do Projeto: Flutter & NestJS

Este documento detalha a arquitetura do aplicativo móvel de segurança e seu respectivo backend/worker, seguindo rigorosamente os princípios de **Clean Architecture**, **SOLID**, **DRY**, **KISS** e **YAGNI**.

## Visão Geral

O sistema é composto por duas partes principais:
1.  **Mobile (Flutter):** Interface do usuário e detecção ativa quando em redes públicas (Modo Rua), além de visualização do dashboard.
2.  **Backend/Worker (NestJS):** Executado em um servidor local/Raspberry Pi (Modo Casa) para monitoramento contínuo da rede, com alta disponibilidade e confiabilidade.

## Arquitetura de Pastas - Mobile (Flutter)

A estrutura segue o padrão Clean Architecture, separando as camadas de apresentação, domínio e dados, o que facilita testes unitários rigorosos (Meta de 100% no core) e a manutenção a longo prazo.

```text
mobile/
├── lib/
│   ├── core/                   # Utilitários globais, temas, injeção de dependência e erros
│   │   ├── errors/             # Exceções personalizadas (NetworkException, AuthException)
│   │   ├── network/            # Cliente HTTP, verificador de conectividade
│   │   ├── theme/              # Design System (Cores noir/minimalista, Dark Mode)
│   │   └── utils/              # Funções puras (debounce, formatadores de data)
│   │
│   ├── domain/                 # Camada de Regras de Negócio (100% Pura, sem dependência externa)
│   │   ├── entities/           # Objetos de negócio centrais (Device, NetworkInfo, Alert)
│   │   ├── repositories/       # Interfaces dos repositórios (contratos de abstração)
│   │   └── usecases/           # Casos de Uso (ScanNetwork, ClassifyDevice, ToggleHomeMode)
│   │
│   ├── data/                   # Implementação técnica de busca e armazenamento
│   │   ├── models/             # Extensões das entities com métodos fromJson/toJson
│   │   ├── datasources/        # Acesso direto a APIs (NestJS local), ARP tables, Banco local (SQLite/Isar)
│   │   └── repositories/       # Implementação dos contratos definidos no Domain
│   │
│   └── presentation/           # Camada de UI (Bloc/Cubit ou Riverpod para gerência de estado)
│       ├── widgets/            # Componentes reutilizáveis do Design System (Botões, Cards)
│       └── features/           # Divisão por telas ou fluxos (Dashboard, Radar, Settings)
│           ├── dashboard/
│           │   ├── bloc/       # Gerenciamento de estado do Dashboard
│           │   └── pages/      # Telas principais do Dashboard
│           └── radar/
│
├── test/                       # Estrutura refletida de testes (100% para domain/ e data/)
│   ├── domain/
│   ├── data/
│   └── presentation/
└── pubspec.yaml
```

## Arquitetura de Pastas - Backend (NestJS)

O backend atua primariamente como um Worker (Modo Casa), focado no monitoramento contínuo da rede via ARP, mDNS e Ping Sweep, aplicando regras temporais antes de disparar alertas (via WebSocket/FCM) para o aplicativo.

```text
backend/
├── src/
│   ├── main.ts                   # Entry point da aplicação
│   ├── app.module.ts             # Módulo principal que engloba tudo
│   │
│   ├── core/                     # Constantes globais, configurações e middlewares
│   │   ├── config/               # Configuração do ambiente (Portas, chaves JWT/FCM)
│   │   ├── filters/              # Global Exception Filters (Tratamento centralizado de erros)
│   │   └── interceptors/         # Interceptadores (Log de requisições, transformadores)
│   │
│   ├── domain/                   # Camada de Domínio isolada (Entities e Casos de Uso agnósticos)
│   │   ├── entities/             # Representação rica de Dispositivos e Históricos
│   │   ├── repositories/         # Interfaces que a camada de dados deverá implementar
│   │   └── usecases/             # DetectIntruder, ApplyDebounceRules, GenerateTimeline
│   │
│   ├── infrastructure/           # Acesso direto ao S.O. e Bancos de Dados
│   │   ├── network/              # Parsers para ler tabela ARP nativa (Linux/Raspberry), mDNS, Nmap
│   │   ├── database/             # Conexão e repositórios Prisma/TypeORM (SQLite/PostgreSQL local)
│   │   └── messaging/            # Envio de notificações (Firebase/WebSocket, integração Telegram/App)
│   │
│   └── presentation/             # Controladores REST, Gateways (WebSocket) e Workers
│       ├── controllers/          # Endpoints para o Mobile consultar histórico ou ignorar dispositivos
│       ├── gateways/             # WebSocket para notificações em tempo real na LAN
│       └── workers/              # Schedulers (Cron) para Ping Sweep e leitura passiva da rede
│
├── test/                         # E2E e unit testes (Foco em Jest)
├── package.json
└── tsconfig.json
```

## Padrões Adotados (SOLID, DRY, KISS, YAGNI)

*   **S (Single Responsibility):** Cada Use Case (ex: `DetectIntruderUseCase`) tem apenas um motivo para mudar. Ele não formata strings de data nem chama requisições HTTP; apenas processa o fluxo lógico.
*   **O (Open/Closed):** A camada `infrastructure/network` no backend permite plugar facilmente um leitor ARP de Windows ou Linux sem alterar o domínio.
*   **L (Liskov Substitution):** Múltiplos métodos de notificação (Firebase, WebSocket, Telegram) implementam a mesma interface genérica `INotificationService`.
*   **I (Interface Segregation):** Em vez de um `IDeviceService` massivo, dividimos em `IDeviceScanner`, `IDeviceClassifier` e `IDeviceStorage`.
*   **D (Dependency Inversion):** O domínio não conhece o Firebase nem o SQLite; ele depende exclusivamente das interfaces (contratos).
*   **DRY (Don't Repeat Yourself):** Toda a lógica de formatação de endereços MAC e IP é centralizada em utilitários únicos.
*   **KISS (Keep It Simple, Stupid):** Inicialmente, sem uso de filas Kafka ou RabbitMQ para um escopo de rede doméstica; chamadas diretas ou RxJS resolvem a reatividade no worker.
*   **YAGNI (You Aren't Gonna Need It):** Evitamos construir agora suporte a múltiplos bancos de dados dinâmicos ou dashboards Web complexos. Foco estrito nas funcionalidades Core definidas (Worker + App Flutter).
