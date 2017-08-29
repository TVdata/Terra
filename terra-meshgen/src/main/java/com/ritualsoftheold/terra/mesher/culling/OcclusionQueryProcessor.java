package com.ritualsoftheold.terra.mesher.culling;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.FrameBuffer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

/**
 * Uses OpenGL hardware occlusion queries to potentially reduce amount of
 * stuff that is rendered.
 *
 */
public class OcclusionQueryProcessor implements SceneProcessor {
    
    private VisualObject[] objs;
    private int objCount;
    private int extraAlloc;
    
    private Object2IntMap<VisualObject> queries;
    
    /**
     * Constructs a new occlusion query scene processor.
     * @param initialCount How many visual objects there will be initially.
     * Must be at least 0.
     * @param extraAlloc How much array space will be allocated whenever
     * increasing array size. Must be at least 1!
     */
    public OcclusionQueryProcessor(int initialCount, int extraAlloc) {
        if (extraAlloc < 1) {
            throw new IllegalArgumentException("extraAlloc must be at least");
        } else if (initialCount < 0) {
            throw new IllegalArgumentException("initialCount cannot be negative");
        }
        
        this.objs = new VisualObject[initialCount + extraAlloc];
        this.objCount = 0;
        this.extraAlloc = extraAlloc;
        this.queries = new Object2IntOpenHashMap<>();
    }
    
    /**
     * Adds visual object to this query processor.
     * @param obj Object.
     * @return Index of the object.
     */
    public int addObject(VisualObject obj) {
        if (objs.length == objCount) {
            // Need to allocate more array
            VisualObject[] old = objs;
            objs = new VisualObject[objCount + extraAlloc];
            System.arraycopy(old, 0, objs, 0, objCount);
        }
        objs[objCount] = obj;
        objCount++;
        
        return objCount - 1;
    }
    
    public void removeObject(int index) {
        objs[index] = null;
    }
    
    public void removeObject(VisualObject obj) {
        removeObject(obj.cullingId);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // No need to do anything here
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void preFrame(float tpf) {
        // Mark objects which are not to be rendered according to queries

        for (VisualObject obj : objs) {
            if (obj == null) { // Ignore null objects...
                continue;
            }
            
            int queryId = queries.getInt(obj);
            if (glGetQueryObjecti(queryId, GL_QUERY_RESULT_AVAILABLE) != GL_FALSE) {
                int result = glGetQueryi(queryId, GL_QUERY_RESULT);
                if (result == GL_FALSE) {
                    obj.linkedGeom.setCullHint(CullHint.Always);
                }
            } else { // We better render this stuff, as we have no idea if it is needed or not
                obj.linkedGeom.setCullHint(CullHint.Never);
            }
        }
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // We cannot remove objects at this stage, so do nothing
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // Do queries for next frame
        
        // Make sure that our query objects are not actually rendered
        glColorMask(false, false, false, false); // This also increases performance
        glDepthMask(false);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertices = stack.mallocFloat(108 * objCount);
            
            for (int i = 0; i < objCount; i++) {
                VisualObject obj = objs[i];
                if (obj == null) { // Ignore null objects...
                    continue;
                }
                
                float posX = obj.posX;
                float posY = obj.posY;
                float posZ = obj.posZ;
                float posMod = obj.posMod;
                
                // LEFT
                vertices.put(posX - posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ - posMod);
                
                // RIGHT
                vertices.put(posX + posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ + posMod);
                
                // UP
                vertices.put(posX - posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ + posMod)
                
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ - posMod);
                
                // DOWN
                vertices.put(posX - posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ + posMod)
                
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ - posMod);
                
                // FRONT
                vertices.put(posX - posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ - posMod);
                
                // BACK
                vertices.put(posX - posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ + posMod)
                
                .put(posX + posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ + posMod);
            }
            vertices.flip(); // Needed by LWJGL to not crash horribly
            
            for (int i = 0; i < objCount; i++) {
                VisualObject obj = objs[i];
                
                int queryId = glGenQueries();
                
                glBeginQuery(GL_ANY_SAMPLES_PASSED, queryId);
                
                glDrawArrays(GL_TRIANGLES, 108 * i, 108);
                
                glEndQuery(GL_ANY_SAMPLES_PASSED);
                queries.put(obj, queryId);
            }
        }
        
        // Re-enable normal rendering
        glColorMask(true, true, true, true);
        glDepthMask(true);
    }

    @Override
    public void cleanup() {
        // We don't have resources that need cleanup
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        // New jME feature?
    }

}