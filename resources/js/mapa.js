/**
 * INTEGRAÇÃO FRONT-END / BACK-END DA 39ª CIA
 */
let deckgl = null;
let geojsonData = null;
let horaAtual = 20;

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

// Troca a hora e atualiza as cores do mapa
async function selecionarHora(hora) {
  horaAtual = hora;

  // Atualiza label visual
  const display = document.getElementById('display-hora-selecionada');
  if (display) display.innerText = `${String(hora).padStart(2, '0')}:00 h`;

  // Requisita a nova análise ao Java
  await carregarDadosEColorir(hora);
}

async function carregarDadosEColorir(hora) {
  try {
    const resposta = await fetch(`${API_URL}?hora=${hora}`);
    const dadosJava = await resposta.json();

    desenharMalhaEViaturas(dadosJava);
  } catch (err) {
    console.error("Servidor Java não respondeu:", err);
  }
}

function desenharMalhaEViaturas(dadosJava) {
  if (!geojsonData) return;

  // Paleta de Cores em RGBA [R, G, B, Opacidade]
  const paletaCores = {
    5: [47, 0, 48, 210],     // Nível 5: Roxo Escuro / Bordo Escuro (#2F0030) - Crítico
    4: [207, 2, 0, 180],     // Nível 4: Vermelho (#CF0200) - Alto
    3: [242, 67, 0, 160],    // Nível 3: Laranja Vivo (#F24300) - Médio
    2: [227, 133, 0, 140],   // Nível 2: Laranja Fechado (#E38500) - Baixo
    1: [238, 209, 0, 120]
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
        // Aplica a cor do nível de perigo calculado pelo Java para a hora
        const nivel = mapaPerigo[hexId] || 1;
        intersection.properties = {
          h3Index: hexId,
          nivelPerigo: nivel,
          cor: paletaCores[nivel]
        };
        clippedHexagons.push(intersection);
      }
    } catch (e) {}
  });

  const clippedGeoJSON = turf.featureCollection(clippedHexagons);

  // Camada dos Hexágonos Recortados
  const camadaHexagonos = new deck.GeoJsonLayer({
    id: 'camada-malha-h3-clipped',
    data: clippedGeoJSON,
    filled: true,
    stroke: true,
    getFillColor: d => d.properties.cor,
    getLineColor: [100, 40, 20, 150],
    getLineWidth: 1,
    lineWidthUnits: 'pixels',
    updateTriggers: {
      getFillColor: [horaAtual] // Garante que re-renderize as cores ao mudar a hora
    }
  });

  // Camada do Contorno Azul
  const camadaContorno = new deck.GeoJsonLayer({
    id: 'camada-contorno',
    data: geojsonData,
    filled: false,
    stroke: true,
    getLineColor: [0, 80, 200, 255],
    getLineWidth: 3,
    lineWidthUnits: 'pixels'
  });

  // Camada das Viaturas
  let camadaViaturas = null;
  if (dadosJava && dadosJava.viaturas) {
    const vtrData = dadosJava.viaturas.map(vtr => {
      const centro = h3.h3ToGeo(vtr.h3Index);
      return { id: vtr.idViatura, coordinates: [centro[1], centro[0]] };
    });

    camadaViaturas = new deck.IconLayer({
      id: 'camada-viaturas',
      data: vtrData,
      getPosition: d => d.coordinates,
      getIcon: d => ({
        url: 'https://img.icons8.com/color/48/police-car.png',
        width: 48,
        height: 48,
        anchorY: 48
      }),
      getSize: 36
    });
  }

  const layers = [camadaHexagonos, camadaContorno];
  if (camadaViaturas) layers.push(camadaViaturas);

  deckgl.setProps({ layers });
}

function toggleMenuHoras() {
  const modal = document.getElementById('modal-horas');
  if (modal) modal.classList.toggle('hidden');
}