package com.ritualsoftheold.terra.material;

import com.ritualsoftheold.terra.TerraModule;

/**
 * Represents a material of block.
 *
 */
public class TerraMaterial {
    
    /**
     * Constructs a new material builder.
     * @return Material builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    private TerraMaterial() {} // Only builder can create this
    
    // Set by material registry
    private int worldId;
    private String fullName;
    
    // Set by builder
    private TerraModule mod;
    private String name;
    
    // Set by TextureManager (on client side)
    private TerraTexture texture;
    
    /**
     * Gets this material's id in world data.
     * @return Material id.
     */
    public int getWorldId() {
        return worldId;
    }
    
    void setWorldId(short id) {
        worldId = id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    void setFullName(String name) {
        fullName = name;
    }
    
    public TerraTexture getTexture() {
        return texture;
    }
    
    /**
     * Builder for Terra's materials.
     *
     */
    public static class Builder {
        
        /**
         * Reference to material we are building right now.
         */
        private TerraMaterial material;
        
        private Builder() {
            material = new TerraMaterial();
        }
        
        public Builder module(TerraModule mod) {
            material.mod = mod;
            return this;
        }
        
        public Builder name(String name) {
            material.name = name;
            return this;
        }
        
        public Builder texture(TerraTexture texture) {
            material.texture = texture;
            return this;
        }
        
        public TerraMaterial build() {
            return material;
        }
    }
}
