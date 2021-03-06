// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.exoplanet.generator.providers;

import org.joml.Vector2f;
import org.joml.Vector2ic;
import org.terasology.engine.utilities.procedural.BrownianNoise;
import org.terasology.engine.utilities.procedural.PerlinNoise;
import org.terasology.engine.utilities.procedural.SubSampledNoise;
import org.terasology.engine.world.generation.Facet;
import org.terasology.engine.world.generation.FacetProvider;
import org.terasology.engine.world.generation.GeneratingRegion;
import org.terasology.engine.world.generation.Requires;
import org.terasology.engine.world.generation.Updates;
import org.terasology.exoplanet.generator.facets.ExoplanetHumidityFacet;
import org.terasology.exoplanet.generator.facets.ExoplanetSurfaceHeightFacet;
import org.terasology.exoplanet.generator.facets.ExoplanetSurfaceTempFacet;
import org.terasology.math.TeraMath;

import java.util.Iterator;

@Updates(@Facet(ExoplanetSurfaceHeightFacet.class))
@Requires({@Facet(ExoplanetSurfaceTempFacet.class), @Facet(ExoplanetHumidityFacet.class)})
public class ExoplanetMountainsProvider implements FacetProvider {
    private SubSampledNoise mountainNoise;
    private SubSampledNoise hillNoise;

    private float amplitude;

    public ExoplanetMountainsProvider(float amplitude) {
        this.amplitude = amplitude;
    }

    @Override
    public void setSeed(long seed) {
        mountainNoise = new SubSampledNoise(new BrownianNoise(new PerlinNoise(seed + 2), 8),
                new Vector2f(0.0003f, 0.0003f), 4);
        hillNoise = new SubSampledNoise(new BrownianNoise(new PerlinNoise(seed + 3)),
                new Vector2f(0.0007f, 0.0007f), 4);
    }

    @Override
    public void process(GeneratingRegion region) {
        ExoplanetSurfaceHeightFacet facet = region.getRegionFacet(ExoplanetSurfaceHeightFacet.class);

        float[] mountainData = mountainNoise.noise(facet.getWorldArea());
        float[] hillData = hillNoise.noise(facet.getWorldArea());

        ExoplanetSurfaceTempFacet tempFacet = region.getRegionFacet(ExoplanetSurfaceTempFacet.class);
        ExoplanetHumidityFacet humidityFacet = region.getRegionFacet(ExoplanetHumidityFacet.class);

        float[] heightData = facet.getInternal();
        Iterator<Vector2ic> positionIterator = facet.getRelativeArea().iterator();
        for (int i = 0; i < heightData.length; ++i) {
            Vector2ic pos = positionIterator.next();
            float temp = tempFacet.get(pos);
            float hum = humidityFacet.get(pos);
            Vector2f distanceToMountainBiome = new Vector2f(temp - 0.25f, (temp * hum) - 0.35f);
            float mIntens = TeraMath.clamp(1.0f - distanceToMountainBiome.length() * 3.0f);
            float densityMountains = Math.max(mountainData[i] * 2.12f, 0) * mIntens * amplitude;
            float densityHills = Math.max(hillData[i] * 2.12f - 0.1f, 0) * (1.0f - mIntens) * amplitude;

            heightData[i] = heightData[i] + 512 * densityMountains + 64 * densityHills;
        }
    }
}

