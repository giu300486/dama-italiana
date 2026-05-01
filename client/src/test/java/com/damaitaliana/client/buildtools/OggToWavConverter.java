package com.damaitaliana.client.buildtools;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * One-shot asset pipeline tool: decode every {@code *.ogg} under {@code <inputDir>} into a 16-bit
 * PCM {@code *.wav} written under {@code <outputDir>} using the JOrbis low-level decode API.
 *
 * <p>Why this exists: JavaFX Media on Windows does not decode OGG Vorbis (empirically verified
 * 2026-05-01: {@code MediaException — Unrecognized file signature!}). Kenney distributes its CC0
 * SFX packs in OGG only. To keep the curated Kenney selection from {@code CREDITS.md} without
 * rerunning asset acquisition, this tool converts the OGG masters to WAV PCM that JavaFX plays
 * natively. The OGG files are kept in {@code assets/audio/sfx-master/} for traceability; the WAV
 * outputs are committed to {@code assets/audio/sfx/} where {@link
 * com.damaitaliana.client.audio.Sfx} resolves them.
 *
 * <p>The {@code javax.sound.sampled} SPI route via {@code com.googlecode.soundlibs:vorbisspi} was
 * tried first and produced empty output for some short clips (<400 ms) due to a buffering quirk
 * where the decoder signals EOF before any PCM is drained. Switching to the JOrbis low-level
 * stream/packet API (this implementation, modelled on {@code com.jcraft.jorbis.DecodeExample}) is
 * fully deterministic.
 *
 * <p>Not a JUnit test (no {@code @Test} annotation, doesn't end in {@code Test}); Surefire ignores
 * it. Lives under {@code src/test/java} only because its dependencies (jorbis + vorbisspi) are
 * {@code <scope>test</scope>} — see {@code client/pom.xml} for rationale.
 *
 * <p>Run with:
 *
 * <pre>
 *   mvn -pl client exec:java \
 *       -Dexec.classpathScope=test \
 *       -Dexec.mainClass="com.damaitaliana.client.buildtools.OggToWavConverter" \
 *       -Dexec.args="client/src/main/resources/assets/audio/sfx-master \
 *                    client/src/main/resources/assets/audio/sfx"
 * </pre>
 */
public final class OggToWavConverter {

  private static final int OGG_READ_BUFFER = 4096;

  private OggToWavConverter() {}

  public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
    if (args.length != 2) {
      System.err.println("Usage: OggToWavConverter <inputDir> <outputDir>");
      System.exit(2);
    }
    Path inputDir = Path.of(args[0]).toAbsolutePath().normalize();
    Path outputDir = Path.of(args[1]).toAbsolutePath().normalize();
    if (!Files.isDirectory(inputDir)) {
      System.err.println("Input directory does not exist: " + inputDir);
      System.exit(2);
    }
    Files.createDirectories(outputDir);

    List<Path> oggFiles = new ArrayList<>();
    try (Stream<Path> stream = Files.list(inputDir)) {
      stream
          .filter(
              p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"))
          .sorted(Comparator.naturalOrder())
          .forEach(oggFiles::add);
    }
    if (oggFiles.isEmpty()) {
      System.out.println("No *.ogg files found in " + inputDir);
      return;
    }
    System.out.println("Converting " + oggFiles.size() + " file(s):");
    for (Path ogg : oggFiles) {
      Path wav = outputDir.resolve(replaceExtension(ogg.getFileName().toString(), ".wav"));
      DecodedAudio decoded = decodeOgg(ogg);
      writeWav(decoded, wav);
      System.out.printf(
          "  %s (%d bytes, %d Hz, %d ch) -> %s (%d PCM bytes)%n",
          ogg.getFileName(),
          Files.size(ogg),
          decoded.sampleRate,
          decoded.channels,
          wav.getFileName(),
          decoded.pcm.length);
    }
  }

  /** Holds the raw 16-bit signed little-endian PCM bytes plus the format hints. */
  private record DecodedAudio(byte[] pcm, int sampleRate, int channels) {}

  /**
   * Decode an OGG Vorbis file into 16-bit signed little-endian interleaved PCM. Modelled on the
   * upstream {@code com.jcraft.jorbis.DecodeExample} reference implementation.
   */
  private static DecodedAudio decodeOgg(Path oggFile) throws IOException {
    SyncState oy = new SyncState();
    StreamState os = new StreamState();
    Page og = new Page();
    Packet op = new Packet();
    Info vi = new Info();
    Comment vc = new Comment();
    DspState vd = new DspState();
    Block vb = new Block(vd);

    oy.init();

    try (InputStream in =
        new BufferedInputStream(new ByteArrayInputStream(Files.readAllBytes(oggFile)))) {
      // ---------- Phase 1: read first OGG page, init stream state ----------
      int pageBufIdx = oy.buffer(OGG_READ_BUFFER);
      int bytesRead = in.read(oy.data, pageBufIdx, OGG_READ_BUFFER);
      oy.wrote(bytesRead);
      if (oy.pageout(og) != 1) {
        throw new IOException("Not a valid OGG bitstream: " + oggFile);
      }
      os.init(og.serialno());
      vi.init();
      vc.init();
      if (os.pagein(og) < 0) {
        throw new IOException("Error reading first OGG page header: " + oggFile);
      }
      if (os.packetout(op) != 1) {
        throw new IOException("Error reading initial Vorbis packet: " + oggFile);
      }
      if (vi.synthesis_headerin(vc, op) < 0) {
        throw new IOException("File is not a Vorbis bitstream: " + oggFile);
      }

      // ---------- Phase 2: read 2 more header packets (comment + codebook) ----------
      int headersRead = 1;
      while (headersRead < 3) {
        int pageoutResult;
        while (headersRead < 3 && (pageoutResult = oy.pageout(og)) != 0) {
          if (pageoutResult == -1) {
            throw new IOException("Corrupt secondary header in OGG: " + oggFile);
          }
          os.pagein(og);
          while (headersRead < 3) {
            int packetoutResult = os.packetout(op);
            if (packetoutResult == 0) {
              break;
            }
            if (packetoutResult == -1) {
              throw new IOException("Corrupt secondary header packet in OGG: " + oggFile);
            }
            vi.synthesis_headerin(vc, op);
            headersRead++;
          }
        }
        if (headersRead < 3) {
          int idx = oy.buffer(OGG_READ_BUFFER);
          int bytes = in.read(oy.data, idx, OGG_READ_BUFFER);
          if (bytes <= 0 && headersRead < 3) {
            throw new IOException("EOF before headers complete: " + oggFile);
          }
          oy.wrote(Math.max(bytes, 0));
        }
      }

      // ---------- Phase 3: init synthesis ----------
      vd.synthesis_init(vi);
      vb.init(vd);
      int channels = vi.channels;
      int sampleRate = vi.rate;
      float[][][] pcmBuffer = new float[1][][];
      int[] pcmIndex = new int[channels];
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      // 4 KiB intermediate buffer to write 16-bit interleaved PCM.
      byte[] convBuffer = new byte[4096];

      // ---------- Phase 4: drain all audio packets ----------
      boolean eof = false;
      while (!eof) {
        while (!eof) {
          int pageoutResult = oy.pageout(og);
          if (pageoutResult == 0) {
            break; // need more input
          }
          if (pageoutResult == -1) {
            // missing or corrupt data — ignore and keep reading
            continue;
          }
          os.pagein(og);
          while (true) {
            int packetoutResult = os.packetout(op);
            if (packetoutResult == 0) {
              break;
            }
            if (packetoutResult == -1) {
              continue;
            }
            if (vb.synthesis(op) == 0) {
              vd.synthesis_blockin(vb);
            }
            int samples;
            while ((samples = vd.synthesis_pcmout(pcmBuffer, pcmIndex)) > 0) {
              writeInterleavedPcm(sink, pcmBuffer[0], pcmIndex, samples, channels, convBuffer);
              vd.synthesis_read(samples);
            }
          }
          if (og.eos() != 0) {
            eof = true;
          }
        }
        if (!eof) {
          int idx = oy.buffer(OGG_READ_BUFFER);
          int bytes = in.read(oy.data, idx, OGG_READ_BUFFER);
          oy.wrote(Math.max(bytes, 0));
          if (bytes <= 0) {
            eof = true;
          }
        }
      }

      // ---------- Phase 5: cleanup ----------
      os.clear();
      vb.clear();
      vd.clear();
      vi.clear();
      oy.clear();

      return new DecodedAudio(sink.toByteArray(), sampleRate, channels);
    }
  }

  /**
   * Write {@code samples} samples per channel as 16-bit signed LE interleaved PCM into {@code
   * sink}.
   */
  private static void writeInterleavedPcm(
      ByteArrayOutputStream sink,
      float[][] pcm,
      int[] pcmIndex,
      int samples,
      int channels,
      byte[] convBuffer) {
    int bytesPerSample = 2;
    int frameSize = channels * bytesPerSample;
    int maxFramesPerWrite = convBuffer.length / frameSize;
    int frame = 0;
    while (frame < samples) {
      int batch = Math.min(maxFramesPerWrite, samples - frame);
      for (int f = 0; f < batch; f++) {
        for (int ch = 0; ch < channels; ch++) {
          float value = pcm[ch][pcmIndex[ch] + frame + f];
          int sample = Math.round(value * 32767.0f);
          if (sample > 32767) {
            sample = 32767;
          } else if (sample < -32768) {
            sample = -32768;
          }
          int bufOffset = (f * channels + ch) * bytesPerSample;
          convBuffer[bufOffset] = (byte) (sample & 0xFF);
          convBuffer[bufOffset + 1] = (byte) ((sample >> 8) & 0xFF);
        }
      }
      sink.write(convBuffer, 0, batch * frameSize);
      frame += batch;
    }
  }

  private static void writeWav(DecodedAudio decoded, Path wavFile)
      throws IOException, UnsupportedAudioFileException {
    AudioFormat format =
        new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            decoded.sampleRate,
            16,
            decoded.channels,
            decoded.channels * 2,
            decoded.sampleRate,
            false);
    long frames = decoded.pcm.length / format.getFrameSize();
    try (AudioInputStream stream =
        new AudioInputStream(new ByteArrayInputStream(decoded.pcm), format, frames)) {
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wavFile.toFile());
    }
  }

  private static String replaceExtension(String filename, String newExt) {
    int dot = filename.lastIndexOf('.');
    return (dot < 0 ? filename : filename.substring(0, dot)) + newExt;
  }
}
