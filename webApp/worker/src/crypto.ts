import type { PlaybackTokenPayload } from "./types";

const encoder = new TextEncoder();
const decoder = new TextDecoder();

export async function encryptPlaybackToken(
  secret: string,
  payload: PlaybackTokenPayload,
): Promise<string> {
  const key = await importSecretKey(secret);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const plaintext = encoder.encode(JSON.stringify(payload));
  const ciphertext = await crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv: toArrayBuffer(iv),
    },
    key,
    toArrayBuffer(plaintext),
  );

  return `${toBase64Url(iv)}.${toBase64Url(new Uint8Array(ciphertext))}`;
}

export async function decryptPlaybackToken(
  secret: string,
  token: string,
): Promise<PlaybackTokenPayload | null> {
  const [ivPart, cipherPart] = token.split(".");
  if (ivPart == null || cipherPart == null) {
    return null;
  }

  const key = await importSecretKey(secret);
  const iv = fromBase64Url(ivPart);
  const ciphertext = fromBase64Url(cipherPart);

  try {
    const plaintext = await crypto.subtle.decrypt(
      {
        name: "AES-GCM",
        iv: toArrayBuffer(iv),
      },
      key,
      toArrayBuffer(ciphertext),
    );
    return JSON.parse(decoder.decode(plaintext)) as PlaybackTokenPayload;
  } catch {
    return null;
  }
}

async function importSecretKey(secret: string): Promise<CryptoKey> {
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(secret));
  return crypto.subtle.importKey("raw", digest, "AES-GCM", false, ["encrypt", "decrypt"]);
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/u, "");
}

function fromBase64Url(value: string): Uint8Array {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const paddingLength = (4 - (normalized.length % 4)) % 4;
  const padded = normalized + "=".repeat(paddingLength);
  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
}
