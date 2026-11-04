# Prompt de validação — cole isto no OpenCode recém-instalado

```
Acabei de instalar o OpenCode com um kit de ferramentas. Faça um diagnóstico completo e me diga o que falta, em ordem de prioridade.

Verifique e reporte:

1. Versão: rode `opencode --version`.
2. Config: leia `~/.config/opencode/opencode.jsonc` e confirme que existem os campos `plugins`, `skills`, `mcp.servers` e `provider`.
3. MCPs: rode `opencode mcp list` e classifique cada servidor como conectado, precisa de login ou falhou.
4. Skills: conte as pastas em `~/.config/opencode/skills` e em `~/.config/opencode/vendor/*/skills`.
5. Agentes: liste os arquivos em `~/.config/opencode/agents` e diga qual modelo cada um usa.
6. Modelo: diga qual provedor está autenticado (se nenhum, me mande rodar `opencode auth login`).

Depois disso, faça um teste real:

7. Use o subagente `revisor` para revisar um arquivo pequeno do projeto atual.
8. Chame o MCP `context7` para buscar a documentação de uma lib que o projeto usa.

No fim, me dê:
- uma lista do que está funcionando,
- uma lista do que falta (com o comando exato para resolver),
- e uma sugestão de modelo principal (free ou pago) para este projeto.

Não instale nada sem me perguntar antes.
```
