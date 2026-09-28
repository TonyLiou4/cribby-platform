"use client";

import Image from "next/image";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Bath, BedDouble, Bell, Building2, CalendarDays, Check, ChevronDown,
  ChevronLeft, ChevronRight, CircleDollarSign, Filter, Heart, Home,
  KeyRound, MapPin, MessageCircle, MoreHorizontal, PawPrint, Plus,
  Search, Send, Settings2, ShieldCheck, Sparkles, UserRound, UsersRound, X,
} from "lucide-react";
import { toast } from "sonner";

import { Avatar, AvatarFallback, AvatarGroup } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel,
  DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Sheet, SheetClose, SheetContent, SheetDescription, SheetFooter,
  SheetHeader, SheetTitle, SheetTrigger,
} from "@/components/ui/sheet";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { Toaster } from "@/components/ui/sonner";
import { useIsMobile } from "@/hooks/use-mobile";
import { cn } from "@/lib/utils";

type Role = "student" | "manager";
type StudentTab = "overview" | "homes" | "roommates" | "messages" | "profile";
type ManagerTab = "overview" | "properties" | "inquiries" | "profile";
type AppTab = StudentTab | ManagerTab;

type HomeListing = {
  id: number; title: string; place: string; rent: number; fit: number;
  beds: number; baths: number; available: string; pets: boolean;
};

type NavItem = { value: string; label: string; icon: typeof Home };

const studentNav = [
  { value: "overview", label: "Overview", icon: Home },
  { value: "homes", label: "Homes", icon: Building2 },
  { value: "roommates", label: "Roommates", icon: UsersRound },
  { value: "messages", label: "Messages", icon: MessageCircle },
  { value: "profile", label: "Profile", icon: UserRound },
] as const;

const managerNav = [
  { value: "overview", label: "Overview", icon: Home },
  { value: "properties", label: "Properties", icon: Building2 },
  { value: "inquiries", label: "Inquiries", icon: MessageCircle },
  { value: "profile", label: "Profile", icon: UserRound },
] as const;

const listings: HomeListing[] = [
  { id: 1, title: "Two-bedroom near South Campus", place: "University District · 8 min to OSU", rent: 1680, fit: 96, beds: 2, baths: 1, available: "Aug 12", pets: true },
  { id: 2, title: "Quiet apartment on King Avenue", place: "Grandview · COTA line 1", rent: 1540, fit: 91, beds: 2, baths: 1, available: "Aug 1", pets: false },
  { id: 3, title: "Renovated three-bedroom on Neil", place: "Victorian Village · 11 min to OSU", rent: 2290, fit: 88, beds: 3, baths: 2, available: "Jul 28", pets: true },
  { id: 4, title: "Sunlit duplex near Lane Avenue", place: "Old North · bike to campus", rent: 1895, fit: 84, beds: 3, baths: 1.5, available: "Aug 15", pets: true },
];

const roommates = [
  { name: "Maya Chen", initials: "MC", match: 94, school: "OSU · Design", budget: "$850–$1,000", note: "Early riser · tidy · no smoking", color: "bg-[#e8f0ff] text-[#135cc5]" },
  { name: "Eli Brooks", initials: "EB", match: 91, school: "OSU · Engineering", budget: "$800–$950", note: "Quiet weekdays · has a cat", color: "bg-[#ecf8f1] text-[#24734b]" },
  { name: "Sam Rivera", initials: "SR", match: 87, school: "CCAD · Illustration", budget: "$750–$900", note: "Night owl · social weekends", color: "bg-[#fff2df] text-[#9a5b07]" },
];

const managedProperties = [
  { id: 1, name: "Neil Avenue Duplex", rent: "$2,290", status: "Active", views: 184, saves: 31, inquiries: 8 },
  { id: 2, name: "King Avenue Apartment", rent: "$1,540", status: "Active", views: 126, saves: 19, inquiries: 5 },
  { id: 3, name: "High Street Studio", rent: "$1,120", status: "Paused", views: 73, saves: 8, inquiries: 2 },
];

type ChatMessage = { mine: boolean; text: string };
type Conversation = {
  id: string; name: string; detail: string; initials: string;
  preview: string; seed: ChatMessage[];
};

const conversations: Conversation[] = [
  {
    id: "alex", name: "Alex Morgan", detail: "Neil Avenue Duplex · Landlord", initials: "AM",
    preview: "Saturday afternoon works…",
    seed: [
      { mine: false, text: "Hi Jordan — your group looks like a strong fit. Would you like to tour Saturday?" },
      { mine: true, text: "Saturday afternoon works for all three of us." },
    ],
  },
  {
    id: "maya", name: "Maya Chen", detail: "Roommate group", initials: "MC",
    preview: "I can cover up to $950 each month.",
    seed: [
      { mine: false, text: "I ran the numbers — I can cover up to $950 each month." },
      { mine: true, text: "That works. Eli, how does that land for you?" },
    ],
  },
  {
    id: "eli", name: "Eli Brooks", detail: "Roommate group", initials: "EB",
    preview: "Shared a new housing preference",
    seed: [
      { mine: false, text: "I added pet-friendly to my preferences — my cat is coming with me." },
      { mine: true, text: "Makes sense. Most of our matches already allow pets." },
    ],
  },
];

type ModelContext = {
  registerTool: (tool: {
    name: string; title: string; description: string; inputSchema: object;
    annotations?: { readOnlyHint?: boolean; untrustedContentHint?: boolean };
    execute: (input: Record<string, unknown>) => unknown;
  }, options?: { signal?: AbortSignal }) => void | Promise<void>;
};

export default function HomePage() {
  const [role, setRole] = useState<Role>("student");
  const [activeTab, setActiveTab] = useState<AppTab>("overview");
  const [saved, setSaved] = useState<number[]>([2]);
  const [connected, setConnected] = useState<string[]>([]);
  const [search, setSearch] = useState("");
  const [maxRent, setMaxRent] = useState(2400);

  const nav: readonly NavItem[] = role === "student" ? studentNav : managerNav;
  const filteredListings = useMemo(() => listings.filter((home) => {
    const matchesSearch = `${home.title} ${home.place}`.toLowerCase().includes(search.toLowerCase());
    return matchesSearch && home.rent <= maxRent;
  }), [maxRent, search]);

  const now = new Date();
  const currentHour = now.getHours();
  const greeting = currentHour < 12 ? "Good morning" : currentHour < 18 ? "Good afternoon" : "Good evening";
  const dateLabel = now.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" });

  function changeRole(nextRole: Role) {
    setRole(nextRole);
    setActiveTab("overview");
    toast.success(nextRole === "student" ? "Student workspace opened" : "Property manager workspace opened");
  }

  function toggleSaved(id: number) {
    setSaved((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
    toast.success(saved.includes(id) ? "Removed from saved homes" : "Home saved for your group");
  }

  function toggleConnection(name: string) {
    setConnected((current) => current.includes(name) ? current : [...current, name]);
    toast.success(`Connection request sent to ${name}`);
  }

  useEffect(() => {
    const context = (document as Document & { modelContext?: ModelContext }).modelContext;
    if (!context?.registerTool) return;
    const lifecycle = new AbortController();
    const register = async () => {
      await context.registerTool({
        name: "search_cribby_homes",
        title: "Search Cribby homes",
        description: "Search visible Cribby housing results by keyword and optional maximum monthly rent.",
        inputSchema: { type: "object", properties: { query: { type: "string" }, maxRent: { type: "number" } }, additionalProperties: false },
        annotations: { readOnlyHint: true, untrustedContentHint: false },
        execute(input) {
          const query = typeof input.query === "string" ? input.query : "";
          const limit = typeof input.maxRent === "number" ? input.maxRent : maxRent;
          setRole("student"); setActiveTab("homes"); setSearch(query); setMaxRent(limit);
          const results = listings.filter((home) => `${home.title} ${home.place}`.toLowerCase().includes(query.toLowerCase()) && home.rent <= limit);
          return { count: results.length, homes: results.map(({ id, title, rent, fit }) => ({ id, title, rent, fit })) };
        },
      }, { signal: lifecycle.signal });
      await context.registerTool({
        name: "open_cribby_workspace",
        title: "Open Cribby workspace",
        description: "Open either the student or property-manager overview.",
        inputSchema: { type: "object", properties: { role: { type: "string", enum: ["student", "manager"] } }, required: ["role"], additionalProperties: false },
        annotations: { readOnlyHint: true, untrustedContentHint: false },
        execute(input) {
          const next = input.role === "manager" ? "manager" : "student";
          setRole(next); setActiveTab("overview");
          return { role: next, tab: "overview" };
        },
      }, { signal: lifecycle.signal });
      await context.registerTool({
        name: "save_cribby_home",
        title: "Save a Cribby home",
        description: "Save one visible home to the active student's shortlist.",
        inputSchema: { type: "object", properties: { listingId: { type: "number" } }, required: ["listingId"], additionalProperties: false },
        annotations: { readOnlyHint: false, untrustedContentHint: false },
        execute(input) {
          const id = Number(input.listingId);
          const home = listings.find((item) => item.id === id);
          if (!home) throw new Error("Listing not found");
          setSaved((current) => current.includes(id) ? current : [...current, id]);
          return { saved: true, listingId: id, title: home.title };
        },
      }, { signal: lifecycle.signal });
    };
    void register().catch(() => undefined);
    return () => lifecycle.abort();
  }, [maxRent]);

  return (
    <main className="min-h-dvh bg-background text-foreground">
      <Toaster position="top-center" offset={58} richColors />
      <Header role={role} onRoleChange={changeRole} onOpenProfile={() => setActiveTab("profile")} />

      <Tabs
        value={activeTab}
        onValueChange={(value) => setActiveTab(value as AppTab)}
        className="mx-auto max-w-[1440px] gap-0 lg:grid lg:grid-cols-[220px_minmax(0,1fr)]"
      >
        <Sidebar role={role} nav={nav} activeTab={activeTab} onChange={(tab) => setActiveTab(tab)} />
        <section
          id="main"
          className="min-w-0 px-4 pb-[calc(7.5rem+env(safe-area-inset-bottom))] pt-6 sm:px-8 sm:pt-9 lg:px-12 lg:pb-20 lg:pt-12 xl:px-16"
        >
          {role === "student" ? (
            <>
              <TabsContent value="overview"><StudentOverview dateLabel={dateLabel} greeting={greeting} onNavigate={(tab) => setActiveTab(tab)} saved={saved} onSave={toggleSaved} /></TabsContent>
              <TabsContent value="homes"><HomesView listings={filteredListings} search={search} setSearch={setSearch} maxRent={maxRent} setMaxRent={setMaxRent} saved={saved} onSave={toggleSaved} /></TabsContent>
              <TabsContent value="roommates"><RoommatesView connected={connected} onConnect={toggleConnection} /></TabsContent>
              <TabsContent value="messages"><MessagesView /></TabsContent>
              <TabsContent value="profile"><ProfileView role={role} /></TabsContent>
            </>
          ) : (
            <>
              <TabsContent value="overview"><ManagerOverview onNavigate={(tab) => setActiveTab(tab)} /></TabsContent>
              <TabsContent value="properties"><PropertiesView /></TabsContent>
              <TabsContent value="inquiries"><InquiriesView /></TabsContent>
              <TabsContent value="profile"><ProfileView role={role} /></TabsContent>
            </>
          )}
        </section>
        <MobileNav nav={nav} />
      </Tabs>
    </main>
  );
}

/* ----------------------------- Chrome (nav) ----------------------------- */

function Header({ role, onRoleChange, onOpenProfile }: { role: Role; onRoleChange: (role: Role) => void; onOpenProfile: () => void }) {
  return (
    <header className="sticky top-0 z-50 border-b border-border/70 bg-background/75 pt-[env(safe-area-inset-top)] backdrop-blur-xl supports-[backdrop-filter]:bg-background/65">
      <div className="mx-auto flex h-14 max-w-[1440px] items-center gap-2 px-3 sm:gap-4 sm:px-8">
        <a className="flex items-center gap-2 rounded-md" href="#main" aria-label="Cribby overview">
          <span className="grid size-8 place-items-center rounded-[9px] bg-foreground text-background">
            <Home className="size-4" strokeWidth={2.5} />
          </span>
          <span className="text-[17px] font-semibold tracking-[-0.02em]">Cribby</span>
        </a>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="secondary" className="ml-auto h-9 rounded-full px-2.5 text-sm font-medium sm:px-3.5">
              {role === "student" ? <UsersRound className="size-4" /> : <Building2 className="size-4" />}
              <span className="hidden sm:inline">{role === "student" ? "Student" : "Property manager"}</span>
              <ChevronDown className="size-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-60 rounded-2xl p-1.5">
            <DropdownMenuLabel className="px-2.5 py-2 text-xs font-medium text-muted-foreground">Switch workspace</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={role} onValueChange={(value) => onRoleChange(value as Role)}>
              <DropdownMenuRadioItem value="student" className="rounded-xl py-2.5"><UsersRound className="size-4" /> Student</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="manager" className="rounded-xl py-2.5"><Building2 className="size-4" /> Property manager</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>
        <Button variant="ghost" size="icon" className="size-9 rounded-full" aria-label="Notifications">
          <Bell className="size-[18px]" />
        </Button>
        <button
          onClick={onOpenProfile}
          className="flex h-10 items-center gap-2 rounded-full px-1.5 text-sm font-medium hover:bg-secondary"
          type="button"
        >
          <Avatar className="size-8">
            <AvatarFallback className="bg-foreground text-[11px] font-semibold text-background">JL</AvatarFallback>
          </Avatar>
          <span className="hidden sm:inline">Jordan Lee</span>
          <ChevronDown className="hidden size-3.5 text-muted-foreground sm:inline" />
        </button>
      </div>
    </header>
  );
}

function Sidebar({ role, nav, activeTab, onChange }: { role: Role; nav: readonly NavItem[]; activeTab: AppTab; onChange: (tab: AppTab) => void }) {
  return (
    <aside className="hidden lg:block">
      <div className="sticky top-[calc(3.5rem+env(safe-area-inset-top))] flex h-[calc(100dvh-3.5rem-env(safe-area-inset-top))] flex-col overflow-y-auto px-4 py-8">
        <p className="px-3 text-[11px] font-semibold uppercase tracking-[0.12em] text-muted-foreground">
          {role === "student" ? "Student workspace" : "Manager workspace"}
        </p>
        <nav className="mt-3 space-y-0.5" aria-label="Workspace navigation">
          {nav.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => onChange(item.value as AppTab)}
              className={cn(
                "flex h-10 w-full items-center gap-3 rounded-xl px-3 text-[15px] transition-colors",
                activeTab === item.value
                  ? "bg-foreground/[0.07] font-semibold text-foreground dark:bg-foreground/[0.12]"
                  : "text-muted-foreground hover:bg-foreground/[0.04] hover:text-foreground",
              )}
            >
              <item.icon className="size-[18px]" />
              {item.label}
            </button>
          ))}
        </nav>
      </div>
    </aside>
  );
}

function MobileNav({ nav }: { nav: readonly NavItem[] }) {
  return (
    <TabsList className="fixed inset-x-0 bottom-0 z-50 h-auto w-full justify-stretch gap-0 rounded-none border-t border-border/70 bg-background/80 px-1 pb-[env(safe-area-inset-bottom)] pt-1.5 shadow-none backdrop-blur-xl supports-[backdrop-filter]:bg-background/70 lg:hidden">
      {nav.map((item) => (
        <TabsTrigger
          key={item.value}
          value={item.value}
          className="after:hidden h-11 min-w-0 flex-1 flex-col gap-0.5 rounded-lg px-0.5 text-[10px] font-medium data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none"
        >
          <item.icon className="size-[21px]" />
          {item.label}
        </TabsTrigger>
      ))}
    </TabsList>
  );
}

/* ------------------------------ Primitives ------------------------------ */

function PageHeading({ eyebrow, title, copy, action }: { eyebrow: string; title: string; copy?: string; action?: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <p className="text-[13px] font-medium text-muted-foreground">{eyebrow}</p>
        <h1 className="mt-1.5 text-[34px] font-semibold leading-[1.08] tracking-[-0.03em] sm:text-5xl">{title}</h1>
        {copy && <p className="mt-3 max-w-2xl text-[17px] leading-relaxed text-muted-foreground">{copy}</p>}
      </div>
      {action && <div className="shrink-0 sm:mb-1">{action}</div>}
    </div>
  );
}

function SectionHeading({ title, copy, action }: { title: string; copy: string; action?: React.ReactNode }) {
  return (
    <div className="mt-12 flex items-end justify-between gap-4">
      <div>
        <h2 className="text-2xl font-semibold tracking-[-0.025em] sm:text-[28px]">{title}</h2>
        <p className="mt-1 text-sm text-muted-foreground">{copy}</p>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

// Apple's signature blue "Learn more ›" style link.
function TextLink({ children, className, ...props }: React.ComponentProps<"button">) {
  return (
    <button
      type="button"
      className={cn("flex items-center gap-0.5 text-[15px] font-medium text-primary hover:underline", className)}
      {...props}
    >
      {children}
      <ChevronRight className="size-4" />
    </button>
  );
}

/* --------------------------- Student: Overview -------------------------- */

function StudentOverview({ dateLabel, greeting, onNavigate, saved, onSave }: { dateLabel: string; greeting: string; onNavigate: (tab: StudentTab) => void; saved: number[]; onSave: (id: number) => void }) {
  return (
    <>
      <PageHeading
        eyebrow={dateLabel}
        title={`${greeting}, Jordan.`}
        copy="Your group is almost ready. Compare homes that work for everyone."
        action={<Button onClick={() => onNavigate("roommates")} className="h-11 rounded-full px-5"><UsersRound className="size-4" />View my group</Button>}
      />
      <div className="mt-8 grid gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.78fr)]">
        <article className="relative min-h-[440px] overflow-hidden rounded-[24px] bg-zinc-950 text-white sm:min-h-[500px]">
          <Image
            src="/images/cribby-columbus-apartment.png"
            alt="Bright two-bedroom apartment living room near campus"
            fill
            priority
            className="object-cover opacity-90"
            sizes="(max-width: 1280px) 100vw, 760px"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/35 to-black/10" />
          <div className="relative flex h-full min-h-[440px] flex-col items-center justify-end p-7 text-center sm:items-start sm:p-10 sm:text-left">
            <p className="text-[12px] font-semibold uppercase tracking-[0.14em] text-white/70">12 new group matches</p>
            <h2 className="mt-2 text-4xl font-semibold tracking-[-0.03em] sm:text-[44px] sm:leading-[1.05]">One search. Everyone&apos;s needs.</h2>
            <p className="mt-3 max-w-md text-[15px] leading-relaxed text-white/75">Combined budget, move-in date, commute, pets, and space — already lined up.</p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-x-6 gap-y-3 sm:justify-start">
              <Button onClick={() => onNavigate("homes")} className="h-11 rounded-full bg-white px-6 text-foreground hover:bg-white/90">Explore matches</Button>
              <button type="button" onClick={() => onNavigate("homes")} className="flex items-center gap-1 text-[15px] font-medium text-[#5eb5ff] hover:underline">
                Learn more <ChevronRight className="size-4" />
              </button>
            </div>
          </div>
        </article>
        <GroupReadiness />
      </div>
      <SectionHeading
        title="Best matches for your group"
        copy="Ranked by the preferences your group shares."
        action={<TextLink onClick={() => onNavigate("homes")}>View all</TextLink>}
      />
      <div className="mt-5 grid gap-4 md:grid-cols-2">
        {listings.slice(0, 2).map((home) => (
          <ListingCard key={home.id} home={home} saved={saved.includes(home.id)} onSave={() => onSave(home.id)} />
        ))}
      </div>
    </>
  );
}

function GroupReadiness() {
  const rows: [string, string, boolean][] = [
    ["Combined budget", "$2,400 / month", true],
    ["Move-in date", "August 12", true],
    ["Neighborhoods", "Choose together", false],
  ];
  return (
    <Card className="justify-center rounded-[24px] border-border/70 p-7 shadow-none sm:p-8">
      <div className="flex items-baseline justify-between gap-4">
        <p className="text-sm text-muted-foreground">Group readiness</p>
        <p className="text-5xl font-semibold tracking-[-0.03em]">82%</p>
      </div>
      <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-secondary">
        <div className="h-full w-[82%] rounded-full bg-primary" />
      </div>
      <div className="mt-4 divide-y divide-border/70">
        {rows.map(([label, value, done]) => (
          <div key={label} className="flex items-center justify-between gap-4 py-3 text-[15px] first:pt-0 last:pb-0">
            <span className="text-muted-foreground">{label}</span>
            <span className={cn("font-medium", !done && "text-primary")}>{value}</span>
          </div>
        ))}
      </div>
      <Button variant="outline" className="mt-5 h-11 w-full rounded-full border-border" onClick={() => toast.info("Group preferences opened")}>
        Finish group setup
      </Button>
    </Card>
  );
}

/* ----------------------------- Student: Homes --------------------------- */

function ListingCard({ home, saved, onSave }: { home: HomeListing; saved: boolean; onSave: () => void }) {
  return (
    <Card className="group gap-0 overflow-hidden rounded-[24px] border-border/70 p-0 shadow-none transition-shadow duration-300 hover:shadow-[0_8px_30px_rgba(0,0,0,0.08)]">
      <div className="relative aspect-[3/2] overflow-hidden bg-secondary">
        <Image
          src="/images/cribby-columbus-apartment.png"
          alt=""
          fill
          className={cn(
            "object-cover transition-transform duration-700 group-hover:scale-[1.03]",
            home.id % 2 === 0 ? "object-[70%_45%]" : "object-center",
          )}
          sizes="(max-width: 768px) 100vw, 45vw"
        />
        <button
          aria-label={`${saved ? "Unsave" : "Save"} ${home.title}`}
          onClick={onSave}
          className="absolute right-3 top-3 grid size-10 place-items-center rounded-full bg-white/85 text-foreground backdrop-blur transition hover:bg-white active:scale-95"
          type="button"
        >
          <Heart className={cn("size-[18px]", saved && "fill-primary text-primary")} />
        </button>
        <span className="absolute left-3 top-3 rounded-full bg-white/85 px-3 py-1 text-xs font-semibold text-foreground backdrop-blur">
          {home.fit}% fit
        </span>
      </div>
      <div className="p-5 sm:p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="text-[17px] font-semibold tracking-[-0.01em]">{home.title}</h3>
            <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
              <MapPin className="size-3.5 shrink-0" />
              {home.place}
            </p>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-[17px] font-semibold tracking-tight">${home.rent.toLocaleString()}</p>
            <p className="text-xs text-muted-foreground">per month</p>
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1.5 border-t border-border/70 pt-4 text-[13px] text-muted-foreground">
          <span className="flex items-center gap-1"><BedDouble className="size-3.5" />{home.beds} beds</span>
          <span className="flex items-center gap-1"><Bath className="size-3.5" />{home.baths} baths</span>
          <span className="flex items-center gap-1"><CalendarDays className="size-3.5" />{home.available}</span>
          {home.pets && <span className="flex items-center gap-1"><PawPrint className="size-3.5" />Pets</span>}
        </div>
      </div>
    </Card>
  );
}

function HomesView({ listings: homes, search, setSearch, maxRent, setMaxRent, saved, onSave }: { listings: HomeListing[]; search: string; setSearch: (value: string) => void; maxRent: number; setMaxRent: (value: number) => void; saved: number[]; onSave: (id: number) => void }) {
  const quickFilters = ["For your group", "2–3 bedrooms", "Near campus", "Pets allowed", "Available August"];
  return (
    <>
      <PageHeading
        eyebrow={`${homes.length} homes match your group`}
        title="Find your next home."
        copy="Every result is checked against your group's combined budget, move-in date, and space needs."
      />
      <div className="mt-7 flex gap-2.5">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="h-12 rounded-2xl border-border bg-card pl-11 shadow-none"
            placeholder="Search neighborhood or home"
          />
        </div>
        <FilterSheet maxRent={maxRent} setMaxRent={setMaxRent} />
      </div>
      <div className="scrollbar-none mt-4 flex gap-2 overflow-x-auto pb-1">
        {quickFilters.map((filter, index) => (
          <Button
            key={filter}
            variant={index === 0 ? "default" : "outline"}
            className={cn("h-9 shrink-0 rounded-full px-4 text-[13px]", index !== 0 && "border-border bg-card text-muted-foreground shadow-none")}
          >
            {filter}
          </Button>
        ))}
      </div>
      {homes.length ? (
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          {homes.map((home) => (
            <ListingCard key={home.id} home={home} saved={saved.includes(home.id)} onSave={() => onSave(home.id)} />
          ))}
        </div>
      ) : (
        <Card className="mt-8 items-center rounded-[24px] p-10 text-center shadow-none">
          <span className="grid size-12 place-items-center rounded-full bg-secondary"><Search className="size-5 text-muted-foreground" /></span>
          <h2 className="mt-4 font-semibold">No homes match yet</h2>
          <p className="text-sm text-muted-foreground">Try a different neighborhood or raise the maximum rent.</p>
          <Button variant="outline" className="mt-2 rounded-full border-border" onClick={() => { setSearch(""); setMaxRent(2400); }}>
            Clear filters
          </Button>
        </Card>
      )}
    </>
  );
}

function FilterSheet({ maxRent, setMaxRent }: { maxRent: number; setMaxRent: (value: number) => void }) {
  const [draft, setDraft] = useState(maxRent);
  const isMobile = useIsMobile();
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="outline" className="h-12 shrink-0 rounded-2xl border-border bg-card px-5"><Filter className="size-4" />Filters</Button>
      </SheetTrigger>
      <SheetContent
        side={isMobile ? "bottom" : "right"}
        className={cn(
          "pb-[env(safe-area-inset-bottom)]",
          isMobile
            ? "max-h-[90dvh] overflow-y-auto rounded-t-[26px]"
            : "w-full sm:max-w-md",
        )}
      >
        <SheetHeader className="px-6 pt-7 text-left">
          <SheetTitle className="text-2xl tracking-tight">Home filters</SheetTitle>
          <SheetDescription>Keep the results realistic for your entire group.</SheetDescription>
        </SheetHeader>
        <div className="space-y-7 px-6 py-6">
          <div>
            <Label htmlFor="max-rent">Maximum monthly rent</Label>
            <Input id="max-rent" className="mt-2 h-12 rounded-xl" type="number" value={draft} onChange={(e) => setDraft(Number(e.target.value))} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Bedrooms</Label>
              <Button variant="outline" className="mt-2 h-12 w-full justify-between rounded-xl border-border"><span>2–3</span><ChevronDown className="size-4" /></Button>
            </div>
            <div>
              <Label>Move-in</Label>
              <Button variant="outline" className="mt-2 h-12 w-full justify-between rounded-xl border-border"><span>August</span><ChevronDown className="size-4" /></Button>
            </div>
          </div>
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-[15px] font-medium">Pets allowed</p>
              <p className="text-xs text-muted-foreground">At least one pet-friendly option</p>
            </div>
            <Switch aria-label="Pets allowed" />
          </div>
        </div>
        <SheetFooter className="px-6 pb-[calc(1.5rem+env(safe-area-inset-bottom))] sm:flex-row">
          <SheetClose asChild>
            <Button className="h-12 flex-1 rounded-full" onClick={() => setMaxRent(draft)}>Show matching homes</Button>
          </SheetClose>
          <SheetClose asChild>
            <Button variant="ghost" className="rounded-full">Cancel</Button>
          </SheetClose>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

/* -------------------------- Student: Roommates -------------------------- */

function RoommatesView({ connected, onConnect }: { connected: string[]; onConnect: (name: string) => void }) {
  return (
    <>
      <PageHeading
        eyebrow="Preference-based, never mysterious"
        title="Meet compatible roommates."
        copy="Matches explain the habits, budget, timing, and housing needs you have in common."
        action={<Button className="h-11 rounded-full"><Settings2 className="size-4" />Edit preferences</Button>}
      />
      <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {roommates.map((mate) => {
          const isConnected = connected.includes(mate.name);
          return (
            <Card key={mate.name} className="rounded-[24px] border-border/70 p-7 shadow-none">
              <div className="flex items-start justify-between">
                <Avatar className={cn("size-14", mate.color)}>
                  <AvatarFallback className={cn(mate.color, "font-semibold")}>{mate.initials}</AvatarFallback>
                </Avatar>
                <Badge className="border-0 bg-primary/10 text-primary">{mate.match}% match</Badge>
              </div>
              <div className="mt-5">
                <h2 className="text-lg font-semibold tracking-[-0.01em]">{mate.name}</h2>
                <p className="mt-1 text-sm text-muted-foreground">{mate.school}</p>
              </div>
              <div className="space-y-2 text-[15px]">
                <p className="flex items-center gap-2"><CircleDollarSign className="size-4 text-muted-foreground" />{mate.budget}</p>
                <p className="flex items-start gap-2"><Sparkles className="mt-0.5 size-4 text-muted-foreground" />{mate.note}</p>
              </div>
              <Button
                disabled={isConnected}
                onClick={() => onConnect(mate.name)}
                variant={isConnected ? "secondary" : "default"}
                className="mt-auto h-11 w-full rounded-full"
              >
                {isConnected ? <><Check className="size-4" />Request sent</> : "Connect"}
              </Button>
            </Card>
          );
        })}
      </div>
      <SectionHeading title="Your roommate group" copy="Three people, one shared set of housing requirements." />
      <Card className="mt-5 flex-row flex-wrap items-center justify-between gap-4 rounded-[24px] border-border/70 p-6 shadow-none">
        <div className="flex items-center gap-4">
          <AvatarGroup>
            {["JL", "MC", "EB"].map((initials) => (
              <Avatar key={initials} className="size-10">
                <AvatarFallback className="bg-secondary text-foreground">{initials}</AvatarFallback>
              </Avatar>
            ))}
          </AvatarGroup>
          <div>
            <p className="font-semibold">Jordan, Maya &amp; Eli</p>
            <p className="text-sm text-muted-foreground">$2,400 combined budget · move-in August</p>
          </div>
        </div>
        <Button variant="outline" className="rounded-full border-border">Manage</Button>
      </Card>
    </>
  );
}

/* --------------------------- Student: Messages -------------------------- */

function MessagesView() {
  const [activeId, setActiveId] = useState(conversations[0].id);
  const [threadOpen, setThreadOpen] = useState(false);
  const [threads, setThreads] = useState<Record<string, ChatMessage[]>>(
    () => Object.fromEntries(conversations.map((conversation) => [conversation.id, conversation.seed])),
  );
  const [draft, setDraft] = useState("");

  const active = conversations.find((conversation) => conversation.id === activeId) ?? conversations[0];
  const messages = threads[active.id];

  function openConversation(id: string) {
    setActiveId(id);
    setThreadOpen(true);
    setDraft("");
  }

  function send(event: FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text) return;
    setThreads((current) => ({ ...current, [active.id]: [...current[active.id], { mine: true, text }] }));
    setDraft("");
  }

  return (
    <>
      <PageHeading
        eyebrow="3 active conversations"
        title="Messages."
        copy="Keep your roommate group and property managers in one clear thread."
      />
      <Card className="mt-8 gap-0 overflow-hidden rounded-[22px] border-border/70 p-0 md:grid md:h-[calc(100dvh-15rem)] md:min-h-[500px] md:grid-cols-[300px_minmax(0,1fr)]">
        {/* Conversation list — full screen on mobile until a thread is opened. */}
        <aside className={cn("border-b border-border md:border-b-0 md:border-r", threadOpen && "hidden md:block")}>
          <div className="border-b border-border p-3">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input className="h-11 rounded-xl border-border bg-secondary pl-10 shadow-none" placeholder="Search messages" />
            </div>
          </div>
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              onClick={() => openConversation(conversation.id)}
              className={cn(
                "flex min-h-[68px] w-full items-center gap-3 border-b border-border/60 px-4 text-left last:border-b-0 transition-colors",
                conversation.id === active.id ? "bg-secondary" : "hover:bg-secondary/60",
              )}
            >
              <Avatar className="size-12 shrink-0">
                <AvatarFallback className="bg-secondary text-foreground">{conversation.initials}</AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="truncate text-[15px] font-semibold">{conversation.name}</p>
                <p className="mt-0.5 truncate text-[13px] text-muted-foreground">{conversation.preview}</p>
              </div>
            </button>
          ))}
        </aside>

        {/* Thread — on phones it becomes a fixed "pushed screen" between the
            site header and the tab bar; on desktop it is the right grid pane. */}
        <section
          className={cn(
            "flex flex-col bg-card",
            "fixed inset-x-0 z-40 top-[calc(3.5rem+env(safe-area-inset-top))] bottom-[calc(3.25rem+env(safe-area-inset-bottom))]",
            "md:static md:z-auto md:inset-auto md:bg-transparent",
            !threadOpen && "hidden md:flex",
          )}
        >
          <div className="flex items-center gap-2.5 border-b border-border p-3 sm:px-4">
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden size-9 rounded-full"
              aria-label="Back to conversations"
              onClick={() => setThreadOpen(false)}
            >
              <ChevronLeft className="size-5" />
            </Button>
            <Avatar className="size-10">
              <AvatarFallback className="bg-secondary text-foreground">{active.initials}</AvatarFallback>
            </Avatar>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[15px] font-semibold">{active.name}</p>
              <p className="truncate text-xs text-muted-foreground">{active.detail}</p>
            </div>
            <Button variant="outline" className="hidden h-9 rounded-full border-border sm:inline-flex">View home</Button>
          </div>
          <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto p-4 sm:p-5">
            {messages.map((message, index) => (
              <div
                key={`${message.text}-${index}`}
                className={cn(
                  "max-w-[82%] px-4 py-2.5 text-[15px] leading-relaxed rounded-[20px]",
                  message.mine
                    ? "ml-auto rounded-br-md bg-primary text-primary-foreground"
                    : "rounded-bl-md bg-secondary",
                )}
              >
                {message.text}
              </div>
            ))}
          </div>
          <form onSubmit={send} className="flex items-center gap-2 border-t border-border p-3">
            <Input
              aria-label="Message"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              className="h-11 rounded-full border-border bg-secondary shadow-none"
              placeholder="Write a message"
            />
            <Button aria-label="Send message" size="icon" className="size-11 shrink-0 rounded-full">
              <Send className="size-[18px]" />
            </Button>
          </form>
        </section>
      </Card>
    </>
  );
}

/* --------------------------- Manager: Overview -------------------------- */

function ManagerOverview({ onNavigate }: { onNavigate: (tab: ManagerTab) => void }) {
  const stats = [
    { label: "Active listings", value: "2" },
    { label: "New inquiries", value: "13" },
    { label: "Saved this week", value: "58" },
    { label: "Average response", value: "1.8h" },
  ];
  return (
    <>
      <PageHeading
        eyebrow="Property manager workspace"
        title="Good morning, Jordan."
        copy="See which listings are getting attention and respond to qualified roommate groups."
        action={<NewListingDialog />}
      />
      <div className="mt-8 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.label} className="rounded-[22px] border-border/70 p-5 shadow-none sm:p-6">
            <p className="text-[13px] text-muted-foreground">{stat.label}</p>
            <p className="mt-2 text-4xl font-semibold tracking-[-0.03em]">{stat.value}</p>
          </Card>
        ))}
      </div>
      <SectionHeading
        title="Property performance"
        copy="Your active listings, ordered by renter interest."
        action={<TextLink onClick={() => onNavigate("properties")}>Manage all</TextLink>}
      />
      <div className="mt-5 space-y-3">
        {managedProperties.slice(0, 2).map((property) => (
          <PropertyRow key={property.id} property={property} />
        ))}
      </div>
      <SectionHeading
        title="Recent group inquiries"
        copy="Groups ready to move together tend to convert faster."
        action={<TextLink onClick={() => onNavigate("inquiries")}>View all</TextLink>}
      />
      <InquiryCard />
    </>
  );
}

/* -------------------------- Manager: Properties ------------------------- */

function PropertiesView() {
  return (
    <>
      <PageHeading
        eyebrow="3 rental properties"
        title="Manage your properties."
        copy="Keep availability, pricing, photos, and renter interest current."
        action={<NewListingDialog />}
      />
      <div className="mt-8 space-y-3">
        {managedProperties.map((property) => (
          <PropertyRow key={property.id} property={property} />
        ))}
      </div>
    </>
  );
}

function PropertyRow({ property }: { property: typeof managedProperties[number] }) {
  const [active, setActive] = useState(property.status === "Active");
  return (
    <Card className="flex-row items-center gap-3 rounded-[22px] border-border/70 p-4 shadow-none sm:gap-4 sm:p-5">
      <div className="relative hidden size-16 shrink-0 overflow-hidden rounded-2xl bg-secondary sm:block">
        <Image src="/images/cribby-columbus-apartment.png" alt="" fill className="object-cover" sizes="64px" />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="truncate text-[15px] font-semibold sm:text-base">{property.name}</h3>
          <span
            className={cn(
              "flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium",
              active ? "bg-emerald-500/12 text-emerald-600 dark:text-emerald-400" : "bg-secondary text-muted-foreground",
            )}
          >
            <span className="size-1.5 rounded-full bg-current" />
            {active ? "Active" : "Paused"}
          </span>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">{property.rent}/month</p>
        <div className="mt-2.5 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <span>{property.views} views</span>
          <span>{property.saves} saves</span>
          <span>{property.inquiries} inquiries</span>
        </div>
      </div>
      <Switch
        checked={active}
        onCheckedChange={(next) => { setActive(next); toast.success(next ? "Listing activated" : "Listing paused"); }}
        aria-label={`Toggle ${property.name}`}
        className="shrink-0"
      />
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="size-9 shrink-0" aria-label={`Actions for ${property.name}`}>
            <MoreHorizontal className="size-[18px]" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="rounded-2xl">
          <DropdownMenuItem onClick={() => toast.info("Listing editor opened")}>Edit listing</DropdownMenuItem>
          <DropdownMenuItem>Preview listing</DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant="destructive">Remove listing</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </Card>
  );
}

function NewListingDialog() {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button className="h-11 rounded-full px-5"><Plus className="size-4" />Add property</Button>
      </DialogTrigger>
      <DialogContent className="rounded-[24px] sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-2xl tracking-tight">Add a rental property</DialogTitle>
          <DialogDescription>Start with the facts students and roommate groups need to decide quickly.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-3 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Label htmlFor="property-title">Listing title</Label>
            <Input id="property-title" className="mt-2 h-11 rounded-xl" placeholder="Two-bedroom near campus" />
          </div>
          <div>
            <Label htmlFor="property-rent">Monthly rent</Label>
            <Input id="property-rent" className="mt-2 h-11 rounded-xl" type="number" placeholder="1680" />
          </div>
          <div>
            <Label htmlFor="property-date">Available from</Label>
            <Input id="property-date" className="mt-2 h-11 rounded-xl" type="date" />
          </div>
          <div className="sm:col-span-2">
            <Label htmlFor="property-address">Address</Label>
            <Input id="property-address" className="mt-2 h-11 rounded-xl" placeholder="Street, city, state" />
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="rounded-full border-border">Cancel</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button onClick={() => toast.success("Draft property created")} className="rounded-full">Create draft</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/* -------------------------- Manager: Inquiries -------------------------- */

function InquiriesView() {
  const groups = [
    { name: "Maya, Jordan & Eli", detail: "3 bedroom request · move-in August · verified students" },
    { name: "Noah & Priya", detail: "2 bedroom request · move-in August · verified students" },
    { name: "Avery, Mia & Theo", detail: "3 bedroom request · move-in September · verified students" },
  ];
  return (
    <>
      <PageHeading
        eyebrow="13 inquiries this week"
        title="Qualified renter groups."
        copy="Review budget, timing, and group readiness before you schedule a tour."
      />
      <div className="mt-8 space-y-4">
        <InquiryCard />
        {groups.map((group) => (
          <Card key={group.name} className="flex-col gap-4 rounded-[22px] border-border/70 p-5 shadow-none sm:flex-row sm:items-center sm:p-6">
            <AvatarGroup className="shrink-0">
              {group.name.split(" & ").map((name) => (
                <Avatar key={name}>
                  <AvatarFallback className="bg-secondary text-foreground">{name.trim()[0]}</AvatarFallback>
                </Avatar>
              ))}
            </AvatarGroup>
            <div className="min-w-0 flex-1">
              <p className="font-semibold">{group.name}</p>
              <p className="text-sm text-muted-foreground">{group.detail}</p>
            </div>
            <div className="grid grid-cols-2 gap-2 sm:flex sm:shrink-0">
              <Button variant="outline" className="rounded-full border-border">View profile</Button>
              <Button className="rounded-full" onClick={() => toast.success("Tour invitation sent")}>Invite to tour</Button>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
}

function InquiryCard() {
  return (
    <Card className="mt-5 flex-col gap-4 rounded-[22px] border-border/70 p-6 shadow-none sm:flex-row sm:items-center">
      <AvatarGroup className="shrink-0">
        {["JL", "MC", "EB"].map((initials) => (
          <Avatar key={initials} className="size-11">
            <AvatarFallback className="bg-secondary text-foreground">{initials}</AvatarFallback>
          </Avatar>
        ))}
      </AvatarGroup>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-semibold">Jordan, Maya &amp; Eli</h3>
          <Badge className="border-0 bg-emerald-500/12 text-emerald-600 dark:text-emerald-400">
            <ShieldCheck className="size-3" />Verified group
          </Badge>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">Interested in Neil Avenue Duplex · $2,400 combined budget · August move-in</p>
      </div>
      <div className="grid grid-cols-2 gap-2 sm:flex sm:shrink-0">
        <Button variant="outline" className="rounded-full border-border">Message</Button>
        <Button className="rounded-full" onClick={() => toast.success("Tour invitation sent")}>Invite to tour</Button>
      </div>
    </Card>
  );
}

/* -------------------------------- Profile ------------------------------- */

function ProfileView({ role }: { role: Role }) {
  const [editing, setEditing] = useState(false);
  const [updates, setUpdates] = useState({ messages: true, matches: true, marketing: false });

  const details: [string, string][] = [
    ["Role", role === "student" ? "Student renter" : "Property manager"],
    ["School or company", role === "student" ? "The Ohio State University" : "North Campus Homes"],
    ["Phone", "(614) 555-0148"],
    ["Location", "Columbus, Ohio"],
  ];

  const notificationRows: [keyof typeof updates, string][] = [
    ["messages", "New messages"],
    ["matches", role === "student" ? "New housing matches" : "New listing inquiries"],
    ["marketing", "Product updates"],
  ];

  return (
    <>
      <PageHeading
        eyebrow="Account and preferences"
        title="Your profile."
        copy={role === "student"
          ? "Help roommates and property managers understand who you are and what you need."
          : "Keep your contact details, company information, and security settings current."}
        action={(
          <Button onClick={() => setEditing(!editing)} variant={editing ? "secondary" : "default"} className="h-11 rounded-full">
            {editing ? <><X className="size-4" />Cancel editing</> : "Edit profile"}
          </Button>
        )}
      />
      <div className="mt-8 grid gap-5 xl:grid-cols-[minmax(0,1.3fr)_minmax(320px,0.7fr)]">
        <Card className="rounded-[24px] border-border/70 p-6 shadow-none sm:p-8">
          <div className="flex flex-wrap items-center gap-4">
            <Avatar className="size-20">
              <AvatarFallback className="bg-foreground text-xl font-semibold text-background">JL</AvatarFallback>
            </Avatar>
            <div>
              <h2 className="text-2xl font-semibold tracking-[-0.02em]">Jordan Lee</h2>
              <p className="text-sm text-muted-foreground">jordan.lee@osu.edu</p>
              <Badge className="mt-2 border-0 bg-emerald-500/12 text-emerald-600 dark:text-emerald-400">
                <ShieldCheck className="size-3" />Identity verified
              </Badge>
            </div>
          </div>
          {editing ? (
            <ProfileForm onDone={() => setEditing(false)} />
          ) : (
            <div className="mt-6 grid gap-5 border-t border-border/70 pt-6 sm:grid-cols-2">
              {details.map(([label, value]) => (
                <div key={label}>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">{label}</p>
                  <p className="mt-1 text-[15px] font-medium">{value}</p>
                </div>
              ))}
            </div>
          )}
        </Card>
        <div className="space-y-5">
          <Card className="gap-4 rounded-[24px] border-border/70 p-6 shadow-none">
            <h2 className="font-semibold">Security</h2>
            <p className="text-sm leading-relaxed text-muted-foreground">Use a unique password and review account access regularly.</p>
            <PasswordDialog />
            <Button variant="outline" className="h-11 justify-start rounded-xl border-border">
              <ShieldCheck className="size-4" />Verification settings
            </Button>
          </Card>
          <Card className="gap-2 rounded-[24px] border-border/70 p-6 shadow-none">
            <h2 className="mb-2 font-semibold">Notifications</h2>
            {notificationRows.map(([key, label]) => (
              <div key={key} className="flex items-center justify-between gap-4 border-b border-border/60 py-3 last:border-b-0 last:pb-0 first:pt-0">
                <Label htmlFor={key} className="text-[15px] font-normal">{label}</Label>
                <Switch
                  id={key}
                  checked={updates[key]}
                  onCheckedChange={(value) => setUpdates((current) => ({ ...current, [key]: value }))}
                />
              </div>
            ))}
          </Card>
        </div>
      </div>
    </>
  );
}

function ProfileForm({ onDone }: { onDone: () => void }) {
  function save(event: FormEvent) {
    event.preventDefault();
    toast.success("Profile updated");
    onDone();
  }
  return (
    <form onSubmit={save} className="mt-6 grid gap-4 border-t border-border/70 pt-6 sm:grid-cols-2">
      <div>
        <Label htmlFor="first-name">First name</Label>
        <Input id="first-name" defaultValue="Jordan" className="mt-2 h-11 rounded-xl" />
      </div>
      <div>
        <Label htmlFor="last-name">Last name</Label>
        <Input id="last-name" defaultValue="Lee" className="mt-2 h-11 rounded-xl" />
      </div>
      <div className="sm:col-span-2">
        <Label htmlFor="bio">About you</Label>
        <Textarea
          id="bio"
          defaultValue="Third-year student looking for a calm, friendly home close to campus."
          className="mt-2 min-h-28 rounded-xl"
        />
      </div>
      <div className="sm:col-span-2 flex justify-end">
        <Button className="h-11 rounded-full px-6">Save changes</Button>
      </div>
    </form>
  );
}

function PasswordDialog() {
  function update(event: FormEvent) {
    event.preventDefault();
    toast.success("Password updated securely");
  }
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="h-11 justify-start rounded-xl border-border">
          <KeyRound className="size-4" />Update password
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-[24px]">
        <form onSubmit={update}>
          <DialogHeader>
            <DialogTitle className="text-2xl tracking-tight">Update password</DialogTitle>
            <DialogDescription>Use at least 12 characters that you do not reuse elsewhere.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-5">
            <div>
              <Label htmlFor="current-password">Current password</Label>
              <Input id="current-password" type="password" className="mt-2 h-11 rounded-xl" />
            </div>
            <div>
              <Label htmlFor="new-password">New password</Label>
              <Input id="new-password" type="password" minLength={12} className="mt-2 h-11 rounded-xl" />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline" type="button" className="rounded-full border-border">Cancel</Button>
            </DialogClose>
            <DialogClose asChild>
              <Button type="submit" className="rounded-full">Update password</Button>
            </DialogClose>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
