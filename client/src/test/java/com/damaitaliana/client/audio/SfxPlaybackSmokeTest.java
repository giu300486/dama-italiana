package com.damaitaliana.client.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that every bundled SFX is a well-formed PCM WAV file readable by {@code
 * javax.sound.sampled}. Built after the Task 3.5.4 follow-up that replaced the original OGG masters
 * (unsupported by JavaFX Media on Windows — empirically verified, {@code MediaException —
 * Unrecognized file signature!}) with WAV files produced by {@link
 * com.damaitaliana.client.buildtools.OggToWavConverter}. Acts as a regression guard if a future
 * asset acquisition reintroduces a codec/encoding that JavaFX cannot decode.
 *
 * <p>Uses {@code javax.sound.sampled} rather than booting JavaFX because (a) PCM WAV is the lowest
 * common denominator both stacks read identically, (b) the test runs headless without a JavaFX
 * toolkit, and (c) JavaFX {@code MediaPlayer.setOnReady} callbacks are flaky when many short media
 * items are created back-to-back in unit tests.
 */
class SfxPlaybackSmokeTest {

  @Test
  void everyBundledSfxIsAWellFormedPcmWav() throws IOException, UnsupportedAudioFileException {
    for (Sfx sfx : Sfx.values()) {
      URL resource = SfxPlaybackSmokeTest.class.getResource(sfx.resourcePath());
      assertThat(resource).as("classpath resource for %s", sfx).isNotNull();
      try (AudioInputStream stream = AudioSystem.getAudioInputStream(resource)) {
        AudioFormat format = stream.getFormat();
        assertThat(format.getEncoding())
            .as("encoding of %s", sfx)
            .isEqualTo(AudioFormat.Encoding.PCM_SIGNED);
        assertThat(format.getSampleRate()).as("sample rate of %s", sfx).isPositive();
        assertThat(format.getChannels()).as("channel count of %s", sfx).isBetween(1, 2);
        assertThat(stream.getFrameLength()).as("frame count of %s", sfx).isPositive();
      }
    }
  }
}
