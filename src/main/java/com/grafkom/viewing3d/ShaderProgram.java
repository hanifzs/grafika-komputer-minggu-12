package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public final class ShaderProgram implements AutoCloseable {
    private final int program;

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vertex = compile(GL_VERTEX_SHADER, vertexResource);
        int fragment;
        try {
            fragment = compile(GL_FRAGMENT_SHADER, fragmentResource);
        } catch (RuntimeException e) {
            glDeleteShader(vertex);
            throw e;
        }
        program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new IllegalStateException("Shader link gagal: " + log);
        }
    }

    private static int compile(int type, String path) {
        String source;
        try (var input = ShaderProgram.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Resource tidak ditemukan: " + path);
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader " + path + " gagal: " + log);
        }
        return shader;
    }

    public void use() { glUseProgram(program); }
    public void set(String name, int value) { glUniform1i(glGetUniformLocation(program, name), value); }
    public void set(String name, float value) { glUniform1f(glGetUniformLocation(program, name), value); }
    public void set(String name, Vector3f value) { glUniform3f(glGetUniformLocation(program, name), value.x, value.y, value.z); }
    public void set(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            glUniformMatrix4fv(glGetUniformLocation(program, name), false, value.get(buffer));
        }
    }
    @Override public void close() { glDeleteProgram(program); }
}
