import { useState } from "react";
import { Package } from "lucide-react";
import { cn } from "@/lib/cn";
import { resolveImageUrl } from "@/lib/format";

export function ProductImage({
  src,
  alt,
  className,
  imgClassName,
}: {
  src: string | null | undefined;
  alt: string;
  className?: string;
  imgClassName?: string;
}) {
  const url = resolveImageUrl(src);
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const failed = !url || failedUrl === url;

  return (
    <div className={cn("relative flex items-center justify-center overflow-hidden bg-white", className)}>
      {failed ? (
        <div className="flex h-full w-full flex-col items-center justify-center gap-1 bg-linear-to-br from-slate-50 to-slate-100 text-slate-300">
          <Package className="h-1/3 max-h-12 w-1/3 max-w-12" strokeWidth={1.4} />
        </div>
      ) : (
        <img
          src={url}
          alt={alt}
          loading="lazy"
          onError={() => setFailedUrl(url)}
          className={cn("h-full w-full object-contain", imgClassName)}
        />
      )}
    </div>
  );
}
