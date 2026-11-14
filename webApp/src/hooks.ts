import { useEffect, useState } from "react";
import type { RefObject } from "react";

/** 每隔 intervalMs 更新一次的当前时间，用来让卡片在整点切到下一档、推进进度线。 */
export function useClock(intervalMs: number): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), intervalMs);
    return () => window.clearInterval(timerId);
  }, [intervalMs]);
  return now;
}

/**
 * 元素是否进入（或接近）视口。rootMargin 让它提前一段距离就算「接近」，滚动到时数据已经在路上了。
 * 不支持 IntersectionObserver 的环境一律当作可见。
 */
export function useNearViewport(ref: RefObject<Element | null>, rootMargin: string): boolean {
  const [near, setNear] = useState(false);
  useEffect(() => {
    const element = ref.current;
    if (element == null) {
      return;
    }
    if (typeof IntersectionObserver === "undefined") {
      setNear(true);
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => setNear(entries.some((entry) => entry.isIntersecting)),
      { rootMargin },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [ref, rootMargin]);
  return near;
}
