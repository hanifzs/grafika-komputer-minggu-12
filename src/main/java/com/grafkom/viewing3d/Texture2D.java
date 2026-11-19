package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

public final class Texture2D implements AutoCloseable {
    private final int id;

    public Texture2D(String resource) {
        byte[] bytes;
        try (var input = Texture2D.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Tekstur tidak ditemukan: " + resource);
            bytes = input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer encoded = stack.malloc(bytes.length).put(bytes).flip();
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            var channels = stack.mallocInt(1);
            // Koordinat UV OpenGL berawal di pojok kiri bawah; PNG berawal di kiri atas.
            stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixels = stbi_load_from_memory(encoded, width, height, channels, 4);
            if (pixels == null) {
                glDeleteTextures(id);
                throw new IllegalStateException("Gagal membaca PNG/JPG: " + resource + ": " + stbi_failure_reason());
            }
            try {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                glGenerateMipmap(GL_TEXTURE_2D);
            } finally {
                stbi_image_free(pixels);
            }
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        setNearest(false);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void setNearest(boolean nearest) {
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, nearest ? GL_NEAREST : GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, nearest ? GL_NEAREST : GL_LINEAR);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    @Override public void close() { glDeleteTextures(id); }
}
