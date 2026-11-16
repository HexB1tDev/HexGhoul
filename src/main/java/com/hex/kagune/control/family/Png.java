package com.hex.kagune.control.family;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * 1.3.10: a small PNG reader (pure Java, nothing of the game's or the desktop's): 8-bit RGBA or RGB, not interlaced, every filter. The
 * child skin parts are kept that way ({@code textures/entity/child}), so they read the same on every device, in game and off it.
 */
public final class Png {
    /** A decoded image: its size and its pixels (ARGB, row by row). */
    public record Image(int width, int height, int[] pixels) {
    }

    private Png() {
    }

    public static Image read(InputStream in) throws IOException {
        byte[] data = in.readAllBytes();
        if (data.length < 8 || (data[0] & 255) != 0x89 || data[1] != 'P' || data[2] != 'N' || data[3] != 'G') {
            throw new IOException("not a PNG");
        }
        int width = 0;
        int height = 0;
        int depth = 0;
        int colour = 0;
        int interlace = 0;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        int at = 8;
        while (at + 8 <= data.length) {
            int length = int32(data, at);
            String type = new String(data, at + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int body = at + 8;
            if (length < 0 || body + length > data.length) {
                throw new IOException("truncated PNG");
            }
            switch (type) {
                case "IHDR" -> {
                    width = int32(data, body);
                    height = int32(data, body + 4);
                    depth = data[body + 8] & 255;
                    colour = data[body + 9] & 255;
                    interlace = data[body + 12] & 255;
                }
                case "IDAT" -> idat.write(data, body, length);
                default -> {
                }
            }
            if (type.equals("IEND")) {
                break;
            }
            at = body + length + 4;
        }
        if (depth != 8 || colour != 6 && colour != 2 || interlace != 0 || width <= 0 || height <= 0 || width > 4096 || height > 4096) {
            throw new IOException("unsupported PNG (depth " + depth + ", colour " + colour + ", interlace " + interlace + ")");
        }
        int bpp = colour == 6 ? 4 : 3;
        int stride = width * bpp;
        byte[] raw = inflate(idat.toByteArray(), (stride + 1) * height);
        int[] pixels = new int[width * height];
        byte[] previous = new byte[stride];
        byte[] row = new byte[stride];
        for (int y = 0; y < height; y++) {
            int filter = raw[y * (stride + 1)] & 255;
            System.arraycopy(raw, y * (stride + 1) + 1, row, 0, stride);
            unfilter(filter, row, previous, bpp);
            for (int x = 0; x < width; x++) {
                int r = row[x * bpp] & 255;
                int g = row[x * bpp + 1] & 255;
                int b = row[x * bpp + 2] & 255;
                int a = bpp == 4 ? row[x * bpp + 3] & 255 : 255;
                pixels[y * width + x] = a << 24 | r << 16 | g << 8 | b;
            }
            byte[] swap = previous;
            previous = row;
            row = swap;
        }
        return new Image(width, height, pixels);
    }

    private static void unfilter(int filter, byte[] row, byte[] up, int bpp) throws IOException {
        for (int i = 0; i < row.length; i++) {
            int a = i >= bpp ? row[i - bpp] & 255 : 0;
            int b = up[i] & 255;
            int c = i >= bpp ? up[i - bpp] & 255 : 0;
            int x = row[i] & 255;
            int v = switch (filter) {
                case 0 -> x;
                case 1 -> x + a;
                case 2 -> x + b;
                case 3 -> x + ((a + b) >> 1);
                case 4 -> x + paeth(a, b, c);
                default -> throw new IOException("bad PNG filter " + filter);
            };
            row[i] = (byte)v;
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
    }

    private static byte[] inflate(byte[] compressed, int expected) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        byte[] out = new byte[expected];
        try {
            int n = 0;
            while (n < expected && !inflater.finished()) {
                int got = inflater.inflate(out, n, expected - n);
                if (got == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                n += got;
            }
            if (n != expected) {
                throw new IOException("PNG data short: " + n + " of " + expected);
            }
        } catch (DataFormatException bad) {
            throw new IOException("bad PNG data", bad);
        } finally {
            inflater.end();
        }
        return out;
    }

    private static int int32(byte[] d, int at) {
        return (d[at] & 255) << 24 | (d[at + 1] & 255) << 16 | (d[at + 2] & 255) << 8 | d[at + 3] & 255;
    }
}
