import { useEffect, useRef, useState, type RefObject } from "react";

export function useClickOutside<T extends HTMLElement>(ref: RefObject<T | null>, onOutside: () => void, enabled = true) {
  const cb = useRef(onOutside);
  cb.current = onOutside;
  useEffect(() => {
    if (!enabled) return;
    const handler = (e: MouseEvent | TouchEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) cb.current();
    };
    document.addEventListener("mousedown", handler);
    document.addEventListener("touchstart", handler);
    return () => {
      document.removeEventListener("mousedown", handler);
      document.removeEventListener("touchstart", handler);
    };
  }, [ref, enabled]);
}

export function useDebounced<T>(value: T, delay = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function useDocumentTitle(title: string | undefined) {
  useEffect(() => {
    if (title) document.title = `${title} · Vendora`;
  }, [title]);
}
