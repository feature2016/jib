/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.crepecake.cache;

import com.google.cloud.tools.crepecake.image.DuplicateLayerException;
import com.google.cloud.tools.crepecake.image.ImageLayers;
import com.google.cloud.tools.crepecake.image.LayerPropertyNotFoundException;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * The cache stores all the layer BLOBs as separate files and the cache metadata contains
 * information about each layer BLOB.
 */
class CacheMetadata {

  private final ImageLayers<CachedLayerWithMetadata> layers = new ImageLayers<>();

  /** Can be used to filter layers in the metadata. */
  static class LayerFilter {

    private final ImageLayers<CachedLayerWithMetadata> layers;

    @Nullable private CachedLayerType type;
    @Nullable private Set<File> sourceDirectories;

    /** True if the filters are used; false otherwise. */
    private boolean isTypeFilterEnabled = false;

    private boolean isSourceDirectoriesFilterEnabled = false;

    private LayerFilter(ImageLayers<CachedLayerWithMetadata> layers) {
      this.layers = layers;
    }

    /** Filters to a certain layer type. */
    LayerFilter byType(CachedLayerType type) {
      this.type = type;
      isTypeFilterEnabled = true;
      return this;
    }

    /** Filters to a certain set of source directories. */
    LayerFilter bySourceDirectories(Set<File> sourceDirectories) {
      this.sourceDirectories = sourceDirectories;
      isSourceDirectoriesFilterEnabled = true;
      return this;
    }

    /** Applies the filters to the metadata layers. */
    ImageLayers<CachedLayerWithMetadata> filter() throws CacheMetadataCorruptedException {
      try {
        ImageLayers<CachedLayerWithMetadata> filteredLayers = new ImageLayers<>();

        for (CachedLayerWithMetadata layer : layers) {
          if (isTypeFilterEnabled) {
            if (type != layer.getMetadata().getType()) {
              continue;
            }
          }

          if (isSourceDirectoriesFilterEnabled) {
            List<String> cachedLayerSourceDirectoryPaths =
                layer.getMetadata().getSourceDirectories();
            if (cachedLayerSourceDirectoryPaths == null) {
              if (sourceDirectories != null) {
                continue;
              }
            } else {
              Set<File> cachedLayerSourceDirectories = new HashSet<>();
              for (String sourceDirectory : cachedLayerSourceDirectoryPaths) {
                cachedLayerSourceDirectories.add(new File(sourceDirectory));
              }
              if (!cachedLayerSourceDirectories.equals(sourceDirectories)) {
                continue;
              }
            }
          }

          filteredLayers.add(layer);
        }

        return filteredLayers;

      } catch (DuplicateLayerException | LayerPropertyNotFoundException ex) {
        throw new CacheMetadataCorruptedException(ex);
      }
    }
  }

  ImageLayers<CachedLayerWithMetadata> getLayers() {
    return layers;
  }

  void addLayer(CachedLayerWithMetadata layer)
      throws LayerPropertyNotFoundException, DuplicateLayerException {
    layers.add(layer);
  }

  LayerFilter filterLayers() {
    return new LayerFilter(layers);
  }
}
