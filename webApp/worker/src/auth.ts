import { fullKeyBase64, gpsCoordinates } from "./data";
import type { AuthSession } from "./types";

const APP_NAME = "aSmartPhone8";
const APP_VERSION = "8.2.4";

const DEVICE_PROFILES = [
  {
    androidVersion: "14.0.0",
    sdk: "34",
    model: "G9S9B16",
    build: "AP4A.250105.002.B1",
  },
  {
    androidVersion: "14.0.0",
    sdk: "34",
    model: "GVU6C",
    build: "AD1A.240905.004",
  },
  {
    androidVersion: "13.0.0",
    sdk: "33",
    model: "G1AZG",
    build: "TQ3A.230805.001.A2",
  },
];

const decoder = new TextDecoder();

export async function authenticate(areaId: string): Promise<AuthSession> {
  const device = pickDeviceProfile();
  const userAgent = `Dalvik/2.1.0 (Linux; U; Android ${device.androidVersion}; ${device.model}/${device.build})`;
  const userId = randomHex(32);
  const deviceHeader = `${device.sdk}.${device.model}`;

  const auth1Response = await fetch("https://radiko.jp/v2/api/auth1", {
    headers: {
      "X-Radiko-App": APP_NAME,
      "X-Radiko-App-Version": APP_VERSION,
      "X-Radiko-Device": deviceHeader,
      "X-Radiko-User": userId,
      "User-Agent": userAgent,
    },
  });

  if (!auth1Response.ok) {
    throw new Error(`Auth1 failed with HTTP ${auth1Response.status}`);
  }

  const token = auth1Response.headers.get("x-radiko-authtoken");
  const keyOffset = Number.parseInt(auth1Response.headers.get("x-radiko-keyoffset") ?? "", 10);
  const keyLength = Number.parseInt(auth1Response.headers.get("x-radiko-keylength") ?? "", 10);
  if (token == null || Number.isNaN(keyOffset) || Number.isNaN(keyLength)) {
    throw new Error("Auth1 succeeded but required headers were missing.");
  }

  const auth2Response = await fetch("https://radiko.jp/v2/api/auth2", {
    headers: {
      "X-Radiko-App": APP_NAME,
      "X-Radiko-App-Version": APP_VERSION,
      "X-Radiko-Device": deviceHeader,
      "X-Radiko-User": userId,
      "X-Radiko-AuthToken": token,
      "X-Radiko-Partialkey": generatePartialKey(keyOffset, keyLength),
      "X-Radiko-Location": generateGps(areaId),
      "X-Radiko-Connection": "wifi",
      "User-Agent": userAgent,
    },
  });

  const auth2Text = await auth2Response.text();
  if (!auth2Response.ok) {
    throw new Error(`Auth2 failed with HTTP ${auth2Response.status}: ${auth2Text}`);
  }

  const resolvedAreaId = auth2Text.match(/JP\d{1,2}/u)?.[0] ?? areaId;
  return {
    token,
    areaId: resolvedAreaId,
    userAgent,
  };
}

function generatePartialKey(offset: number, length: number): string {
  const decodedKey = decodeBase64(fullKeyBase64);
  const partial = decodedKey.slice(offset, offset + length);
  return encodeBase64(partial);
}

function generateGps(areaId: string): string {
  const coordinate = gpsCoordinates[areaId];
  if (coordinate == null) {
    throw new Error(`Unsupported areaId for GPS spoofing: ${areaId}`);
  }

  const latitude = coordinate.latitude + randomSigned() / 40;
  const longitude = coordinate.longitude + randomSigned() / 40;
  return `${latitude.toFixed(6)},${longitude.toFixed(6)},gps`;
}

function pickDeviceProfile() {
  return DEVICE_PROFILES[Math.floor(Math.random() * DEVICE_PROFILES.length)] ?? DEVICE_PROFILES[0];
}

function randomSigned(): number {
  const random = new Uint32Array(1);
  crypto.getRandomValues(random);
  const unit = random[0] / 0xffffffff;
  const sign = unit >= 0.5 ? 1 : -1;
  return sign * unit;
}

function randomHex(length: number): string {
  const bytes = crypto.getRandomValues(new Uint8Array(Math.ceil(length / 2)));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("").slice(0, length);
}

function decodeBase64(value: string): Uint8Array {
  const binary = atob(value);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function encodeBase64(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
}

export function randomPlaybackSessionId(): string {
  return randomHex(32);
}

export function currentEpochMillis(): number {
  return Date.now();
}
