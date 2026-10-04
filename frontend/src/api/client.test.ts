import { describe, expect, it } from "vitest";
import { toPage } from "./client";

// The backend returns three different page shapes; every list view relies on toPage.
describe("toPage", () => {
  it("normalizes the app's own ProductResponse/OrderResponse shape", () => {
    expect(toPage({ content: [1, 2], pageNumber: 1, pageSize: 2, totalElements: 6, totalPages: 3, lastPage: false })).toEqual({
      content: [1, 2],
      pageNumber: 1,
      pageSize: 2,
      totalElements: 6,
      totalPages: 3,
      lastPage: false,
    });
  });

  it("normalizes a raw Spring Data Page", () => {
    expect(toPage({ content: ["a"], number: 2, size: 10, totalElements: 21, totalPages: 3, last: true })).toMatchObject({
      pageNumber: 2,
      pageSize: 10,
      totalElements: 21,
      lastPage: true,
    });
  });

  it("normalizes Spring's PagedModel (page metadata nested)", () => {
    const page = toPage({ content: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } });
    expect(page).toMatchObject({ pageNumber: 0, pageSize: 20, totalElements: 0 });
    expect(page.lastPage).toBe(true);
  });

  it("tolerates an empty body", () => {
    expect(toPage({})).toMatchObject({ content: [], pageNumber: 0, totalPages: 1, lastPage: true });
  });
});
