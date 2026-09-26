import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Cribby — roommates and homes, together",
  description: "Find compatible roommates, form a group, and discover a home that works for everyone.",
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="antialiased">{children}</body>
    </html>
  );
}
