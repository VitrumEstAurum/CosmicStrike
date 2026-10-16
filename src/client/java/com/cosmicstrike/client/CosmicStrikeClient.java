
package com.cosmicstrike.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public class CosmicStrikeClient implements ClientModInitializer {
    private static final int POINT_COUNT = 8;

    // Расстояние до будущей цели по горизонтали.
    private static final double TARGET_DISTANCE = 100.0;

    // Высота окружности над начальной позицией игрока.
    private static final double SKY_HEIGHT = 100.0;

    // Размер и радиус окружности.
    private static final float RADIUS = 30.0f;
    private static final float HALF_SIZE = 1.2f;

    // Координаты цели фиксируются при первом появлении в мире.
    private static boolean initialized = false;
    private static double targetX;
    private static double targetY;
    private static double targetZ;

    // Направление окружности также фиксируется.
    private static float leftX, leftY, leftZ;
    private static float upX, upY, upZ;

    @Override
    public void onInitializeClient() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level == null || minecraft.player == null) {
                return;
            }

            Camera camera = minecraft.gameRenderer.mainCamera();
            Vec3 cameraPos = camera.position();

            if (!initialized) {
                Vector3fc forward = camera.forwardVector();

                double fx = forward.x();
                double fz = forward.z();
                double length = Math.sqrt(fx * fx + fz * fz);

                // Если игрок смотрит почти вертикально, используем
                // направление на север как запасной вариант.
                if (length < 0.001) {
                    fx = 0.0;
                    fz = 1.0;
                    length = 1.0;
                }

                fx /= length;
                fz /= length;

                targetX = cameraPos.x + fx * TARGET_DISTANCE;
                targetZ = cameraPos.z + fz * TARGET_DISTANCE;
                targetY = minecraft.player.getY() + SKY_HEIGHT;

                Vector3fc left = camera.leftVector();
                Vector3fc up = camera.upVector();

                leftX = left.x();
                leftY = left.y();
                leftZ = left.z();

                upX = up.x();
                upY = up.y();
                upZ = up.z();

                initialized = true;
            }

            // Переводим зафиксированные мировые координаты
            // в координаты относительно камеры.
            float centerX = (float) (targetX - cameraPos.x);
            float centerY = (float) (targetY - cameraPos.y);
            float centerZ = (float) (targetZ - cameraPos.z);

            PoseStack poseStack = context.poseStack();

            context.submitNodeCollector().submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, vertices) -> {
                    for (int i = 0; i < POINT_COUNT; i++) {
                        double angle =
                            2.0 * Math.PI * i / POINT_COUNT;

                        float horizontal =
                            (float) (RADIUS * Math.cos(angle));
                        float vertical =
                            (float) (RADIUS * Math.sin(angle));

                        float x = centerX
                            + horizontal * leftX + vertical * upX;
                        float y = centerY
                            + horizontal * leftY + vertical * upY;
                        float z = centerZ
                            + horizontal * leftZ + vertical * upZ;

                        drawPoint(vertices, pose, x, y, z);
                    }
                }
            );
        });
    }

    private static void drawPoint(
        VertexConsumer vertices,
        PoseStack.Pose pose,
        float x,
        float y,
        float z
    ) {
        vertices.addVertex(
            pose, x - HALF_SIZE, y - HALF_SIZE, z
        ).setColor(255, 12, 18, 255);

        vertices.addVertex(
            pose, x + HALF_SIZE, y - HALF_SIZE, z
        ).setColor(255, 12, 18, 255);

        vertices.addVertex(
            pose, x + HALF_SIZE, y + HALF_SIZE, z
        ).setColor(255, 12, 18, 255);

        vertices.addVertex(
            pose, x - HALF_SIZE, y + HALF_SIZE, z
        ).setColor(255, 12, 18, 255);
    }
}
