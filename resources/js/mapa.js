/**
 * INTEGRAÇÃO FRONT-END / BACK-END DA 39ª CIA
 * Mapeamento dinâmico, hexágonos recortados, exibição de peso, posicionamento de VTRs e Cartão Programa.
 */

// 1. VARIÁVEIS GLOBAIS
let deckgl = null;
let geojsonData = null;
let horaAtual = 20;
let vtrCartaoAberta = null;

const estadoViaturas = {
  "VTR-1": true,
  "VTR-2": true,
  "VTR-3": true,
  "VTR-4": true,
  "VTR-5": true
};

const API_URL = 'http://localhost:8080/api/dados-mapa';

// 2. INICIALIZAÇÃO DO MAPA
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

    await carregarDadosEColorir(horaAtual);
  } catch (err) {
    console.error("Erro ao carregar mapa base:", err);
  }
};

// 3. SELEÇÃO DE HORÁRIO
async function selecionarHora(hora) {
  horaAtual = hora;

  const display = document.getElementById('display-hora-selecionada');
  if (display) display.innerText = `${String(hora).padStart(2, '0')}:00 h`;

  const modal = document.getElementById('modal-horas');
  if (modal) modal.classList.add('hidden');

  await carregarDadosEColorir(hora);

  if (vtrCartaoAberta) {
    carregarCartaoPrograma(vtrCartaoAberta);
  }
}

// 4. CARREGAMENTO DOS DADOS DO MAPA
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

// 5. ALTERNAR STATUS DA VIATURA
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

  if (vtrCartaoAberta) {
    carregarCartaoPrograma(vtrCartaoAberta);
  }
}

// 6. RENDERIZAR CAMADAS NO DECK.GL
function desenharMalhaEViaturas(dadosJava) {
  if (!geojsonData) return;

  const paletaCores = {
    5: [47, 0, 48, 210],
    4: [207, 2, 0, 180],
    3: [242, 67, 0, 160],
    2: [227, 133, 0, 140],
    1: [238, 209, 0, 120]
  };

  const mapaPerigo = {};
  const mapaPeso = {};
  if (dadosJava && dadosJava.hexagonos) {
    dadosJava.hexagonos.forEach(h => {
      mapaPerigo[h.h3Index] = h.nivelPerigo;
      mapaPeso[h.h3Index] = h.pesoHorario || 0;
    });
  }

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
  const dadosTextoHex = [];

  Array.from(hexagonesIndex).forEach(hexId => {
    const boundary = h3.h3ToGeoBoundary(hexId);
    const coords = boundary.map(c => [c[1], c[0]]);
    coords.push(coords[0]);

    const hexPolygon = turf.polygon([coords]);
    try {
      const intersection = turf.intersect(hexPolygon, mapBoundary);
      if (intersection) {
        const nivel = mapaPerigo[hexId] !== undefined ? mapaPerigo[hexId] : 1;
        const peso = mapaPeso[hexId] !== undefined ? mapaPeso[hexId] : 0;

        intersection.properties = {
          h3Index: hexId,
          nivelPerigo: nivel,
          cor: paletaCores[nivel] || paletaCores[1]
        };
        clippedHexagons.push(intersection);

        const centro = turf.centroid(intersection);
        dadosTextoHex.push({
          coordinates: centro.geometry.coordinates,
          texto: String(peso),
          nivel: nivel
        });
      }
    } catch (e) {}
  });

  const clippedGeoJSON = turf.featureCollection(clippedHexagons);

  // Camada 1: Hexágonos
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

  // Camada 2: Números do Peso
  const camadaTextos = new deck.TextLayer({
    id: `camada-texto-pontuacao-${horaAtual}-${Date.now()}`,
    data: dadosTextoHex,
    getPosition: d => d.coordinates,
    getText: d => d.texto,
    getSize: 11,
    getColor: d => d.nivel >= 4 ? [255, 255, 255, 240] : [15, 23, 42, 220],
    getTextAnchor: 'middle',
    getAlignmentBaseline: 'center',
    fontFamily: 'Inter, monospace, sans-serif',
    fontWeight: 'bold',
    updateTriggers: {
      getText: [horaAtual, JSON.stringify(mapaPeso)],
      getColor: [horaAtual, JSON.stringify(mapaPerigo)]
    }
  });

  // Camada 3: Contorno Azul
  const camadaContorno = new deck.GeoJsonLayer({
    id: 'camada-contorno',
    data: geojsonData,
    filled: false,
    stroke: true,
    getLineColor: [0, 80, 200, 255],
    getLineWidth: 3,
    lineWidthUnits: 'pixels'
  });

  // Camada 4: Viaturas
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

  const layers = [camadaHexagonos, camadaTextos, camadaContorno];
  if (camadaViaturas) layers.push(camadaViaturas);

  deckgl.setProps({ layers });
}

// 7. CARTÃO PROGRAMA
async function carregarCartaoPrograma(idViatura) {
  try {
    vtrCartaoAberta = idViatura;
    const res = await fetch(`http://localhost:8080/api/cartao-programa?vtr=${idViatura}&hora=${horaAtual}`);
    const dados = await res.json();

    renderizarBarra24H(dados.idViatura, dados.comportamentos);
  } catch (err) {
    console.error("Erro ao carregar Cartão Programa:", err);
  }
}

function renderizarBarra24H(idViatura, comportamentos) {
  const container = document.getElementById('barra-cartao-programa');
  const titulo = document.getElementById('vtr-cartao-titulo');
  const grid = document.getElementById('grid-24h-cartao');

  if (!container || !grid) return;

  titulo.innerText = idViatura;
  grid.innerHTML = '';

  const coresComportamento = {
    1: 'bg-red-600 text-white border-red-400 shadow-[0_0_8px_rgba(220,38,38,0.5)]',
    2: 'bg-orange-500 text-slate-950 border-orange-300 shadow-[0_0_8px_rgba(249,115,22,0.5)]',
    3: 'bg-yellow-400 text-slate-950 border-yellow-200 shadow-[0_0_8px_rgba(250,204,21,0.5)]'
  };

  comportamentos.forEach((comp, hora) => {
    const horaFormatada = String(hora).padStart(2, '0');
    const classeCor = coresComportamento[comp] || 'bg-slate-700 text-slate-300';
    const bordaDestaque = (hora === horaAtual) ? 'ring-2 ring-white scale-105 z-10' : '';

    const divBloco = document.createElement('div');
    divBloco.className = `flex flex-col items-center justify-center p-1 rounded border text-center transition-all ${classeCor} ${bordaDestaque}`;
    divBloco.title = `Hora: ${horaFormatada}:00h - Comportamento: ${comp}`;
    divBloco.innerHTML = `
      <span class="text-[9px] font-mono opacity-80 leading-none">${horaFormatada}h</span>
      <span class="text-xs font-bold font-mono leading-tight mt-0.5">${comp}</span>
    `;

    grid.appendChild(divBloco);
  });

  container.classList.remove('hidden');
}

function fecharBarraCartao() {
  vtrCartaoAberta = null;
  const container = document.getElementById('barra-cartao-programa');
  if (container) container.classList.add('hidden');
}

function toggleMenuHoras() {
  const modal = document.getElementById('modal-horas');
  if (modal) modal.classList.toggle('hidden');
}