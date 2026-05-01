package com.damaitaliana.client.audio;

/**
 * The two independent audio buses exposed in Settings: ambient music and SFX. Volume and mute state
 * are tracked separately per SPEC §13.4.
 */
public enum AudioBus {
  MUSIC,
  SFX
}
