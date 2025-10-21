package com.volt.gui.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShaderManager {
    private static ShaderManager instance;
    private final Map<String, Integer> shaderPrograms = new HashMap<>();
    private final Map<String, Integer> shaders = new HashMap<>();
    
    public static ShaderManager getInstance() {
        if (instance == null) {
            instance = new ShaderManager();
        }
        return instance;
    }

    public String loadShaderSource(String path) throws IOException {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        String resourcePath = path.startsWith("/") ? path.substring(1) : path;
        Identifier identifier = Identifier.of("volt-id", resourcePath);
        
        try (InputStream inputStream = resourceManager.getResource(identifier).get().getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public int compileShader(int type, String source) {
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

    public int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String error = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            GL20.glDeleteProgram(program);
            throw new RuntimeException("Shader program linking failed: " + error);
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        return program;
    }

    public int loadShaderProgram(String name, String vertexPath, String fragmentPath) throws IOException {
        if (shaderPrograms.containsKey(name)) {
            return shaderPrograms.get(name);
        }
        
        String vertexSource = loadShaderSource(vertexPath);
        String fragmentSource = loadShaderSource(fragmentPath);
        
        int program = createShaderProgram(vertexSource, fragmentSource);
        shaderPrograms.put(name, program);
        
        return program;
    }

    public int getShaderProgram(String name) {
        return shaderPrograms.getOrDefault(name, 0);
    }
 
    public boolean validateShaderProgram(int program) {
        GL20.glValidateProgram(program);
        return GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) != 0;
    }

    public int getUniformLocation(int program, String uniformName) {
        return GL20.glGetUniformLocation(program, uniformName);
    }

    public void cleanup() {
        for (int program : shaderPrograms.values()) {
            GL20.glDeleteProgram(program);
        }
        shaderPrograms.clear();
        
        for (int shader : shaders.values()) {
            GL20.glDeleteShader(shader);
        }
        shaders.clear();
    }

    public void cleanupShaderProgram(String name) {
        Integer program = shaderPrograms.remove(name);
        if (program != null) {
            GL20.glDeleteProgram(program);
        }
    }
}