package com.volt.gui.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MSAARoundedRectShader {
    private static MSAARoundedRectShader instance;
    private int shaderProgram;
    private int vertexShader;
    private int fragmentShader;
    
    private int uProjectionLoc;
    private int uModelViewLoc;
    private int uResolutionLoc;
    private int uColorLoc;
    private int uPositionLoc;
    private int uSizeLoc;
    private int uRadiusLoc;
    private int uBorderWidthLoc;
    private int uBorderColorLoc;
    private int uSamplesLoc;
    
    private int vao;
    private int vbo;
    
    private boolean initialized = false;
    
    public static MSAARoundedRectShader getInstance() {
        if (instance == null) {
            instance = new MSAARoundedRectShader();
        }
        return instance;
    }
    
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            String vertexSource = loadShaderSource("/shaders/msaa_rounded_rect.vert");
            String fragmentSource = loadShaderSource("/shaders/msaa_rounded_rect.frag");
            
            vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
            fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            
            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vertexShader);
            GL20.glAttachShader(shaderProgram, fragmentShader);
            GL20.glLinkProgram(shaderProgram);
            
            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                String error = GL20.glGetProgramInfoLog(shaderProgram);
                throw new RuntimeException("Shader program linking failed: " + error);
            }
            
            uProjectionLoc = GL20.glGetUniformLocation(shaderProgram, "uProjection");
            uModelViewLoc = GL20.glGetUniformLocation(shaderProgram, "uModelView");
            uResolutionLoc = GL20.glGetUniformLocation(shaderProgram, "uResolution");
            uColorLoc = GL20.glGetUniformLocation(shaderProgram, "uColor");
            uPositionLoc = GL20.glGetUniformLocation(shaderProgram, "uPosition");
            uSizeLoc = GL20.glGetUniformLocation(shaderProgram, "uSize");
            uRadiusLoc = GL20.glGetUniformLocation(shaderProgram, "uRadius");
            uBorderWidthLoc = GL20.glGetUniformLocation(shaderProgram, "uBorderWidth");
            uBorderColorLoc = GL20.glGetUniformLocation(shaderProgram, "uBorderColor");
            uSamplesLoc = GL20.glGetUniformLocation(shaderProgram, "uSamples");
            
            createQuadGeometry();
            
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MSAA rounded rect shader", e);
        }
    }
    
    private String loadShaderSource(String path) throws IOException {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Identifier identifier = Identifier.of("volt", path.substring(1));
        
        try (InputStream inputStream = resourceManager.getResource(identifier).get().getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String error = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + error);
        }
        
        return shader;
    }
    
    private void createQuadGeometry() {
        float[] vertices = {
            -1.0f, -1.0f, 0.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
             1.0f,  1.0f, 1.0f, 1.0f,
            -1.0f,  1.0f, 0.0f, 1.0f
        };
        
        int[] indices = {
            0, 1, 2,
            2, 3, 0
        };
        
        vao = GL30.glGenVertexArrays();
        vbo = GL20.glGenBuffers();
        int ebo = GL20.glGenBuffers();
        
        GL30.glBindVertexArray(vao);
        
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, vertices, GL20.GL_STATIC_DRAW);
        
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
    }
    
    public void drawRoundedRect(float x, float y, float width, float height, float radius, Color color, int samples) {
        drawRoundedRect(x, y, width, height, radius, color, 0, null, samples);
    }
    
    public void drawRoundedRect(float x, float y, float width, float height, float radius, 
                               Color fillColor, float borderWidth, Color borderColor, int samples) {
        if (!initialized) {
            initialize();
        }
        
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        
        GL20.glUseProgram(shaderProgram);
        
        GL20.glUniformMatrix4fv(uProjectionLoc, false, projection.get(new float[16]));
        GL20.glUniformMatrix4fv(uModelViewLoc, false, modelView.get(new float[16]));
        
        MinecraftClient mc = MinecraftClient.getInstance();
        GL20.glUniform2f(uResolutionLoc, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        GL20.glUniform2f(uPositionLoc, x, y);
        GL20.glUniform2f(uSizeLoc, width, height);
        GL20.glUniform1f(uRadiusLoc, radius);
        GL20.glUniform1f(uBorderWidthLoc, borderWidth);
        GL20.glUniform1i(uSamplesLoc, samples);
        
        if (fillColor != null) {
            GL20.glUniform4f(uColorLoc, 
                fillColor.getRed() / 255.0f, 
                fillColor.getGreen() / 255.0f, 
                fillColor.getBlue() / 255.0f, 
                fillColor.getAlpha() / 255.0f);
        }
        
        if (borderColor != null) {
            GL20.glUniform4f(uBorderColorLoc, 
                borderColor.getRed() / 255.0f, 
                borderColor.getGreen() / 255.0f, 
                borderColor.getBlue() / 255.0f, 
                borderColor.getAlpha() / 255.0f);
        } else {
            GL20.glUniform4f(uBorderColorLoc, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        GL30.glBindVertexArray(vao);
        GL20.glDrawElements(GL20.GL_TRIANGLES, 6, GL20.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
        
        GL20.glUseProgram(0);
        RenderSystem.disableBlend();
    }
    
    public void cleanup() {
        if (initialized) {
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            GL20.glDeleteProgram(shaderProgram);
            GL20.glDeleteBuffers(vbo);
            GL30.glDeleteVertexArrays(vao);
            initialized = false;
        }
    }
}