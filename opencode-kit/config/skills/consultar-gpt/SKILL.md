---
name: Consultar o GPT (Codex)
description: Consulta um modelo GPT (gpt-5.6-terra / gpt-5.6-luna) usando a assinatura ChatGPT do usuário pelo Codex CLI — sem chave de API e sem custo extra. Use quando precisar de segunda opinião de outro modelo sobre um plano, decisão ou diagnóstico, ou quando o usuário pedir para "perguntar ao ChatGPT".
---

# Consultar o GPT pelo Codex CLI

Ponte 0800 para conversar com os modelos GPT: usa a **assinatura ChatGPT** do usuário
(`auth_mode = chatgpt`), **sem chave de API e sem cobrança por token**. Consome a cota do
plano ChatGPT (Codex), então use com parcimônia.

## Comando

```powershell
& "$env:USERPROFILE\.config\opencode\tools\consultar-codex.ps1" "SEU BRIEFING AQUI"
```

Opções úteis:
- `-Model gpt-5.6-luna` — variante mais leve; o padrão é `gpt-5.6-terra`.
- `-WorkDir C:\caminho` — diretório que o Codex pode **ler** como contexto do projeto.
- `-ShowLog` — mostra o log bruto (útil quando não vem resposta).

O script já força `--sandbox read-only`, `--ignore-user-config` e um diretório de rascunho,
então **o Codex não escreve em nada** do projeto.

## Modelos disponíveis nesta conta

Saem de `codex debug models`: `gpt-5.6-terra`, `gpt-5.6-luna`, `gpt-reserve`,
`codex-auto-review`. Modelos antigos como `gpt-5.5` foram aposentados para contas ChatGPT.

A ponte **cai sozinha para o próximo** se um modelo for aposentado (testado com o `gpt-5.5`).
Para trocar a ordem, use a lista: `-Model gpt-5.6-luna,gpt-5.6-terra`.
Se todos falharem, rode `codex debug models` e ajuste.

## Como usar bem

- **Briefing autocontido.** O Codex não vê a nossa conversa nem o nosso histórico. Escreva o
  contexto, o que já foi tentado e a pergunta exata.
- **Peça uma resposta acionável**: "aponte o furo nesse plano", "o que eu não considerei",
  "isso quebra em qual caso".
- **Trate como opinião externa.** Confira contra o código/fatos antes de aplicar.
- **Não delegue o trabalho inteiro** — o Codex roda isolado e não tem as nossas ferramentas.
  Sirva-se dele para segunda opinião, não para execução.
