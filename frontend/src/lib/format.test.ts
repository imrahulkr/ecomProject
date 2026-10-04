import { describe, expect, it } from "vitest";
import { formatMoney, toMajorUnits, toMinorUnits } from "./format";

// Backend money is integer minor units (paise); these helpers are the only conversion point.
describe("formatMoney", () => {
  it("drops decimals for whole rupee amounts", () => {
    expect(formatMoney(129_900, "INR")).toBe("₹1,299");
  });

  it("keeps two decimals otherwise", () => {
    expect(formatMoney(209_940, "INR")).toBe("₹2,099.40");
  });

  it("uses Indian digit grouping", () => {
    expect(formatMoney(1_680_000_00, "INR")).toBe("₹16,80,000");
  });

  it("treats missing amounts as zero", () => {
    expect(formatMoney(null)).toBe("₹0");
    expect(formatMoney(undefined)).toBe("₹0");
  });
});

describe("toMinorUnits / toMajorUnits", () => {
  it("round-trips without floating-point drift", () => {
    expect(toMinorUnits("19.99")).toBe(1999);
    expect(toMinorUnits(0.1 + 0.2)).toBe(30);
    expect(toMajorUnits(1999)).toBe(19.99);
  });

  it("returns 0 for unparseable input", () => {
    expect(toMinorUnits("abc")).toBe(0);
  });
});
