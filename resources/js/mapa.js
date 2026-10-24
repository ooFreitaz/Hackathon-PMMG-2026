window.onload = async () => {
      if (typeof deck === 'undefined' || typeof h3 === 'undefined' || typeof turf === 'undefined') {
        console.error("Falha ao carregar dependências.");
        return;
      }

      const deckgl = new deck.DeckGL({
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
        const resposta = await fetch('resources/js/39_cia.geojson');
        if (!resposta.ok) throw new Error("Erro ao carregar 39_cia.geojson");
        const geojsonData = await resposta.json();

        // Extrai a geometria unificada do contorno principal usando Turf
        let mapBoundaryPolygon = geojsonData.features[0];
        if (geojsonData.features.length > 1) {
          mapBoundaryPolygon = turf.combine(geojsonData).features[0];
        }

        const hexagonesIndex = new Set();
        
        // 1. Coleta os hexágonos internos e os vizinhos da borda
        geojsonData.features.forEach(feature => {
          let geoms = [];
          if (feature.geometry.type === "Polygon") {
            geoms = [feature.geometry.coordinates];
          } else if (feature.geometry.type === "MultiPolygon") {
            geoms = feature.geometry.coordinates;
          }

          geoms.forEach(polygon => {
            const polygonCoords = polygon[0].map(coord => [coord[1], coord[0]]);
            const innerHexes = h3.polyfill(polygonCoords, 9);
            
            innerHexes.forEach(h => {
              hexagonesIndex.add(h);
              // Expande 1 anel para cobrir as extremidades da margem
              const ring = h3.kRing(h, 1);
              ring.forEach(neighbor => hexagonesIndex.add(neighbor));
            });
          });
        });

        // 2. Transforma os hexágonos em Polígonos e faz o RECORTE (intersect) com a borda do mapa
        const clippedHexagons = [];

        Array.from(hexagonesIndex).forEach(hexId => {
          const boundary = h3.h3ToGeoBoundary(hexId);
          // O H3 retorna [lat, lng], invertemos para [lng, lat] que o GeoJSON/Turf aceita
          const coordinates = boundary.map(coord => [coord[1], coord[0]]);
          // Fecha o anel do polígono
          coordinates.push(coordinates[0]);

          const hexPolygon = turf.polygon([coordinates]);

          // Realiza o corte geométrico exato entre o hexágono e o contorno do mapa
          try {
            const intersection = turf.intersect(hexPolygon, mapBoundaryPolygon);
            if (intersection) {
              clippedHexagons.push(intersection);
            }
          } catch (e) {
            // Em caso de falha de sobreposição exata no limite, ignora a célula inconsistente
          }
        });

        // Cria a coleção de feições recortadas
        const clippedGeoJSON = turf.featureCollection(clippedHexagons);

        // 3. Camada dos Hexágonos Recortados (GeoJsonLayer para suportar geometrias recortadas)
        const camadaHexagonos = new deck.GeoJsonLayer({
          id: 'camada-malha-h3-clipped',
          data: clippedGeoJSON,
          filled: true,
          stroke: true,
          getFillColor: [255, 140, 0, 45],     // Laranja semi-transparente
          getLineColor: [200, 80, 0, 180],     // Borda Laranja
          getLineWidth: 1,
          lineWidthUnits: 'pixels',
          pickable: true
        });

        // 4. Camada do Contorno (Borda externa azul)
        const camadaContorno = new deck.GeoJsonLayer({
          id: 'camada-contorno',
          data: geojsonData,
          filled: false,
          stroke: true,
          getLineColor: [0, 50, 150, 255],
          getLineWidth: 3,
          lineWidthUnits: 'pixels'
        });

        deckgl.setProps({ layers: [camadaHexagonos, camadaContorno] });
        console.log(`Malha preenchida e recortada com sucesso no contorno do mapa!`);

      } catch (erro) {
        console.error("Erro ao processar e recortar malha H3:", erro);
      }
    };