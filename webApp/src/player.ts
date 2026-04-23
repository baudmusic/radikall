import Hls from "hls.js";

export class UnsupportedPlaybackError extends Error {
  constructor() {
    super("UNSUPPORTED_HLS_PLAYBACK");
  }
}

export function supportsNativeHls(audio: HTMLAudioElement): boolean {
  return audio.canPlayType("application/vnd.apple.mpegurl") !== "";
}

export function buildPlaybackCandidates(
  audio: HTMLAudioElement,
  primaryUrl: string,
): string[] {
  const trimmedPrimary = primaryUrl.trim();
  if (trimmedPrimary.length === 0) {
    return [];
  }

  if (isLikelyHlsStream(trimmedPrimary) && !supportsNativeHls(audio) && !Hls.isSupported()) {
    throw new UnsupportedPlaybackError();
  }

  return [trimmedPrimary];
}

export async function attachLiveStream(
  audio: HTMLAudioElement,
  streamUrl: string,
  currentHls: Hls | null,
  onFatalError: () => void,
): Promise<Hls | null> {
  currentHls?.destroy();

  if (!isLikelyHlsStream(streamUrl)) {
    audio.src = streamUrl;
    audio.load();
    return null;
  }

  if (Hls.isSupported()) {
    return new Promise<Hls>((resolve, reject) => {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
      });
      let settled = false;
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          if (!settled) {
            settled = true;
            hls.destroy();
            reject(new Error(describeHlsError(data)));
            return;
          }
          onFatalError();
        }
      });
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        settled = true;
        resolve(hls);
      });
      hls.loadSource(streamUrl);
      hls.attachMedia(audio);
    });
  }

  if (supportsNativeHls(audio)) {
    audio.src = streamUrl;
    audio.load();
    return null;
  }

  audio.src = streamUrl;
  audio.load();
  return null;
}

export function playAudioElement(audio: HTMLAudioElement): Promise<void> {
  return audio.play();
}

export function cleanupAudio(audio: HTMLAudioElement, currentHls: Hls | null): null {
  currentHls?.destroy();
  audio.pause();
  audio.removeAttribute("src");
  audio.load();
  return null;
}

function describeHlsError(data: {
  type?: string;
  details?: string;
  error?: { message?: string };
  response?: { code?: number; text?: string };
}): string {
  const parts = [
    data.type,
    data.details,
    data.error?.message,
    data.response?.code?.toString(),
    data.response?.text,
  ].filter((value): value is string => value != null && value.trim().length > 0);

  return parts.length > 0 ? parts.join(" | ") : "HLS playback failed";
}

function isLikelyHlsStream(streamUrl: string): boolean {
  const normalized = streamUrl.toLowerCase();
  return normalized.includes(".m3u8") || normalized.includes("mpegurl");
}
