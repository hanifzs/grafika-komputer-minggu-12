package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Locale;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public final class VisualRealismApp {
    // Ubah koordinat di sini untuk eksperimen posisi sumber cahaya (Minggu 12).
    // Tombol 1/2/3 memilih salah satu posisi tanpa perlu menjalankan ulang aplikasi.
    private static final Vector3f[] LIGHT_POSITIONS = {
        new Vector3f(3f, 5f, 3f),
        new Vector3f(-4f, 5f, 2f),
        new Vector3f(2f, 4f, -4f)
    };
    private long window;
    private int width = 1100, height = 760;
    private boolean blinn = true, flat = false, depth = true, culling = true;
    private boolean nearest = false, texture = true, shadows = true;
    private int lightPreset = 0;
    private float angle = 0.7f, elevation = 0.42f, distance = 5.5f;
    private boolean smokeTest;

    public static void main(String[] args) {
        var app = new VisualRealismApp();
        app.smokeTest = Arrays.asList(args).contains("--smoke");
        app.run();
    }

    private void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW gagal diinisialisasi");
        try {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            if (smokeTest) glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            window = glfwCreateWindow(width, height, "3D Visual Realism Suite", 0, 0);
            if (window == 0) throw new IllegalStateException("Tidak dapat membuat jendela OpenGL 3.3");
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            GL.createCapabilities();
            glfwSetFramebufferSizeCallback(window, (handle, w, h) -> {
                width = Math.max(1, w);
                height = Math.max(1, h);
            });
            glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
                if (action == GLFW_PRESS) toggle(key);
            });
            printControls();
            try (Mesh smoothSphere = Mesh.sphere(false, 32, 20);
                 Mesh flatSphere = Mesh.sphere(true, 32, 20);
                 Mesh floor = Mesh.plane();
                 Texture2D sphereTexture = new Texture2D("/textures/brick.png");
                 Texture2D floorTexture = new Texture2D("/textures/floor.png");
                 ShadowMapFBO shadowMap = new ShadowMapFBO();
                 ShaderProgram depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
                 ShaderProgram lightShader = new ShaderProgram("/shaders/scene.vert", "/shaders/scene.frag")) {
                lightShader.use();
                lightShader.set("diffuseMap", 0);
                lightShader.set("shadowMap", 1);
                renderLoop(smoothSphere, flatSphere, floor, sphereTexture, floorTexture, shadowMap, depthShader, lightShader);
            }
        } finally {
            if (window != 0) glfwDestroyWindow(window);
            glfwTerminate();
            var callback = glfwSetErrorCallback(null);
            if (callback != null) callback.free();
        }
    }

    private void renderLoop(Mesh smoothSphere, Mesh flatSphere, Mesh floor, Texture2D sphereTexture,
                            Texture2D floorTexture, ShadowMapFBO shadowMap, ShaderProgram depthShader,
                            ShaderProgram lightShader) {
        int frames = 0;
        double lastTitleUpdate = glfwGetTime();
        Matrix4f model = new Matrix4f();
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            if (filterChanged) {
                sphereTexture.setNearest(nearest);
                floorTexture.setNearest(nearest);
                filterChanged = false;
            }
            if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) angle -= 0.025f;
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) angle += 0.025f;
            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) elevation = Math.min(1.35f, elevation + 0.025f);
            if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) elevation = Math.max(0.08f, elevation - 0.025f);
            if (glfwGetKey(window, GLFW_KEY_EQUAL) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_ADD) == GLFW_PRESS)
                distance = Math.max(2.2f, distance - 0.06f);
            if (glfwGetKey(window, GLFW_KEY_MINUS) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS)
                distance = Math.min(18f, distance + 0.06f);

            Vector3f lightPos = LIGHT_POSITIONS[lightPreset];
            Matrix4f lightSpace = new Matrix4f().ortho(-8, 8, -8, 8, 1, 22)
                .lookAt(lightPos, new Vector3f(0, -0.2f, 0), new Vector3f(0, 1, 0));
            Mesh sphere = flat ? flatSphere : smoothSphere;

            // Pass 1: depth map dari sudut pandang sumber cahaya.
            HSRManager.setupDepthTest(true);
            HSRManager.setupBackFaceCulling(false); // dua sisi agar caster tidak terpotong
            shadowMap.begin();
            depthShader.use();
            depthShader.set("lightSpaceMatrix", lightSpace);
            depthShader.set("model", model);
            floor.draw();
            sphere.draw();
            shadowMap.end(width, height);

            // Pass 2: scene kamera. Toggle HSR hanya memengaruhi pass ini.
            HSRManager.setupDepthTest(depth);
            HSRManager.setupBackFaceCulling(culling);
            glClearColor(0.075f, 0.11f, 0.17f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            Vector3f camera = new Vector3f((float) (Math.sin(angle) * Math.cos(elevation) * distance),
                (float) (Math.sin(elevation) * distance + 0.2f),
                (float) (Math.cos(angle) * Math.cos(elevation) * distance));
            Matrix4f view = new Matrix4f().lookAt(camera, new Vector3f(0, -0.15f, 0), new Vector3f(0, 1, 0));
            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(50), (float) width / height, 0.1f, 60f);
            lightShader.use();
            lightShader.set("model", model);
            lightShader.set("view", view);
            lightShader.set("projection", projection);
            lightShader.set("lightSpaceMatrix", lightSpace);
            lightShader.set("lightPos", lightPos);
            lightShader.set("viewPos", camera);
            lightShader.set("lightColor", new Vector3f(1f, 0.95f, 0.83f));
            lightShader.set("useBlinnPhong", blinn ? 1 : 0);
            lightShader.set("useTexture", texture ? 1 : 0);
            lightShader.set("useShadows", shadows ? 1 : 0);
            shadowMap.bindTexture(1);
            floorTexture.bind(0);
            lightShader.set("baseColor", new Vector3f(0.65f, 0.72f, 0.79f));
            floor.draw();
            sphereTexture.bind(0);
            lightShader.set("baseColor", new Vector3f(0.94f, 0.46f, 0.25f));
            sphere.draw();
            glfwSwapBuffers(window);
            frames++;
            if (smokeTest) {
                int glError = glGetError();
                if (glError != GL_NO_ERROR) throw new IllegalStateException("OpenGL error: 0x" + Integer.toHexString(glError));
                if (frames == 1) {
                    for (int key : new int[]{GLFW_KEY_B, GLFW_KEY_F, GLFW_KEY_Z, GLFW_KEY_C,
                                             GLFW_KEY_T, GLFW_KEY_M, GLFW_KEY_L, GLFW_KEY_2}) toggle(key);
                }
            }
            if (smokeTest && frames >= 2) {
                System.out.println("SMOKE TEST OK: GLFW, shader, tekstur, FBO, dua render pass, dan seluruh mode toggle.");
                glfwSetWindowShouldClose(window, true);
            }
            double now = glfwGetTime();
            if (now - lastTitleUpdate >= 1) {
                glfwSetWindowTitle(window, String.format(Locale.ROOT,
                    "ThreeDViewing | %d FPS | %s | %s | Z:%s C:%s | T:%s M:%s | Shadow:%s | Light:%d",
                    Math.round(frames / (now - lastTitleUpdate)), blinn ? "Blinn-Phong" : "Phong",
                    flat ? "Flat" : "Smooth", depth ? "ON" : "OFF", culling ? "ON" : "OFF",
                    nearest ? "Nearest" : "Linear+Mipmap", texture ? "ON" : "OFF",
                    shadows ? "ON" : "OFF", lightPreset + 1));
                lastTitleUpdate = now;
                frames = 0;
            }
        }
    }

    private void toggle(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
            case GLFW_KEY_B -> blinn = !blinn;
            case GLFW_KEY_F -> flat = !flat;
            case GLFW_KEY_Z -> depth = !depth;
            case GLFW_KEY_C -> culling = !culling;
            case GLFW_KEY_T -> nearest = !nearest;
            case GLFW_KEY_M -> texture = !texture;
            case GLFW_KEY_L -> shadows = !shadows;
            case GLFW_KEY_1 -> lightPreset = 0;
            case GLFW_KEY_2 -> lightPreset = 1;
            case GLFW_KEY_3 -> lightPreset = 2;
            default -> { return; }
        }
        System.out.printf("B:%s F:%s Z:%s C:%s T:%s M:%s L:%s Light:%d%n",
            blinn ? "Blinn" : "Phong", flat ? "Flat" : "Smooth", depth, culling,
            nearest ? "Nearest" : "Linear+Mip", texture, shadows, lightPreset + 1);
        if (key == GLFW_KEY_T) filterChanged = true;
    }

    private boolean filterChanged = true;

    private void printControls() {
        System.out.println("B Phong/Blinn | F Flat/Smooth | Z Depth | C Culling | T Nearest/Linear+Mip | M Texture | L Shadow");
        System.out.println("1/2/3: posisi cahaya | Panah: orbit kamera | +/-: zoom | ESC: keluar. FPS dan mode pada judul jendela.");
    }
}
