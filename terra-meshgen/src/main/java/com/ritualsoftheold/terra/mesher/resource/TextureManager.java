package com.ritualsoftheold.terra.mesher.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.material.TerraTexture;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Manages textures of materials. Creates texture atlases.
 *
 */
public class TextureManager {
    
    private static final int TEXTURE_MIN_RES = 2;
    private static final int ATLAS_SIZE = 4096;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int ATLAS_SIZE_IMAGE = ATLAS_SIZE * BYTES_PER_PIXEL;
    
    private static final int IMAGE_UP_LEFT = ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL;
    
    private Int2ObjectMap<TerraTexture> textures;
    private AssetManager assetManager;
    
    private TextureArray array;
    private List<Image> atlases;
    
    public TextureManager(AssetManager assetManager) {
        textures = new Int2ObjectArrayMap<>();
        this.assetManager = assetManager;
    }
    
    /**
     * Returns texture array used for ground texture.
     * @return Ground texture array.
     */
    public TextureArray getGroundTexture() {
        return array;
    }
    
    public Image getAtlas(int index) {
        return atlases.get(index);
    }
    
    public TerraTexture getTexture(int worldId) {
        return textures.get(worldId);
    }
    
    public void loadMaterials(MaterialRegistry reg) {
        textures.clear(); // Clear previous textures
        
        Int2ObjectMap<List<TerraTexture>> resulutions = new Int2ObjectArrayMap<>();
        
        for (TerraMaterial mat : reg.getAllMaterials()) {
            TerraTexture texture = mat.getTexture();
            if (texture == null) {
                continue; // This material has no texture (e.g. air)
            }
            textures.put(mat.getWorldId(), texture); // Put texture to map
            
            int width = texture.getWidth();
            int height = texture.getHeight();
            
            // TODO check that texture is power of 2
            if (width != height) {
                throw new UnsupportedOperationException("non-square textures are not yet supported");
            }
            
            List<TerraTexture> sameRes = resulutions.getOrDefault(width, new ArrayList<>()); // Get or create list of others with same res
            sameRes.add(texture); // Add this texture to list
            resulutions.put(width, sameRes); // Re-put list if we actually only just created it
        }
        
        List<Image> atlases = new ArrayList<>(); // All texture atlases go here
        for (Entry<List<TerraTexture>> e : resulutions.int2ObjectEntrySet()) {
            generateAtlases(e.getValue(), e.getIntKey(), atlases); // Generate atlases...
        }
        
        // TODO make these configurable, Rituals art style already changed a bit since I wrote this
        array = new TextureArray(atlases);
        array.setMagFilter(MagFilter.Nearest);
        array.setMinFilter(MinFilter.NearestNoMipMaps);
        this.atlases = atlases;
    }
    
    private void generateAtlases(List<TerraTexture> textures, int size, List<Image> atlases) {
        int texturesPerSide = ATLAS_SIZE / size;
        
        int x = 0;
        int y = 0;
        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL); // 4 for alpha channel+colors, TODO configurable
        for (TerraTexture texture : textures) {
            Image img = assetManager.loadTexture(texture.getAsset()).getImage(); // Use asset manager to load
            if (x == texturesPerSide) { // Pick next row
                x = 0;
                y++;
            } if (y == texturesPerSide) { // Out of y values... need next atlas
                Image readyAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
                atlases.add(readyAtlas);
                atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL);
            }
            
            int atlasStart = x * size * BYTES_PER_PIXEL + y * size * ATLAS_SIZE_IMAGE;
            
            ByteBuffer imgData = img.getData(0);
            for (int i = 0; i < size; i++) {
                byte[] row = new byte[size * BYTES_PER_PIXEL]; // Create array for one row of image data
                imgData.position(i * size * BYTES_PER_PIXEL);
                imgData.get(row); // Copy one row of data to array
                atlasBuf.position(atlasStart + i * ATLAS_SIZE_IMAGE); // Travel to correct point in atlas data
                atlasBuf.put(row); // Set a row of data to atlas
            }
            
            // Assign texture data for shader
            texture.setPage(atlases.size()); // Texture array id, "page"
            texture.setTileId(y * texturesPerSide + x); // Texture tile id
            texture.setTexturesPerSide(texturesPerSide); // For MeshContainer
            
            x++;
        }
        
        // Not full atlas, but not empty either
        if (atlasBuf.position() != 0) {
            System.out.println("Incomplete atlas");
            Image incompleteAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
            atlases.add(incompleteAtlas);
        }
    }

    public int getAtlasSize() {
        return ATLAS_SIZE;
    }
    
}
