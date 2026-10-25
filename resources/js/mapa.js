/**
 * INTEGRAÇÃO FRONT-END / BACK-END DA 39ª CIA
 * Mapeamento dinâmico, hexágonos recortados, posicionamento de VTRs e seleção de horas.
 */
let deckgl = null;
let geojsonData = null;
let horaAtual = 20;

const estadoViaturas = {
  "VTR-1": true,
  "VTR-2": true,
  "VTR-3": true,
  "VTR-4": true,
  "VTR-5": true
};

const API_URL = 'http://localhost:8080/api/dados-mapa';

window.onload = async () => {
  deckgl = new deck.DeckGL({
    container: 'map',
    mapStyle: 'https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json',
    initialViewState: {
      longitude: -44.0508,
      latitude: -19.8670,
      zoom: 12.3,
      pitch: 0,
      bearing: 0
    },
    controller: true,
    layers: []
  });

  try {
    const res = await fetch('resources/js/39_cia.geojson');
    geojsonData = await res.json();

    // Carrega os dados iniciais das 20h
    await carregarDadosEColorir(horaAtual);
  } catch (err) {
    console.error("Erro ao carregar mapa base:", err);
  }
};

// Troca a hora e atualiza as cores da malha no mapa
async function selecionarHora(hora) {
  horaAtual = hora;

  // Atualiza label visual no painel
  const display = document.getElementById('display-hora-selecionada');
  if (display) display.innerText = `${String(hora).padStart(2, '0')}:00 h`;

  // Oculta o modal de seleção de hora
  const modal = document.getElementById('modal-horas');
  if (modal) modal.classList.add('hidden');

  // Requisita a nova análise ao servidor Java
  await carregarDadosEColorir(hora);
}

// Envia a hora e a lista de VTRs operacionais ao Java
async function carregarDadosEColorir(hora) {
  try {
    const viaturasAtivas = Object.keys(estadoViaturas)
      .filter(vtr => estadoViaturas[vtr])
      .join(',');

    const resposta = await fetch(`${API_URL}?hora=${hora}&ativas=${viaturasAtivas}`);
    const dadosJava = await resposta.json();

    desenharMalhaEViaturas(dadosJava);
  } catch (err) {
    console.error("Servidor Java não respondeu:", err);
  }
}

// Alterna status da VTR e recalcula posições das demais no Java
function toggleAtendimentoViatura(idViatura, buttonElement) {
  estadoViaturas[idViatura] = !estadoViaturas[idViatura];
  const estaAtiva = estadoViaturas[idViatura];

  const spanTexto = buttonElement.querySelector('span.text-slate-300, span.text-amber-300');
  const dotStatus = buttonElement.querySelector('.bg-cyan-400, .bg-amber-500');

  if (estaAtiva) {
    if (spanTexto) {
      spanTexto.innerText = "Colocar em atendimento";
      spanTexto.className = "text-slate-300 text-[10px] sm:text-xs lg:text-sm font-medium group-hover:text-white transition-colors text-left leading-tight mt-1";
    }
    if (dotStatus) {
      dotStatus.className = "w-1.5 h-1.5 lg:w-2 lg:h-2 rounded-full bg-cyan-400 shadow-[0_0_6px_#22d3ee] animate-pulse";
    }
    buttonElement.classList.remove('border-amber-500/50', 'bg-amber-950/20');
  } else {
    if (spanTexto) {
      spanTexto.innerText = "Em atendimento (Retornar)";
      spanTexto.className = "text-amber-300 text-[10px] sm:text-xs lg:text-sm font-bold transition-colors text-left leading-tight mt-1";
    }
    if (dotStatus) {
      dotStatus.className = "w-1.5 h-1.5 lg:w-2 lg:h-2 rounded-full bg-amber-500 shadow-[0_0_6px_#f59e0b]";
    }
    buttonElement.classList.add('border-amber-500/50', 'bg-amber-950/20');
  }

  carregarDadosEColorir(horaAtual);
}

function desenharMalhaEViaturas(dadosJava) {
  if (!geojsonData) return;

  // Paleta de Cores em RGBA [R, G, B, Opacidade]
  const paletaCores = {
    5: [47, 0, 48, 210],     // Nível 5: Roxo Escuro (#2F0030) - Crítico
    4: [207, 2, 0, 180],     // Nível 4: Vermelho (#CF0200) - Alto
    3: [242, 67, 0, 160],    // Nível 3: Laranja Vivo (#F24300) - Médio
    2: [227, 133, 0, 140],   // Nível 2: Laranja Fechado (#E38500) - Baixo
    1: [238, 209, 0, 120]    // Nível 1: Amarelo (#EED100) - Muito Baixo
  };

  // Mapeia perigo calculado pelo Java por h3Index
  const mapaPerigo = {};
  if (dadosJava && dadosJava.hexagonos) {
    dadosJava.hexagonos.forEach(h => {
      mapaPerigo[h.h3Index] = h.nivelPerigo;
    });
  }

  // Recorte Geométrico do Contorno usando Turf.js
  let mapBoundary = geojsonData.features[0];
  if (geojsonData.features.length > 1) {
    mapBoundary = turf.combine(geojsonData).features[0];
  }

  const hexagonesIndex = new Set();
  geojsonData.features.forEach(f => {
    let geoms = f.geometry.type === "Polygon" ? [f.geometry.coordinates] : f.geometry.coordinates;
    geoms.forEach(polygon => {
      const coords = polygon[0].map(c => [c[1], c[0]]);
      const innerHexes = h3.polyfill(coords, 9);
      innerHexes.forEach(h => {
        hexagonesIndex.add(h);
        h3.kRing(h, 1).forEach(n => hexagonesIndex.add(n));
      });
    });
  });

  const clippedHexagons = [];
  Array.from(hexagonesIndex).forEach(hexId => {
    const boundary = h3.h3ToGeoBoundary(hexId);
    const coords = boundary.map(c => [c[1], c[0]]);
    coords.push(coords[0]);

    const hexPolygon = turf.polygon([coords]);
    try {
      const intersection = turf.intersect(hexPolygon, mapBoundary);
      if (intersection) {
        // Aplica o nível de perigo dinâmico retornado do Java
        const nivel = mapaPerigo[hexId] !== undefined ? mapaPerigo[hexId] : 1;
        intersection.properties = {
          h3Index: hexId,
          nivelPerigo: nivel,
          cor: paletaCores[nivel] || paletaCores[1]
        };
        clippedHexagons.push(intersection);
      }
    } catch (e) {}
  });

  const clippedGeoJSON = turf.featureCollection(clippedHexagons);

  // 1. CAMADA DOS HEXÁGONOS RECORTADOS
  // Nota: O ID inclui 'horaAtual' e um timestamp/random para invalidar o cache do deck.gl
  const camadaHexagonos = new deck.GeoJsonLayer({
    id: `camada-malha-h3-${horaAtual}-${Date.now()}`,
    data: clippedGeoJSON,
    filled: true,
    stroke: true,
    getFillColor: d => d.properties.cor,
    getLineColor: [100, 40, 20, 150],
    getLineWidth: 1,
    lineWidthUnits: 'pixels',
    updateTriggers: {
      getFillColor: [horaAtual, JSON.stringify(mapaPerigo)]
    }
  });

  // 2. CAMADA DO CONTORNO AZUL DA 39ª CIA
  const camadaContorno = new deck.GeoJsonLayer({
    id: 'camada-contorno',
    data: geojsonData,
    filled: false,
    stroke: true,
    getLineColor: [0, 80, 200, 255],
    getLineWidth: 3,
    lineWidthUnits: 'pixels'
  });

  // 3. CAMADA DAS VIATURAS
  let camadaViaturas = null;
  if (dadosJava && dadosJava.viaturas && dadosJava.viaturas.length > 0) {
    const vtrData = dadosJava.viaturas
      .filter(vtr => vtr.h3Index && vtr.h3Index.trim() !== '')
      .map(vtr => {
        const centro = h3.h3ToGeo(vtr.h3Index);
        return { id: vtr.idViatura, coordinates: [centro[1], centro[0]] };
      });

    camadaViaturas = new deck.IconLayer({
      id: `camada-viaturas-${Date.now()}`,
      data: vtrData,
      getPosition: d => d.coordinates,
      getIcon: d => ({
        url: 'https://img.icons8.com/color/48/police-car.png',
        width: 48,
        height: 48,
        anchorY: 48
      }),
      getSize: 36,
      updateTriggers: {
        data: [horaAtual, vtrData.length]
      }
    });
  }

  // Monta as camadas ativas
  const layers = [camadaHexagonos, camadaContorno];
  if (camadaViaturas) layers.push(camadaViaturas);

  deckgl.setProps({ layers });
}

function toggleMenuHoras() {
  const modal = document.getElementById('modal-horas');
  if (modal) modal.classList.toggle('hidden');
}