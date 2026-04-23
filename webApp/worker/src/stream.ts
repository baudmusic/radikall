const LIVE_FALLBACK_URL = "https://si-f-radiko.smartstream.ne.jp/so/playlist.m3u8";

export async function buildLiveStreamUrl(stationId: string): Promise<string> {
  const baseUrl = await getLivePlaylistUrl(stationId);
  const url = new URL(baseUrl);
  url.searchParams.set("station_id", stationId);
  url.searchParams.set("lsid", randomHex(32));
  url.searchParams.set("type", "b");
  url.searchParams.set("l", "15");
  return url.toString();
}

async function getLivePlaylistUrl(stationId: string): Promise<string> {
  const response = await fetch(`https://radiko.jp/v3/station/stream/pc_html5/${encodeURIComponent(stationId)}.xml`);
  if (!response.ok) {
    return LIVE_FALLBACK_URL;
  }

  const xml = await response.text();
  const match = xml.match(/<url[^>]*areafree="0"[^>]*timefree="0"[^>]*>[\s\S]*?<playlist_create_url>(.*?)<\/playlist_create_url>/u);
  return match?.[1]?.trim() || LIVE_FALLBACK_URL;
}

function randomHex(length: number): string {
  const bytes = crypto.getRandomValues(new Uint8Array(Math.ceil(length / 2)));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("").slice(0, length);
}
