# Conversão de Schematics modernos -> MCEdit/Alpha .schematic (1.7.10)

> NOTA DE TRABALHO — usado para retomar o projeto depois. Atualize quando algo mudar.

## Objetivo
Converter schematics modernos (`fortress.litematic`, `knight-statue-e2080.schem`) para o formato
legado **MCEdit/Alpha .schematic** compatível com o servidor **1.7.10 CustomNPC/nntdark**,
**substituindo blocos modernos por equivalentes visuais da 1.7.10**.

## Resultado (concluído e validado)
Arquivos gerados em `C:\Users\Fernando\nntdarkmp\schematics\`:

| Arquivo | Dim | Notas |
|---|---|---|
| `knight-statue-e2080.schematic` | 27×50×23 | estátua de cavaleiro |
| `fortress.schematic` | 30×14×31 | fortaleza medieval ORIGINAL (sem escala) |
| `fortress-stretch.schematic` | 60×14×61 | fortaleza com **muralhas triplicadas (3x)** — **portões E/O fechados/maciços** |
| `castle.schematic` | 47×42×34 | castelo clássico medieval (BigAriYT "Medieval Castle") convertido p/ 1.7.10 |
| `fortress-stretch-with-castle.schematic` | 60×42×61 | **entregável final**: castelo 47×34 já posicionado no pátio interno (48×50) da fortaleza |

> O `fortress.schematic` é a versão original (30×14×31). A escala 2x uniforme foi **revertida**.
> `fortress-stretch.schematic` gerado via `stretch.py`. As muralhas N/S (sem portão) são expandidas por
> **translação pura** da unidade [7..22) (ameias `@WW` ficam contínuas e simétricas). As muralhas E/O
> (com portão) são expandidas por translação pura do corpo [7..22) 3x.
> **ÚLTIMA MUDANÇA:** os portões das muralhas E/O foram **removidos** (decisão do usuário) — em vez de
> manter 1 portão central, o vão Y2-5 do portão é fechado com parede maciça em **todas** as cópias
> (removido o `k != keep` no `stretch.py`). Muralhas E/O ficaram totalmente fechadas; só os cantos das
> esquinas (z=0, z=60) mantêm o desenho original de ameias. Dims: 30x14x31 -> 60x14x61.
> **Atenção a bug anterior:** usar `rel % seg` com sub-segmentos quebrava as ameias (`@WW@W@`, `WWW@`).
> Correto: **translação pura do corpo inteiro** como unidade de repetição.

### Castelo no pátio (addon)
- Baixado: **BigAriYT "Medieval Castle"** (escolhido pelo usuário entre opções pesquisadas).
- Host com bloqueio Cloudflare/login → baixado em formato proprietário **BawkBlock BBK** (`*.bbk`)
  e **decodificado por script** (`castle.py`): `BBK[+ver][int][DIMS][w,h,l=47,42,34][PALT][n=168 entradas
  (nome + props)][BLKS][5-byte hdr][67116 bytes = palette index por bloco]`.
- Paleta: 168 estados (26 stone_brick_wall, 21 stone_brick_stairs, 16 spruce_stairs, etc.).
- `castle.schematic` (47×42×34) convertido; **nenhum bloco sem mapeamento** (ver tabela abaixo).
- `integrate.py` cola o castelo no pátio da fortaleza (overlay de blocos não-ar):
  - **Castelo ROTACIONADO 90°** para que a entrada (portão principal do castelo) aponte para o **NORTE**.
  - Rotação `integrate.py::rot_castle`: `nx=(L-1)-z; nz=x` (face OESTE do castelo -> NORTE),
    com rotação correspondente dos data de escadas/portas/trapdoors/tochas/cercas-portão.
  - **Posição:** rotacionado (0,0,0) → combinado (13, 0, 6). Castelo rot 34×42×47: X=13..46, Z=6..52.
  - Pátio interno = **48(X: 6..53) × 50(Z: 5..54)**, chão de grama em Y=0; muralhas de 14 de altura.
  - **Portões da fortaleza estão na muralha NORTE (z=0-4), em x=13-16, 28-31, 43-46** (triplicados).
  - Portão principal do castelo (porta 197) fica em combinado (~30, z~22) virado para o **NORTE**,
    de frente para a muralha NORTE (portão central do castelo ~x=30 alinha com o portão central 28-31).
  - Altura cheia mantida (torres do castelo até Y=41 ultrapassam as muralhas de 14) — decisão do usuário.
- Contagem (com portões fechados): fortress-stretch 9025 + castle 6087 = **13550 não-ar** no combinado
  (base do castelo=1562 substitui a grama do pátio no footprint). Antes (portão aberto): 9011/13536.

- Formato byte a byte idêntico aos de referência (mesmas chaves/tipos).
- **Nenhum bloco ficou sem mapeamento** (167+147+168 da paleta convertidos).
- Pendente: confirmar visual no WorldEdit do nntdark.

## Arquivos envolvidos
- Scripts: `C:\Users\Fernando\AppData\Local\Temp\opencode\schem\convert.py` (knight/fortress),
  `stretch.py` (muralha triplicada), `castle.py` (decoder BBK + conversor do castelo),
  `integrate.py` (cola o castelo no pátio), `render.py`, `final_check.py`, `validate_*.py` etc.
- Entradas: `C:\Users\Fernando\Downloads\fortress.litematic`, `C:\Users\Fernando\Downloads\knight-statue-e2080.schem`,
  `C:\Users\Fernando\AppData\Local\Temp\opencode\schem\bigariyt_castle.bbk` (formato BawkBlock BBK)
- Referência de formato/IDs: `C:\Users\Fernando\nntdarkmp\schematics\Dragon-Nether-Portal-Statue.schematic`
  e `Autumn-Hand-Nether-Portal-Statue.schematic`
- Destino: `C:\Users\Fernando\nntdarkmp\schematics\`

## Como rodar (castelo)
```
python "C:\Users\Fernando\AppData\Local\Temp\opencode\schem\castle.py"      # BBK -> castle.schematic
python "C:\Users\Fernando\AppData\Local\Temp\opencode\schem\integrate.py"    # -> fortress-stretch-with-castle.schematic
```

## Convenções idênticas às de referência
- `Materials:"Alpha"`
- Ordem de indexação: `index = x + z*Width + y*Width*Length` (= `(y*Length+z)*Width + x`)
- IDs >127 gravados como **byte assinado negativo** (ex.: 139→-117, 159→-97, 160→-95, 161→-95, 171→-85)
- Campos: `Width, Height, Length (Short)`, `Materials (String)`, `Entities, TileEntities (List[Compound])`,
  `Blocks, Data (ByteArray)`, `WEOffsetX/Y/Z (Int)`
- Formato NBT binário gzip válido (escrito à mão via `BinWriter`, não usa serialização SNBT do nbtlib).

## Formatos de origem lidos
- **knight** (Sponge v2/v3 "compact", WorldEdit 7.4.0 fabric): `Version=3`, `DataVersion=4671` (~1.21).
  `Blocks:{Palette(167), Data(ByteArray)}`, **bits=8 = 1 byte/bloco**, sem varint.
  Índice lido direto: `vals[i] = data[i] & 0xFF` (os bytes vêm assinados do nbtlib).
- **fortress** (Litematica): `Version=7`, `MinecraftDataVersion=3955` (~1.20), `Regions/Unnamed`,
  `BlockStatePalette` + `BlockStates` (longs bit-packed, LSB-first, bits = max(bit_length(len-1),4)).
  Ordem validada contra `litemapy`.

## Mapeamento de blocos usado (moderno -> 1.7.10)
### Knight (estátua)
| Moderno | 1.7.10 (id, data) |
|---|---|
| stone | (1,0) |
| andesite / polished_andesite | (1,0) stone |
| blackstone | (49,0) obsidian |
| black_concrete | (35,15) lã preta |
| mossy_cobblestone / moss_block | (48,0) cobblestone musgosa |
| moss_carpet | (171,13) carpete verde |
| cobbled_deepslate | (4,0) cobblestone |
| deepslate_bricks/tiles, polished_tuff | (98,0) stone bricks |
| ochre_froglight | (89,0) glowstone |
| orange/yellow_stained_glass | (95,1)/(95,4) vidro laranja/amarelo |
| (…)_stained_glass_pane | (160,1)/(160,4) painel de vidro |
| andesite/cobbled_deepslate/polished_tuff_stairs | (67,±) escada de cobblestone |
| deepslate_brick/tile_stairs | (109,±) escada de stone brick |
| *slabs (andesite/etc)* | (44,±) laje de pedra |
| *walls* (blackstone/cobbled_deepslate/deepslate/tuff) | (139,0) muro de cobblestone |

### Fortress (fortaleza)
| Moderno | 1.7.10 (id, data) |
|---|---|
| grass_block | (2,0) |
| dirt | (3,0) |
| cobblestone | (4,0) |
| oak_planks / spruce_planks | (5,0) / (5,1) |
| stone_bricks / cracked_stone_bricks | (98,0) / (98,0) — quebrado vira tijolo normal |
| tuff / tuff_bricks | (98,0) stone bricks |
| cobweb | (30,0) |
| cobblestone_wall / stone_brick_wall / tuff_wall | (139,0) muro de cobblestone |
| spruce_fence | (188,0) cerca de abeto |
| red_carpet | (171,14) carpete vermelha |
| light_gray_candle | (89,0) glowstone |
| barrel / stripped_spruce_log | (17,1) tronco de abeto |
| stripped_oak_log | (17,0) tronco de carvalho |
| stone_brick_stairs / spruce_stairs / oak_stairs | (109,±) / (134,±) / (53,±) |
| stone_brick_slab / spruce_slab | (44,5±) / (126,1±) |
| ladder / wall_torch | (65,±) / (50,±) |
| spruce_trapdoor | (96,±) |
| spruce_door | (194,±) |

Meta-direção das escadas/portas/tochas/lajes convertida das `Properties` modernas.

### Castle (castelo)
| Moderno | 1.7.10 (id, data) |
|---|---|
| grass_block / short_grass / tall_grass | (2,0) / (31,0) / (31,1) |
| bush | (32,0) dead bush |
| podzol / coarse_dirt / rooted_dirt / dirt / dirt_path | (3,2) / (3,1) / (3,0) / (3,0) / (3,0) |
| stone / cobblestone / polished_andesite | (1,0) / (4,0) / (1,0) |
| stone_bricks / mossy_stone_bricks / deepslate_tiles | (98,0) / (98,1) / (98,0) |
| oak_planks / spruce_planks / dark_oak_planks | (5,0) / (5,1) / (5,1) |
| white_concrete | (35,0) lã branca |
| dandelion | (37,0) |
| torch / wall_torch | (50,5) / (50,±) |
| lantern | (89,0) glowstone |
| lectern | (47,0) estante |
| stone_brick_wall / andesite_wall / diorite_wall / deepslate_tile_wall | (139,0) muro de cobblestone |
| iron_bars | (101,0) |
| blue_stained_glass_pane | (160,11) |
| spruce_fence / spruce_fence_gate | (188,0) / (184,±) |
| stone_brick_slab / deepslate_tile_slab / polished_andesite_slab | (44,5±) / (44,5±) / (44,0±) |
| cobblestone_slab | (44,2±) |
| spruce_slab | (126,1±) |
| stone_brick_stairs / deepslate_tile_stairs / polished_diorite_stairs / spruce_stairs | (109,±) / (109,±) / (67,±) / (134,±) |
| spruce_trapdoor / oak_trapdoor | (96,±) |
| dark_oak_door | (197,±) |

## Como rodar
```
python "C:\Users\Fernando\AppData\Local\Temp\opencode\schem\convert.py"
```
Gera ambos os arquivos `.schematic` no destino. Saída: `[knight] unmapped block types: NONE` etc.
Se aparecer `unmapped block types: <lista>`, é necessário adicionar entrada no mapa `KNIGHT`/`FORTRESS`.

Inspecionar visualmente (ASCII por fatia Y):
```
python "C:\Users\Fernando\AppData\Local\Temp\opencode\schem\render.py" <arquivo.schematic>
```

## Dependências Python
- Python 3.12.9, `nbtlib` 2.0.4, `litemapy` 0.11.0b0.
- `nbtlib.serialize_tag` emite SNBT (não binário) → por isso o `BinWriter` manual faz o .schematic binário.
