import { isRandomizedMac, normalizeMac } from '../mac.util';

describe('mac.util', () => {
  it('normalizes separators and case', () => {
    expect(normalizeMac('aa-bb-cc-dd-ee-ff')).toBe('AA:BB:CC:DD:EE:FF');
    expect(normalizeMac('aabb.ccdd.eeff')).toBe('AA:BB:CC:DD:EE:FF');
  });

  it('rejects invalid values', () => {
    expect(normalizeMac(123)).toBeNull();
    expect(normalizeMac('AA:BB:CC')).toBeNull();
    expect(normalizeMac('ZZ:BB:CC:DD:EE:FF')).toBeNull();
  });

  it('detects the locally administered bit', () => {
    expect(isRandomizedMac('02:11:22:33:44:55')).toBe(true);
    expect(isRandomizedMac('DA:11:22:33:44:55')).toBe(true);
    expect(isRandomizedMac('00:11:22:33:44:55')).toBe(false);
    expect(isRandomizedMac('F4:11:22:33:44:55')).toBe(false);
  });
});
