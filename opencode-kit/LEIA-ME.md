# Kit OpenCode — instalação em 3 passos

Tempo: ~10 min se a máquina já tem Node e Git. ~25 min do zero.

## Passo 1 — Instalar (no PC do seu amigo)

Abra o PowerShell na pasta do kit e rode:

```powershell
powershell -ExecutionPolicy Bypass -File .\instalar.ps1
```

O script faz sozinho:

1. Instala Node.js LTS e Git via winget (se faltarem).
2. Instala o OpenCode CLI (`@opencode/cli`).
3. Faz backup da config antiga (se existir).
4. Copia skills, agentes, comandos, plugins e ferramentas.
5. Clona os repositórios de skills (`vendor/`).
6. Ajusta os caminhos e os MCPs no `opencode.jsonc`.
7. Verifica a instalação e lista os MCPs.

Se ele tiver token do PixelLab:

```powershell
powershell -ExecutionPolicy Bypass -File .\instalar.ps1 -PixelLabToken "SEU-TOKEN"
```

Se ele usa o proxy "Free Claude Code" local na porta 8082:

```powershell
powershell -ExecutionPolicy Bypass -File .\instalar.ps1 -ComFCC
```

## Passo 2 — Login no provedor de modelos

```powershell
opencode auth login
```

Escolha **OpenRouter**. Sem isso o agente não responde. (Anthropic, OpenAI e outros também funcionam.)

## Passo 3 — Validar

Abra uma pasta de projeto qualquer, rode `opencode` e cole o conteúdo de `PROMPT.md`.
O próprio agente confere o que está instalado e aponta o que falta.

---

## O que vem no kit

| Item | O que é |
|---|---|
| **74 skills** | Especialistas por stack (Python, React, Postgres, K8s, segurança, testes, arquitetura...). As irrelevantes já vêm com autoinvoke desligado. |
| **+60 skills vendor** | `claude-octopus` (fluxos de trabalho) e `andrej-karpathy-skills` (diretrizes de código). |
| **3 subagentes** | `olheiro` (lê imagem/PDF), `pesquisador` (web + repos), `revisor` (revisão só-leitura). |
| **4 MCPs** | `context7` (docs de libs), `memoria` (grafo persistente), `pensamento` (raciocínio passo a passo), `pixellab` (pixel art, opcional). |
| **Plugin superpowers** | Fluxos de brainstorming, TDD, debug sistemático, planos e revisão. |
| **Plugin i-have-adhd** | Respostas curtas e acionáveis (desligável). |
| **Comandos** | `/common-ground`, discovery, planning, execution, retrospectives. |
| **Ponte ChatGPT** | `tools/consultar-codex.ps1` + skill `consultar-gpt` (opcional, exige Codex CLI). |

## O que precisa de conta/chave

| Recurso | Precisa de |
|---|---|
| Modelos (principal) | Conta OpenRouter (tem modelos `:free`) |
| `pixellab` | Token PixelLab (assinatura) |
| `consultar-gpt` | Codex CLI + assinatura ChatGPT |
| `free-claude-code` | Proxy local na porta 8082 |
| `context7`, `memoria`, `pensamento` | Nada — já funcionam |

## Problemas comuns

**`opencode` não é reconhecido depois de instalar**
Feche e abra o terminal. O PATH só atualiza em sessão nova.

**MCP não conecta**
Rode `opencode mcp list`. Se aparecer `needs authentication`, rode `/mcps` dentro do OpenCode e faça login.
Os MCPs locais (`memoria`, `pensamento`) baixam via `npx` na primeira execução — a primeira chamada demora.

**Skills demais / agente lento**
Edite o `SKILL.md` da skill e adicione:

```yaml
metadata:
  opencode/autoinvoke: false
```

**Quero desligar o modo ADHD**
Apague `%USERPROFILE%\.config\opencode\.i-have-adhd-always` (se existir) ou diga "stop adhd mode" na sessão.

**Deu ruim, quero voltar**
A config antiga ficou em `%USERPROFILE%\.config\opencode.bak-<data>`.
Copie de volta por cima de `%USERPROFILE%\.config\opencode`.

## Atualizar o kit depois

Na sua máquina, dentro desta pasta:

```powershell
powershell -ExecutionPolicy Bypass -File .\exportar.ps1
```

Isso recopia suas skills/agentes/comandos e regenera o `opencode.jsonc` **sem** token e **sem** caminho absoluto.
Depois compacte a pasta e envie de novo.

## Segurança

O kit **não** inclui `service.json` (senha do serviço local), backups nem tokens.
O `opencode.jsonc` vai com o placeholder `COLE_SEU_TOKEN_PIXELLAB_AQUI`.
Nunca commite `service.json` nem tokens num repositório público.
