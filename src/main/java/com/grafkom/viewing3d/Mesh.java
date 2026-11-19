package com.grafkom.viewing3d;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** VAO/VBO interleaved: position(3), normal(3), UV(2). */
public final class Mesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private final int vertexCount;

    private Mesh(float[] vertices) {
        vertexCount = vertices.length / 8;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        var buffer = MemoryUtil.memAllocFloat(vertices.length);
        try {
            buffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public static Mesh plane() {
        // CCW jika dipandang dari atas; UV berulang enam kali.
        return new Mesh(new float[]{
            -6,-0.85f,-6, 0,1,0, 0,0,
            -6,-0.85f, 6, 0,1,0, 0,6,
             6,-0.85f, 6, 0,1,0, 6,6,
            -6,-0.85f,-6, 0,1,0, 0,0,
             6,-0.85f, 6, 0,1,0, 6,6,
             6,-0.85f,-6, 0,1,0, 6,0
        });
    }

    public static Mesh sphere(boolean flat, int slices, int stacks) {
        int columns = slices + 1;
        Vector3f[] positions = new Vector3f[(stacks + 1) * columns];
        float[] uv = new float[positions.length * 2];
        Vector3f[] accumulated = new Vector3f[positions.length];
        for (int r = 0; r <= stacks; r++) {
            float latitude = (float) Math.PI * r / stacks;
            for (int c = 0; c <= slices; c++) {
                float longitude = (float) (2 * Math.PI * c / slices);
                int index = r * columns + c;
                positions[index] = new Vector3f(
                    (float) (Math.sin(latitude) * Math.cos(longitude)),
                    (float) Math.cos(latitude),
                    (float) (Math.sin(latitude) * Math.sin(longitude)));
                uv[2 * index] = (float) c / slices;
                uv[2 * index + 1] = 1f - (float) r / stacks;
                accumulated[index] = new Vector3f();
            }
        }
        List<int[]> triangles = new ArrayList<>();
        List<Vector3f> faceNormals = new ArrayList<>();
        for (int r = 0; r < stacks; r++) {
            for (int c = 0; c < slices; c++) {
                int a = r * columns + c, b = (r + 1) * columns + c;
                int d = r * columns + c + 1, e = (r + 1) * columns + c + 1;
                if (r != stacks - 1) addTriangle(a, e, b, positions, accumulated, triangles, faceNormals);
                if (r != 0) addTriangle(a, d, e, positions, accumulated, triangles, faceNormals);
            }
        }
        float[] data = new float[triangles.size() * 3 * 8];
        int offset = 0;
        for (int t = 0; t < triangles.size(); t++) {
            for (int vertex : triangles.get(t)) {
                Vector3f pos = positions[vertex];
                Vector3f normal = flat ? faceNormals.get(t) : new Vector3f(accumulated[vertex]).normalize();
                data[offset++] = pos.x * 0.95f;
                data[offset++] = pos.y * 0.95f + 0.15f;
                data[offset++] = pos.z * 0.95f;
                data[offset++] = normal.x;
                data[offset++] = normal.y;
                data[offset++] = normal.z;
                data[offset++] = uv[2 * vertex];
                data[offset++] = uv[2 * vertex + 1];
            }
        }
        return new Mesh(data);
    }

    private static void addTriangle(int a, int b, int c, Vector3f[] positions,
                                    Vector3f[] normals, List<int[]> triangles, List<Vector3f> faces) {
        Vector3f ab = new Vector3f(positions[b]).sub(positions[a]);
        Vector3f ac = new Vector3f(positions[c]).sub(positions[a]);
        Vector3f cross = ab.cross(ac);
        if (cross.lengthSquared() < 1e-12f) return;
        // Akumulasi normal berbobot luas agar smooth di sudut yang saling berbagi vertex.
        normals[a].add(cross);
        normals[b].add(cross);
        normals[c].add(cross);
        triangles.add(new int[]{a, b, c});
        faces.add(cross.normalize());
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
