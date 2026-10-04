import { Link } from "react-router";
import { CreditCard, Headset, RotateCcw, ShieldCheck, Truck } from "lucide-react";
import { brand } from "@/config/brand";
import { formatMoney } from "@/lib/format";
import { Logo } from "./Logo";

const perks = [
  { icon: Truck, title: "Fast delivery", text: `Free over ${formatMoney(brand.freeShippingThresholdMinor)}` },
  { icon: ShieldCheck, title: "Secure payments", text: "Stripe & Razorpay protected" },
  { icon: RotateCcw, title: "Easy returns", text: "Hassle-free within 7 days" },
  { icon: Headset, title: "Real support", text: "We answer within a day" },
];

export function Footer() {
  const col = "space-y-2.5 text-sm";
  const link = "text-slate-400 transition-colors hover:text-white";
  return (
    <footer className="mt-16 bg-slate-950 text-slate-300">
      <div className="border-b border-white/5">
        <div className="mx-auto grid max-w-7xl grid-cols-2 gap-6 px-4 py-8 sm:px-6 lg:grid-cols-4">
          {perks.map(({ icon: Icon, title, text }) => (
            <div key={title} className="flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/5 text-amber-400">
                <Icon className="h-5 w-5" />
              </span>
              <div>
                <p className="text-sm font-semibold text-white">{title}</p>
                <p className="text-xs text-slate-400">{text}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-2 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <Logo inverted />
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-slate-400">{brand.tagline}. Thousands of products from verified independent sellers, delivered to your door.</p>
        </div>
        <div className={col}>
          <p className="font-semibold text-white">Shop</p>
          <Link to="/products" className={`block ${link}`}>All products</Link>
          <Link to="/products?sort=discount" className={`block ${link}`}>Top deals</Link>
          <Link to="/products?sort=newest" className={`block ${link}`}>New arrivals</Link>
        </div>
        <div className={col}>
          <p className="font-semibold text-white">Your account</p>
          <Link to="/orders" className={`block ${link}`}>Orders</Link>
          <Link to="/account/wishlist" className={`block ${link}`}>Wishlist</Link>
          <Link to="/account/addresses" className={`block ${link}`}>Addresses</Link>
          <Link to="/account" className={`block ${link}`}>Settings</Link>
        </div>
        <div className={col}>
          <p className="font-semibold text-white">Sell with us</p>
          <Link to="/account/seller" className={`block ${link}`}>Become a seller</Link>
          <Link to="/seller/dashboard" className={`block ${link}`}>Seller dashboard</Link>
          <a href={`mailto:${brand.supportEmail}`} className={`block ${link}`}>Contact support</a>
        </div>
      </div>

      <div className="border-t border-white/5">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-4 py-5 text-xs text-slate-500 sm:flex-row sm:px-6">
          <p>© {new Date().getFullYear()} {brand.name}. All rights reserved.</p>
          <p className="flex items-center gap-2">
            <CreditCard className="h-4 w-4" /> Payments secured by Stripe & Razorpay
          </p>
        </div>
      </div>
    </footer>
  );
}
