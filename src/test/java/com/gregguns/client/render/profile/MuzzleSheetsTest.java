package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * The muzzle-flash frame count is data in each gun's profile; this checks it against the painted cells of the sheet
 * itself, so a regenerated sheet with a different number of frames fails here instead of showing a blank tail.
 */
class MuzzleSheetsTest {

    private static BufferedImage load(String path) throws IOException {
        try (InputStream in = MuzzleSheetsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return ImageIO.read(in);
        }
    }

    /** Number of leading 64x64 cells (4 per row) that hold any visible pixel. */
    private static int paintedCells(BufferedImage img) {
        int count = 0;
        for (int cell = 0; cell < 16; cell++) {
            int cx = cell % 4 * 64;
            int cy = cell / 4 * 64;
            boolean any = false;
            for (int y = cy; y < cy + 64 && !any; y++) {
                for (int x = cx; x < cx + 64 && !any; x++) {
                    int argb = img.getRGB(x, y);
                    int max = Math.max((argb >> 16) & 0xFF, Math.max((argb >> 8) & 0xFF, argb & 0xFF));
                    any = (argb >>> 24) * max / 255 > 16;
                }
            }
            if (!any) {
                return count;
            }
            count++;
        }
        return count;
    }

    @Test
    void pistolFlashPlaysEveryPaintedFrameOfMuzzleflash2AndNoEmptyOne() throws IOException {
        int painted = paintedCells(load("/assets/gregguns/textures/fx/muzzleflash2.png"));
        assertEquals(painted, Pistol9mmProfile.MUZZLE_FLASH_FRAMES);
        assertEquals(
            painted,
            Pistol9mmProfile.TRANSFORMS.muzzleFlash()
                .sheet()
                .frames());
    }

    @Test
    void revolverFlashPlaysEveryPaintedFrameOfMuzzleflash4x4() throws IOException {
        int painted = paintedCells(load("/assets/gregguns/textures/fx/muzzleflash4x4.png"));
        assertEquals(painted, RevolverProfile.MUZZLE_FLASH_FRAMES);
        assertEquals(
            painted,
            RevolverProfile.TRANSFORMS.muzzleFlash()
                .sheet()
                .frames());
    }

    @Test
    void boltActionFlashPlaysEveryPaintedFrameOfMuzzleflash2() throws IOException {
        int painted = paintedCells(load("/assets/gregguns/textures/fx/muzzleflash2.png"));
        assertEquals(painted, BoltActionRifleProfile.MUZZLE_FLASH_FRAMES);
        assertEquals(
            painted,
            BoltActionRifleProfile.TRANSFORMS.muzzleFlash()
                .sheet()
                .frames());
    }
}
