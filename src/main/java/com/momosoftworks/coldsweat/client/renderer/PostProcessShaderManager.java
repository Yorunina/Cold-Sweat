package com.momosoftworks.coldsweat.client.renderer;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostProcessShaderManager implements AutoCloseable
{
    private static final PostProcessShaderManager INSTANCE = new PostProcessShaderManager(
            Minecraft.getInstance().getMainRenderTarget(),
            Minecraft.getInstance().getResourceManager());

    private static final Map<String, PostChain> ACTIVE_EFFECTS = new HashMap<>();
    private final RenderTarget mainTarget;
    private final ResourceManager resourceManager;
    private boolean screenSizeUpdated = false;

    public static final ResourceLocation BLOBS = ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/post/blobs2.json");

    public PostProcessShaderManager(RenderTarget mainTarget, ResourceManager resourceManager)
    {
        this.mainTarget = mainTarget;
        this.resourceManager = resourceManager;
    }

    public static PostProcessShaderManager getInstance()
    {   return INSTANCE;
    }

    public void loadEffect(String id, ResourceLocation shaderLocation)
    {
        try
        {
            // Create new PostChain for this effect
            PostChain postChain = new PostChain(
                    Minecraft.getInstance().getTextureManager(),
                    resourceManager,
                    mainTarget,
                    shaderLocation
            );

            // Store it in our active effects
            if (ACTIVE_EFFECTS.put(id, postChain) != null)
            {
                // If we're replacing an existing effect, close the old one
                closeEffect(id);
            }

        }
        catch (IOException | JsonSyntaxException e)
        {   ColdSweat.LOGGER.error("Failed to load shader effect: " + id, e);
        }
    }

    public void closeEffect(String id)
    {
        PostChain chain = ACTIVE_EFFECTS.remove(id);
        if (chain != null)
        {
            chain.close();
        }
    }

    public void process(float partialTicks)
    {
        // Process all active effects
        for (PostChain chain : ACTIVE_EFFECTS.values())
        {
            if (getPostPasses(chain).stream().anyMatch(pass -> getOrthoMatrix(pass) == null))
            {   chain.resize(mainTarget.width, mainTarget.height);
            }
            chain.process(partialTicks);
        }
    }

    public void resize(int width, int height)
    {
        // Resize all active effects
        for (PostChain chain : ACTIVE_EFFECTS.values())
        {
            chain.resize(width, height);
        }
    }

    public boolean hasEffect(String id)
    {   return ACTIVE_EFFECTS.containsKey(id);
    }

    public PostChain getEffect(String id)
    {   return ACTIVE_EFFECTS.get(id);
    }

    private static final Field POST_PASSES = ObfuscationReflectionHelper.findField(PostChain.class, "passes");
    private static final Field ORTHO_MATRIX = ObfuscationReflectionHelper.findField(PostPass.class, "shaderOrthoMatrix");
    static
    {   POST_PASSES.setAccessible(true);
        ORTHO_MATRIX.setAccessible(true);
    }

    public List<PostPass> getPostPasses(String effectId)
    {
        PostChain chain = ACTIVE_EFFECTS.get(effectId);
        if (chain == null)
        {   return List.of();
        }
        try
        {  return (List<PostPass>) POST_PASSES.get(chain);
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Failed to get post passes for effect: " + effectId, e);
            return List.of();
        }
    }

    public static List<PostPass> getPostPasses(PostChain chain)
    {
        try
        {  return (List<PostPass>) POST_PASSES.get(chain);
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Failed to get post passes for effect: " + chain, e);
            return List.of();
        }
    }

    public static Matrix4f getOrthoMatrix(PostPass pass)
    {
        try
        {  return (Matrix4f) ORTHO_MATRIX.get(pass);
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Failed to get ortho matrix for pass: " + pass, e);
            return new Matrix4f();
        }
    }

    @Override
    public void close()
    {
        // Clean up all effects
        for (PostChain chain : ACTIVE_EFFECTS.values())
        {
            chain.close();
        }
        ACTIVE_EFFECTS.clear();
    }
}