import { isRouteErrorResponse, useRouteError } from "react-router";
import { Compass } from "lucide-react";
import { ButtonLink } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/Feedback";

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-20 sm:px-6">
      <EmptyState
        icon={<Compass />}
        title="We couldn't find that page"
        description="The link may be broken, or the page may have moved."
        action={
          <div className="flex gap-2">
            <ButtonLink to="/">Go home</ButtonLink>
            <ButtonLink to="/products" variant="outline">
              Browse products
            </ButtonLink>
          </div>
        }
      />
    </div>
  );
}

export function RouteErrorPage() {
  const error = useRouteError();
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : "Unexpected error";
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas p-6">
      <EmptyState
        icon={<Compass />}
        title="Something went wrong"
        description={message}
        action={<ButtonLink to="/">Back to the store</ButtonLink>}
      />
    </div>
  );
}
