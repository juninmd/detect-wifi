const HEX_MAC = /^[0-9A-F]{12}$/;

/** Returns the MAC as `AA:BB:CC:DD:EE:FF`, or null when it is not a valid 48-bit address. */
export function normalizeMac(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const hex = raw.replace(/[:\-.]/g, '').toUpperCase();
  if (!HEX_MAC.test(hex)) return null;
  return hex.match(/.{2}/g).join(':');
}

/**
 * Modern iOS/Android randomize the MAC in probe requests by setting the
 * "locally administered" bit (0x02 of the first octet).
 */
export function isRandomizedMac(normalizedMac: string): boolean {
  return (parseInt(normalizedMac.charAt(1), 16) & 0x2) !== 0;
}
