"use client";

import Image from "next/image";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Bath, BedDouble, Bell, Building2, CalendarDays, Check, ChevronDown,
  CircleDollarSign, Clock3, Filter, Heart, Home, KeyRound, MapPin,
  Menu, MessageCircle, MoreHorizontal, PawPrint, Plus, Search, Send,
  Settings2, ShieldCheck, Sparkles, UserRound, UsersRound, X,
} from "lucide-react";
import { toast } from "sonner";

import { Avatar, AvatarFallback, AvatarGroup } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
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

type Role = "student" | "manager";
type StudentTab = "overview" | "homes" | "roommates" | "messages" | "profile";
type ManagerTab = "overview" | "properties" | "inquiries" | "profile";
type AppTab = StudentTab | ManagerTab;

type HomeListing = {
  id: number; title: string; place: string; rent: number; fit: number;
  beds: number; baths: number; available: string; pets: boolean;
};

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
  const [messages, setMessages] = useState([
    { mine: false, text: "Hi Jordan — your group looks like a strong fit. Would you like to tour Saturday?" },
    { mine: true, text: "Saturday afternoon works for all three of us." },
  ]);

  const nav = role === "student" ? studentNav : managerNav;
  const filteredListings = useMemo(() => listings.filter((home) => {
    const matchesSearch = `${home.title} ${home.place}`.toLowerCase().includes(search.toLowerCase());
    return matchesSearch && home.rent <= maxRent;
  }), [maxRent, search]);

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
    <main className="min-h-screen bg-background text-foreground">
      <Toaster position="top-center" richColors />
      <Header role={role} onRoleChange={changeRole} onOpenProfile={() => setActiveTab("profile")} />

      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as AppTab)} className="mx-auto max-w-[1440px] gap-0 lg:grid lg:grid-cols-[236px_minmax(0,1fr)]">
        <Sidebar role={role} nav={nav} activeTab={activeTab} onChange={(tab) => setActiveTab(tab)} />
        <section id="main" className="min-w-0 px-4 py-7 pb-28 sm:px-6 sm:py-10 lg:px-10 lg:pb-12 xl:px-14">
          {role === "student" ? (
            <>
              <TabsContent value="overview"><StudentOverview onNavigate={(tab) => setActiveTab(tab)} saved={saved} onSave={toggleSaved} /></TabsContent>
              <TabsContent value="homes"><HomesView listings={filteredListings} search={search} setSearch={setSearch} maxRent={maxRent} setMaxRent={setMaxRent} saved={saved} onSave={toggleSaved} /></TabsContent>
              <TabsContent value="roommates"><RoommatesView connected={connected} onConnect={toggleConnection} /></TabsContent>
              <TabsContent value="messages"><MessagesView messages={messages} setMessages={setMessages} /></TabsContent>
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

function Header({ role, onRoleChange, onOpenProfile }: { role: Role; onRoleChange: (role: Role) => void; onOpenProfile: () => void }) {
  return (
    <header className="sticky top-0 z-40 border-b border-white/60 bg-background/78 backdrop-blur-2xl supports-[backdrop-filter]:bg-background/68">
      <div className="mx-auto flex h-16 max-w-[1440px] items-center gap-3 px-4 sm:px-6 lg:px-8">
        <a className="flex items-center gap-2.5" href="#main" aria-label="Cribby overview">
          <span className="grid size-9 place-items-center rounded-xl bg-primary text-primary-foreground shadow-sm"><Home className="size-4.5" strokeWidth={2.4} /></span>
          <span className="text-[1.05rem] font-semibold tracking-[-0.03em]">Cribby</span>
        </a>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="secondary" className="ml-auto h-10 rounded-full px-3 sm:ml-4">
              {role === "student" ? <UsersRound /> : <Building2 />}
              <span className="hidden sm:inline">{role === "student" ? "Student" : "Property manager"}</span>
              <ChevronDown className="size-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-64 rounded-xl p-2">
            <DropdownMenuLabel>Switch workspace</DropdownMenuLabel>
            <DropdownMenuRadioGroup value={role} onValueChange={(value) => onRoleChange(value as Role)}>
              <DropdownMenuRadioItem value="student" className="rounded-lg py-2.5"><UsersRound /> Student</DropdownMenuRadioItem>
              <DropdownMenuRadioItem value="manager" className="rounded-lg py-2.5"><Building2 /> Property manager</DropdownMenuRadioItem>
            </DropdownMenuRadioGroup>
          </DropdownMenuContent>
        </DropdownMenu>
        <Button variant="ghost" size="icon" className="rounded-full" aria-label="Notifications"><Bell /></Button>
        <button onClick={onOpenProfile} className="flex min-h-11 items-center gap-2 rounded-full px-1.5 text-sm font-medium hover:bg-secondary" type="button">
          <Avatar className="size-8"><AvatarFallback className="bg-primary text-xs font-semibold text-primary-foreground">JL</AvatarFallback></Avatar>
          <span className="hidden sm:inline">Jordan Lee</span><ChevronDown className="size-3.5 text-muted-foreground" />
        </button>
      </div>
    </header>
  );
}

function Sidebar({ role, nav, activeTab, onChange }: { role: Role; nav: readonly { value: string; label: string; icon: typeof Home }[]; activeTab: AppTab; onChange: (tab: AppTab) => void }) {
  return (
    <aside className="hidden min-h-[calc(100vh-4rem)] border-r border-border/70 px-5 py-8 lg:block">
      <p className="px-3 text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">{role === "student" ? "Student workspace" : "Manager workspace"}</p>
      <nav className="mt-4 space-y-1" aria-label="Workspace navigation">
        {nav.map((item) => (
          <button key={item.value} type="button" onClick={() => onChange(item.value as AppTab)} className={`flex min-h-11 w-full items-center gap-2 rounded-xl px-3 text-sm font-medium transition active:scale-[.98] ${activeTab === item.value ? "bg-primary text-primary-foreground shadow-sm" : "text-muted-foreground hover:bg-secondary hover:text-foreground"}`}>
            <item.icon className="size-[18px]" />{item.label}
          </button>
        ))}
      </nav>
      <Card className="mt-10 gap-3 border-0 bg-[linear-gradient(145deg,#0b54d2,#2563eb)] p-5 text-white shadow-[0_18px_45px_rgba(29,78,216,.22)]">
        <div className="flex items-center gap-2 text-sm font-semibold"><ShieldCheck className="size-4" />{role === "student" ? "Verified profile" : "Verified manager"}</div>
        <p className="text-sm leading-6 text-blue-100">{role === "student" ? "Complete two preferences to improve your matches." : "All active listings passed the quality check."}</p>
        <div className="h-1.5 overflow-hidden rounded-full bg-white/20"><div className="h-full w-[84%] rounded-full bg-white" /></div>
      </Card>
    </aside>
  );
}

function MobileNav({ nav }: { nav: readonly { value: string; label: string; icon: typeof Home }[] }) {
  return (
    <TabsList className="fixed inset-x-3 bottom-3 z-40 flex h-auto items-center justify-around rounded-2xl border border-white/70 bg-background/88 p-1.5 shadow-[0_14px_50px_rgba(15,23,42,.16)] backdrop-blur-2xl lg:hidden">
      {nav.map((item) => (
        <TabsTrigger key={item.value} value={item.value} className="flex min-h-12 min-w-12 flex-1 flex-col gap-0.5 rounded-xl px-1 text-[11px] after:hidden data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
          <item.icon className="size-[18px]" />{item.label}
        </TabsTrigger>
      ))}
    </TabsList>
  );
}

function PageHeading({ eyebrow, title, copy, action }: { eyebrow: string; title: string; copy: string; action?: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="text-sm font-medium text-primary">{eyebrow}</p><h1 className="mt-1 text-3xl font-semibold tracking-[-0.04em] sm:text-4xl">{title}</h1><p className="mt-2 max-w-2xl text-base leading-7 text-muted-foreground">{copy}</p></div>{action}
    </div>
  );
}

function StudentOverview({ onNavigate, saved, onSave }: { onNavigate: (tab: StudentTab) => void; saved: number[]; onSave: (id: number) => void }) {
  return (
    <>
      <PageHeading eyebrow="Friday, September 25" title="Good morning, Jordan." copy="Your group is almost ready. Compare homes that work for everyone." action={<Button onClick={() => onNavigate("roommates")} className="h-11 rounded-full px-5 shadow-sm"><UsersRound />View my group</Button>} />
      <div className="mt-8 grid gap-4 xl:grid-cols-[minmax(0,1.45fr)_minmax(300px,.75fr)]">
        <Card className="relative min-h-[330px] overflow-hidden border-0 bg-zinc-950 p-0 text-white shadow-[0_28px_70px_rgba(15,23,42,.16)]">
          <Image src="/images/cribby-columbus-apartment.png" alt="Bright two-bedroom apartment living room near campus" fill priority className="object-cover opacity-80" sizes="(max-width: 1280px) 100vw, 760px" />
          <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(5,12,25,.88),rgba(5,12,25,.28)_65%,rgba(5,12,25,.1))]" />
          <div className="relative flex h-full min-h-[330px] max-w-xl flex-col justify-between p-6 sm:p-8">
            <Badge className="border-white/20 bg-white/14 text-white backdrop-blur-md" variant="outline"><Sparkles className="size-3" />12 new group matches</Badge>
            <div><p className="text-sm font-medium text-blue-100">Homes picked for all three of you</p><h2 className="mt-2 text-3xl font-semibold tracking-[-0.045em] sm:text-4xl">One search. Everyone&apos;s needs.</h2><p className="mt-3 max-w-md text-sm leading-6 text-zinc-200">Combined budget, move-in date, commute, pets, and space—already lined up.</p><Button onClick={() => onNavigate("homes")} className="mt-5 h-11 rounded-full bg-white px-5 text-zinc-950 hover:bg-zinc-100">Explore matches</Button></div>
          </div>
        </Card>
        <GroupReadiness />
      </div>
      <SectionHeading title="Best matches for your group" copy="Ranked by the preferences your group shares." action={<Button variant="ghost" onClick={() => onNavigate("homes")}>View all</Button>} />
      <div className="mt-5 grid gap-4 md:grid-cols-2">{listings.slice(0, 2).map((home) => <ListingCard key={home.id} home={home} saved={saved.includes(home.id)} onSave={() => onSave(home.id)} />)}</div>
    </>
  );
}

function GroupReadiness() {
  return (
    <Card className="gap-5 border-border/70 bg-card/90 p-6 shadow-[0_18px_50px_rgba(15,23,42,.06)]">
      <div className="flex items-start justify-between"><div><p className="text-sm font-medium text-muted-foreground">Group readiness</p><p className="mt-1 text-4xl font-semibold tracking-[-0.05em]">82%</p></div><div className="grid size-11 place-items-center rounded-2xl bg-blue-50 text-primary dark:bg-blue-950/40"><UsersRound className="size-5" /></div></div>
      <div className="h-2 overflow-hidden rounded-full bg-secondary"><div className="h-full w-[82%] rounded-full bg-primary" /></div>
      <div className="space-y-3">{[["Combined budget", "$2,400 / month", true], ["Move-in date", "August 12", true], ["Neighborhoods", "Choose together", false]].map(([label, value, done]) => <div key={String(label)} className="flex items-center justify-between gap-4 text-sm"><span className="text-muted-foreground">{label}</span><span className={done ? "font-medium" : "font-medium text-primary"}>{value}</span></div>)}</div>
      <Button variant="outline" className="mt-auto h-11 rounded-full" onClick={() => toast.info("Group preferences opened")}>Finish group setup</Button>
    </Card>
  );
}

function SectionHeading({ title, copy, action }: { title: string; copy: string; action?: React.ReactNode }) {
  return <div className="mt-10 flex items-end justify-between gap-4"><div><h2 className="text-2xl font-semibold tracking-[-0.035em]">{title}</h2><p className="mt-1 text-sm text-muted-foreground">{copy}</p></div>{action}</div>;
}

function ListingCard({ home, saved, onSave }: { home: HomeListing; saved: boolean; onSave: () => void }) {
  return (
    <Card className="group gap-0 overflow-hidden border-border/70 p-0 shadow-[0_16px_40px_rgba(15,23,42,.05)] transition hover:-translate-y-0.5 hover:shadow-[0_22px_55px_rgba(15,23,42,.09)]">
      <div className="relative aspect-[16/8] overflow-hidden bg-secondary"><Image src="/images/cribby-columbus-apartment.png" alt="" fill className={`object-cover transition duration-500 group-hover:scale-[1.025] ${home.id % 2 === 0 ? "object-[70%_48%]" : "object-center"}`} sizes="(max-width: 768px) 100vw, 45vw" /><button aria-label={`${saved ? "Unsave" : "Save"} ${home.title}`} onClick={onSave} className="absolute right-3 top-3 grid size-11 place-items-center rounded-full bg-white/90 text-zinc-800 shadow-sm backdrop-blur-lg transition active:scale-95" type="button"><Heart className={`size-[18px] ${saved ? "fill-primary text-primary" : ""}`} /></button><Badge className="absolute bottom-3 left-3 border-0 bg-zinc-950/80 text-white backdrop-blur-lg">{home.fit}% group fit</Badge></div>
      <div className="p-5"><div className="flex items-start justify-between gap-4"><div><h3 className="font-semibold tracking-[-0.02em]">{home.title}</h3><p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground"><MapPin className="size-3.5" />{home.place}</p></div><p className="text-right text-lg font-semibold tracking-[-0.03em]">${home.rent.toLocaleString()}<span className="block text-xs font-normal text-muted-foreground">per month</span></p></div><div className="mt-4 flex flex-wrap gap-3 text-xs font-medium text-muted-foreground"><span className="flex items-center gap-1"><BedDouble className="size-3.5" />{home.beds} beds</span><span className="flex items-center gap-1"><Bath className="size-3.5" />{home.baths} baths</span><span className="flex items-center gap-1"><CalendarDays className="size-3.5" />{home.available}</span>{home.pets && <span className="flex items-center gap-1"><PawPrint className="size-3.5" />Pets</span>}</div></div>
    </Card>
  );
}

function HomesView({ listings: homes, search, setSearch, maxRent, setMaxRent, saved, onSave }: { listings: HomeListing[]; search: string; setSearch: (value: string) => void; maxRent: number; setMaxRent: (value: number) => void; saved: number[]; onSave: (id: number) => void }) {
  return (
    <>
      <PageHeading eyebrow={`${homes.length} homes match your group`} title="Find your next home." copy="Every result is checked against your group’s combined budget, move-in date, and space needs." />
      <div className="mt-7 flex gap-3"><div className="relative flex-1"><Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input value={search} onChange={(event) => setSearch(event.target.value)} className="h-12 rounded-full bg-card pl-11 shadow-sm" placeholder="Search neighborhood or home" /></div><FilterSheet maxRent={maxRent} setMaxRent={setMaxRent} /></div>
      <div className="mt-4 flex gap-2 overflow-x-auto pb-1 scrollbar-none">{["For your group", "2–3 bedrooms", "Near campus", "Pets allowed", "Available August"].map((filter, index) => <Button key={filter} variant={index === 0 ? "default" : "outline"} className="h-9 rounded-full">{filter}</Button>)}</div>
      {homes.length ? <div className="mt-6 grid gap-4 md:grid-cols-2">{homes.map((home) => <ListingCard key={home.id} home={home} saved={saved.includes(home.id)} onSave={() => onSave(home.id)} />)}</div> : <Card className="mt-8 items-center p-10 text-center"><Search className="size-7 text-muted-foreground" /><h2 className="font-semibold">No homes match yet</h2><p className="text-sm text-muted-foreground">Try a different neighborhood or raise the maximum rent.</p><Button variant="outline" onClick={() => { setSearch(""); setMaxRent(2400); }}>Clear filters</Button></Card>}
    </>
  );
}

function FilterSheet({ maxRent, setMaxRent }: { maxRent: number; setMaxRent: (value: number) => void }) {
  const [draft, setDraft] = useState(maxRent);
  return (
    <Sheet><SheetTrigger asChild><Button variant="outline" className="h-12 rounded-full px-5"><Filter />Filters</Button></SheetTrigger><SheetContent className="w-full sm:max-w-md"><SheetHeader className="p-6"><SheetTitle className="text-xl">Home filters</SheetTitle><SheetDescription>Keep the results realistic for your entire group.</SheetDescription></SheetHeader><div className="space-y-6 px-6"><div><Label htmlFor="max-rent">Maximum monthly rent</Label><Input id="max-rent" className="mt-2 h-11" type="number" value={draft} onChange={(e) => setDraft(Number(e.target.value))} /></div><div className="grid grid-cols-2 gap-3"><div><Label>Bedrooms</Label><Button variant="outline" className="mt-2 h-11 w-full justify-between">2–3 <ChevronDown /></Button></div><div><Label>Move-in</Label><Button variant="outline" className="mt-2 h-11 w-full justify-between">August <ChevronDown /></Button></div></div><div className="flex items-center justify-between"><div><p className="text-sm font-medium">Pets allowed</p><p className="text-xs text-muted-foreground">At least one pet-friendly option</p></div><Switch aria-label="Pets allowed" /></div></div><SheetFooter className="p-6"><SheetClose asChild><Button className="h-11 rounded-full" onClick={() => setMaxRent(draft)}>Show matching homes</Button></SheetClose><SheetClose asChild><Button variant="ghost">Cancel</Button></SheetClose></SheetFooter></SheetContent></Sheet>
  );
}

function RoommatesView({ connected, onConnect }: { connected: string[]; onConnect: (name: string) => void }) {
  return (
    <><PageHeading eyebrow="Preference-based, never mysterious" title="Meet compatible roommates." copy="Matches explain the habits, budget, timing, and housing needs you have in common." action={<Button className="h-11 rounded-full"><Settings2 />Edit preferences</Button>} /><div className="mt-8 grid gap-4 lg:grid-cols-3">{roommates.map((mate) => <Card key={mate.name} className="gap-5 border-border/70 p-6 shadow-[0_16px_40px_rgba(15,23,42,.05)]"><div className="flex items-center justify-between"><Avatar className={`size-14 ${mate.color}`}><AvatarFallback className={`${mate.color} font-semibold`}>{mate.initials}</AvatarFallback></Avatar><Badge className="border-blue-200 bg-blue-50 text-primary dark:border-blue-900 dark:bg-blue-950/40">{mate.match}% match</Badge></div><div><h2 className="text-lg font-semibold tracking-[-0.02em]">{mate.name}</h2><p className="mt-1 text-sm text-muted-foreground">{mate.school}</p></div><div className="space-y-2 text-sm"><p className="flex items-center gap-2"><CircleDollarSign className="size-4 text-muted-foreground" />{mate.budget}</p><p className="flex items-start gap-2"><Sparkles className="mt-0.5 size-4 text-muted-foreground" />{mate.note}</p></div><Button disabled={connected.includes(mate.name)} onClick={() => onConnect(mate.name)} className="mt-auto h-11 rounded-full">{connected.includes(mate.name) ? <><Check />Request sent</> : "Connect"}</Button></Card>)}</div><SectionHeading title="Your roommate group" copy="Three people, one shared set of housing requirements." /><Card className="mt-5 flex-row items-center justify-between gap-4 p-6"><div className="flex items-center gap-4"><AvatarGroup>{["JL", "MC", "EB"].map((initials) => <Avatar key={initials} className="size-10"><AvatarFallback>{initials}</AvatarFallback></Avatar>)}</AvatarGroup><div><p className="font-semibold">Jordan, Maya & Eli</p><p className="text-sm text-muted-foreground">$2,400 combined budget · move-in August</p></div></div><Button variant="outline" className="rounded-full">Manage</Button></Card></>
  );
}

function MessagesView({ messages, setMessages }: { messages: { mine: boolean; text: string }[]; setMessages: React.Dispatch<React.SetStateAction<{ mine: boolean; text: string }[]>> }) {
  const [draft, setDraft] = useState("");
  function send(event: FormEvent) { event.preventDefault(); const text = draft.trim(); if (!text) return; setMessages((current) => [...current, { mine: true, text }]); setDraft(""); }
  return (
    <><PageHeading eyebrow="3 active conversations" title="Messages" copy="Keep your roommate group and property managers in one clear thread." /><Card className="mt-8 min-h-[560px] gap-0 overflow-hidden border-border/70 p-0 md:grid md:grid-cols-[280px_minmax(0,1fr)]"><aside className="border-b border-border md:border-b-0 md:border-r"><div className="border-b border-border p-4"><div className="relative"><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9" placeholder="Search messages" /></div></div>{["Alex Morgan · Landlord", "Maya Chen", "Eli Brooks"].map((name, index) => <button key={name} className={`flex w-full gap-3 border-b border-border/60 p-4 text-left ${index === 0 ? "bg-secondary" : "hover:bg-secondary/60"}`}><Avatar><AvatarFallback>{name.split(" ").map((part) => part[0]).slice(0, 2).join("")}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate text-sm font-semibold">{name}</p><p className="mt-0.5 truncate text-xs text-muted-foreground">{index === 0 ? "Saturday afternoon works…" : "Shared a new housing preference"}</p></div></button>)}</aside><section className="flex min-h-[480px] flex-col"><div className="flex items-center gap-3 border-b border-border p-4"><Avatar><AvatarFallback>AM</AvatarFallback></Avatar><div><p className="text-sm font-semibold">Alex Morgan</p><p className="text-xs text-muted-foreground">Neil Avenue Duplex</p></div><Button variant="outline" className="ml-auto rounded-full">View home</Button></div><div className="flex flex-1 flex-col gap-3 overflow-y-auto p-5">{messages.map((message, index) => <div key={`${message.text}-${index}`} className={`max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-6 ${message.mine ? "ml-auto rounded-br-md bg-primary text-primary-foreground" : "rounded-bl-md bg-secondary"}`}>{message.text}</div>)}</div><form onSubmit={send} className="flex gap-2 border-t border-border p-4"><Input aria-label="Message" value={draft} onChange={(e) => setDraft(e.target.value)} className="h-11 rounded-full" placeholder="Write a message" /><Button aria-label="Send message" size="icon-lg" className="rounded-full"><Send /></Button></form></section></Card></>
  );
}

function ManagerOverview({ onNavigate }: { onNavigate: (tab: ManagerTab) => void }) {
  const stats = [
    { label: "Active listings", value: "2", icon: Building2 },
    { label: "New inquiries", value: "13", icon: MessageCircle },
    { label: "Saved this week", value: "58", icon: Heart },
    { label: "Average response", value: "1.8h", icon: Clock3 },
  ];
  return (
    <><PageHeading eyebrow="Property manager workspace" title="Good morning, Jordan." copy="See which listings are getting attention and respond to qualified roommate groups." action={<NewListingDialog />} /><div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{stats.map(({ label, value, icon: Icon }) => <Card key={label} className="gap-4 border-border/70 p-5"><div className="flex items-center justify-between"><span className="text-sm text-muted-foreground">{label}</span><Icon className="size-4 text-primary" /></div><p className="text-3xl font-semibold tracking-[-0.045em]">{value}</p></Card>)}</div><SectionHeading title="Property performance" copy="Your active listings, ordered by renter interest." action={<Button variant="ghost" onClick={() => onNavigate("properties")}>Manage all</Button>} /><div className="mt-5 space-y-3">{managedProperties.slice(0, 2).map((property) => <PropertyRow key={property.id} property={property} />)}</div><SectionHeading title="Recent group inquiries" copy="Groups ready to move together tend to convert faster." action={<Button variant="ghost" onClick={() => onNavigate("inquiries")}>View all</Button>} /><InquiryCard /></>
  );
}

function PropertiesView() {
  return <><PageHeading eyebrow="3 rental properties" title="Manage your properties." copy="Keep availability, pricing, photos, and renter interest current." action={<NewListingDialog />} /><div className="mt-8 space-y-3">{managedProperties.map((property) => <PropertyRow key={property.id} property={property} />)}</div></>;
}

function PropertyRow({ property }: { property: typeof managedProperties[number] }) {
  const [active, setActive] = useState(property.status === "Active");
  return <Card className="flex-row items-center gap-4 border-border/70 p-4 sm:p-5"><div className="relative hidden size-20 overflow-hidden rounded-xl bg-secondary sm:block"><Image src="/images/cribby-columbus-apartment.png" alt="" fill className="object-cover" sizes="80px" /></div><div className="min-w-0 flex-1"><div className="flex items-center gap-2"><h3 className="truncate font-semibold">{property.name}</h3><Badge variant={active ? "default" : "secondary"} className={active ? "bg-emerald-600" : ""}>{active ? "Active" : "Paused"}</Badge></div><p className="mt-1 text-sm text-muted-foreground">{property.rent}/month</p><div className="mt-3 flex flex-wrap gap-4 text-xs text-muted-foreground"><span>{property.views} views</span><span>{property.saves} saves</span><span>{property.inquiries} inquiries</span></div></div><Switch checked={active} onCheckedChange={(next) => { setActive(next); toast.success(next ? "Listing activated" : "Listing paused"); }} aria-label={`Toggle ${property.name}`} /><DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" size="icon" aria-label={`Actions for ${property.name}`}><MoreHorizontal /></Button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuItem onClick={() => toast.info("Listing editor opened")}>Edit listing</DropdownMenuItem><DropdownMenuItem>Preview listing</DropdownMenuItem><DropdownMenuSeparator /><DropdownMenuItem variant="destructive">Remove listing</DropdownMenuItem></DropdownMenuContent></DropdownMenu></Card>;
}

function NewListingDialog() {
  return <Dialog><DialogTrigger asChild><Button className="h-11 rounded-full px-5"><Plus />Add property</Button></DialogTrigger><DialogContent className="rounded-2xl sm:max-w-xl"><DialogHeader><DialogTitle>Add a rental property</DialogTitle><DialogDescription>Start with the facts students and roommate groups need to decide quickly.</DialogDescription></DialogHeader><div className="grid gap-4 py-2 sm:grid-cols-2"><div className="sm:col-span-2"><Label htmlFor="property-title">Listing title</Label><Input id="property-title" className="mt-2 h-11" placeholder="Two-bedroom near campus" /></div><div><Label htmlFor="property-rent">Monthly rent</Label><Input id="property-rent" className="mt-2 h-11" type="number" placeholder="1680" /></div><div><Label htmlFor="property-date">Available from</Label><Input id="property-date" className="mt-2 h-11" type="date" /></div><div className="sm:col-span-2"><Label htmlFor="property-address">Address</Label><Input id="property-address" className="mt-2 h-11" placeholder="Street, city, state" /></div></div><DialogFooter><DialogClose asChild><Button variant="outline">Cancel</Button></DialogClose><DialogClose asChild><Button onClick={() => toast.success("Draft property created")}>Create draft</Button></DialogClose></DialogFooter></DialogContent></Dialog>;
}

function InquiriesView() {
  return <><PageHeading eyebrow="13 inquiries this week" title="Qualified renter groups." copy="Review budget, timing, and group readiness before you schedule a tour." /><div className="mt-8 space-y-4"><InquiryCard />{["Maya, Jordan & Eli", "Noah & Priya", "Avery, Mia & Theo"].map((group, index) => <Card key={group} className="gap-4 border-border/70 p-5 sm:flex-row sm:items-center"><AvatarGroup>{group.split(" & ").map((name) => <Avatar key={name}><AvatarFallback>{name[0]}</AvatarFallback></Avatar>)}</AvatarGroup><div className="flex-1"><p className="font-semibold">{group}</p><p className="text-sm text-muted-foreground">{index + 2} bedroom request · move-in August · verified students</p></div><Button variant="outline" className="rounded-full">View profile</Button><Button className="rounded-full" onClick={() => toast.success("Tour invitation sent")}>Invite to tour</Button></Card>)}</div></>;
}

function InquiryCard() {
  return <Card className="mt-5 border-blue-100 bg-[linear-gradient(120deg,#f2f7ff,#fff)] p-6 dark:border-blue-950 dark:bg-none"><div className="flex flex-col gap-5 sm:flex-row sm:items-center"><AvatarGroup>{["JL", "MC", "EB"].map((initials) => <Avatar key={initials} className="size-11"><AvatarFallback>{initials}</AvatarFallback></Avatar>)}</AvatarGroup><div className="flex-1"><div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold">Jordan, Maya & Eli</h3><Badge className="bg-emerald-600"><ShieldCheck />Verified group</Badge></div><p className="mt-1 text-sm text-muted-foreground">Interested in Neil Avenue Duplex · $2,400 combined budget · August move-in</p></div><Button variant="outline" className="rounded-full">Message</Button><Button className="rounded-full" onClick={() => toast.success("Tour invitation sent")}>Invite to tour</Button></div></Card>;
}

function ProfileView({ role }: { role: Role }) {
  const [editing, setEditing] = useState(false);
  const [updates, setUpdates] = useState({ messages: true, matches: true, marketing: false });
  return (
    <><PageHeading eyebrow="Account and preferences" title="Your profile." copy={role === "student" ? "Help roommates and property managers understand who you are and what you need." : "Keep your contact details, company information, and security settings current."} action={<Button onClick={() => setEditing(!editing)} className="h-11 rounded-full" variant={editing ? "secondary" : "default"}>{editing ? <><X />Cancel editing</> : "Edit profile"}</Button>} /><div className="mt-8 grid gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(300px,.8fr)]"><Card className="gap-6 border-border/70 p-6 sm:p-8"><div className="flex items-center gap-4"><Avatar className="size-20"><AvatarFallback className="bg-primary text-xl font-semibold text-primary-foreground">JL</AvatarFallback></Avatar><div><h2 className="text-xl font-semibold">Jordan Lee</h2><p className="text-sm text-muted-foreground">jordan.lee@osu.edu</p><Badge className="mt-2 bg-emerald-600"><ShieldCheck />Identity verified</Badge></div></div>{editing ? <ProfileForm onDone={() => setEditing(false)} /> : <div className="grid gap-5 border-t border-border pt-6 sm:grid-cols-2">{[["Role", role === "student" ? "Student renter" : "Property manager"], ["School or company", role === "student" ? "The Ohio State University" : "North Campus Homes"], ["Phone", "(614) 555-0148"], ["Location", "Columbus, Ohio"]].map(([label, value]) => <div key={label}><p className="text-xs font-semibold uppercase tracking-[0.08em] text-muted-foreground">{label}</p><p className="mt-1 text-sm font-medium">{value}</p></div>)}</div>}</Card><div className="space-y-5"><Card className="gap-4 border-border/70 p-6"><h2 className="font-semibold">Security</h2><p className="text-sm leading-6 text-muted-foreground">Use a unique password and review account access regularly.</p><PasswordDialog /><Button variant="outline" className="h-11 justify-start rounded-xl"><ShieldCheck />Verification settings</Button></Card><Card className="gap-5 border-border/70 p-6"><h2 className="font-semibold">Notifications</h2>{[["messages", "New messages"], ["matches", role === "student" ? "New housing matches" : "New listing inquiries"], ["marketing", "Product updates"]].map(([key, label]) => <div key={key} className="flex items-center justify-between gap-4"><Label htmlFor={key}>{label}</Label><Switch id={key} checked={updates[key as keyof typeof updates]} onCheckedChange={(value) => setUpdates((current) => ({ ...current, [key]: value }))} /></div>)}</Card></div></div></>
  );
}

function ProfileForm({ onDone }: { onDone: () => void }) {
  function save(event: FormEvent) { event.preventDefault(); toast.success("Profile updated"); onDone(); }
  return <form onSubmit={save} className="grid gap-4 border-t border-border pt-6 sm:grid-cols-2"><div><Label htmlFor="first-name">First name</Label><Input id="first-name" defaultValue="Jordan" className="mt-2 h-11" /></div><div><Label htmlFor="last-name">Last name</Label><Input id="last-name" defaultValue="Lee" className="mt-2 h-11" /></div><div className="sm:col-span-2"><Label htmlFor="bio">About you</Label><Textarea id="bio" defaultValue="Third-year student looking for a calm, friendly home close to campus." className="mt-2 min-h-28" /></div><div className="sm:col-span-2 flex justify-end"><Button className="h-11 rounded-full px-6">Save changes</Button></div></form>;
}

function PasswordDialog() {
  function update(event: FormEvent) { event.preventDefault(); toast.success("Password updated securely"); }
  return <Dialog><DialogTrigger asChild><Button variant="outline" className="h-11 justify-start rounded-xl"><KeyRound />Update password</Button></DialogTrigger><DialogContent className="rounded-2xl"><form onSubmit={update}><DialogHeader><DialogTitle>Update password</DialogTitle><DialogDescription>Use at least 12 characters that you do not reuse elsewhere.</DialogDescription></DialogHeader><div className="space-y-4 py-5"><div><Label htmlFor="current-password">Current password</Label><Input id="current-password" type="password" className="mt-2 h-11" /></div><div><Label htmlFor="new-password">New password</Label><Input id="new-password" type="password" minLength={12} className="mt-2 h-11" /></div></div><DialogFooter><DialogClose asChild><Button variant="outline" type="button">Cancel</Button></DialogClose><DialogClose asChild><Button type="submit">Update password</Button></DialogClose></DialogFooter></form></DialogContent></Dialog>;
}
